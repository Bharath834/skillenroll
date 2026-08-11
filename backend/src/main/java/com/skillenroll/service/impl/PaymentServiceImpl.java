package com.skillenroll.service.impl;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.skillenroll.config.RazorpayProperties;
import com.skillenroll.dto.EnrollmentRequest;
import com.skillenroll.dto.EnrollmentResponse;
import com.skillenroll.dto.PaymentOrderRequest;
import com.skillenroll.dto.PaymentOrderResponse;
import com.skillenroll.dto.PaymentReVerificationRequest;
import com.skillenroll.dto.PaymentVerificationRequest;
import com.skillenroll.dto.PaymentVerificationResponse;
import com.skillenroll.entity.Course;
import com.skillenroll.entity.PaymentOrder;
import com.skillenroll.enums.EnrollmentStatus;
import com.skillenroll.enums.PaymentOrderStatus;
import com.skillenroll.exception.DuplicateResourceException;
import com.skillenroll.exception.ResourceNotFoundException;
import com.skillenroll.repository.CourseRepository;
import com.skillenroll.repository.PaymentOrderRepository;
import com.skillenroll.security.service.SecurityUtils;
import com.skillenroll.service.interfaces.EnrollmentService;
import com.skillenroll.service.interfaces.PaymentService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Implementation of {@link PaymentService}.
 *
 * <p>Creates Razorpay orders for courses using the configured TEST
 * credentials. The amount is derived from the course's stored price (in
 * paise, INR) - never from the client. Every order is persisted in the
 * {@code payment_orders} ledger; verification validates the order-to-user and
 * order-to-course binding against that ledger (no Razorpay fetch) and records
 * the payment before activating the enrollment. The Razorpay key secret is
 * never logged or exposed.
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private static final String CURRENCY_INR = "INR";

    /** Unpaid orders older than this are never reused for a fresh checkout. */
    private static final int ORDER_REUSE_MAX_AGE_HOURS = 24;

    private final CourseRepository courseRepository;
    private final RazorpayClient razorpayClient;
    private final RazorpayProperties razorpayProperties;
    private final EnrollmentService enrollmentService;
    private final PaymentOrderRepository paymentOrderRepository;

    public PaymentServiceImpl(CourseRepository courseRepository,
                              RazorpayClient razorpayClient,
                              RazorpayProperties razorpayProperties,
                              EnrollmentService enrollmentService,
                              PaymentOrderRepository paymentOrderRepository) {
        this.courseRepository = courseRepository;
        this.razorpayClient = razorpayClient;
        this.razorpayProperties = razorpayProperties;
        this.enrollmentService = enrollmentService;
        this.paymentOrderRepository = paymentOrderRepository;
    }

    @Override
    public PaymentOrderResponse createOrder(PaymentOrderRequest request) throws RazorpayException {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getCourseId()));

        if (course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Payment order rejected - course {} is free", course.getId());
            throw new IllegalArgumentException("This course is free and does not require payment");
        }

        Long userId = SecurityUtils.currentUser().getUser().getId();
        long amountPaise = toPaise(course.getPrice());

        // Reuse the newest unpaid order for this user/course (same amount,
        // created recently) so repeated checkouts do not create orphaned
        // Razorpay orders. Orders older than the reuse window are not reused:
        // Razorpay may have invalidated them, and a captured-but-unverified
        // payment from a previous session should not silently resurface.
        Optional<PaymentOrder> existing = paymentOrderRepository
                .findFirstByUserIdAndCourseIdAndStatusOrderByCreatedAtDesc(
                        userId, course.getId(), PaymentOrderStatus.CREATED);
        if (existing.isPresent()
                && existing.get().getAmountPaise() == amountPaise
                && isFreshForReuse(existing.get())) {
            log.info("Reusing existing Razorpay order {} for course {}", existing.get().getOrderId(), course.getId());
            return toResponse(existing.get(), course);
        }

        // Receipt is bounded by Razorpay's 40-char limit; nanoTime keeps it
        // unique per order even for rapid repeat purchases of the same course.
        String receipt = "course-" + course.getId() + "-" + Long.toHexString(System.nanoTime());

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountPaise);
        orderRequest.put("currency", CURRENCY_INR);
        orderRequest.put("receipt", receipt);
        orderRequest.put("notes", new JSONObject()
                .put("courseId", course.getId())
                .put("courseTitle", course.getTitle()));

        Order order;
        try {
            order = razorpayClient.orders.create(orderRequest);
        } catch (RazorpayException ex) {
            // Mapped to 502 Bad Gateway with a generic message by GlobalExceptionHandler.
            log.error("Razorpay order creation failed for course {}", course.getId(), ex);
            throw ex;
        }

        PaymentOrder saved = paymentOrderRepository.save(PaymentOrder.builder()
                .orderId(optString(order, "id"))
                .userId(userId)
                .courseId(course.getId())
                .amountPaise(amountPaise)
                .currency(CURRENCY_INR)
                .receipt(receipt)
                .status(PaymentOrderStatus.CREATED)
                .build());

        log.info("Razorpay order {} created for course {} (amount {} {})",
                saved.getOrderId(), course.getId(), amountPaise, CURRENCY_INR);
        return toResponse(saved, course);
    }

    @Override
    public PaymentVerificationResponse verifyPayment(PaymentVerificationRequest request) {
        PaymentOrder order = paymentOrderRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found"));
        assertOwnedAndMatchesCourse(order, request.getCourseId());

        boolean valid;
        try {
            valid = Utils.verifyPaymentSignature(
                    verificationAttributes(request.getOrderId(), request.getPaymentId(), request.getSignature()),
                    razorpayProperties.getKeySecret());
        } catch (RazorpayException ex) {
            // Pure local HMAC computation - an exception here means the payload
            // is malformed, which is a client error, not a gateway failure.
            log.warn("Payment verification aborted for order {}: {}", request.getOrderId(), ex.getMessage());
            throw new IllegalArgumentException("Payment verification failed: invalid verification data");
        }

        if (!valid) {
            log.warn("Payment signature verification failed for order {}", request.getOrderId());
            throw new IllegalArgumentException("Payment signature verification failed");
        }

        markPaid(order, request.getPaymentId(), request.getSignature());

        EnrollmentStatus enrollmentStatus =
                request.getCourseId() != null ? activateEnrollment(request.getCourseId()) : null;
        log.info("Payment verified for order {} (payment {}), enrollment status: {}",
                order.getOrderId(), request.getPaymentId(), enrollmentStatus);
        return PaymentVerificationResponse.builder()
                .verified(true)
                .orderId(order.getOrderId())
                .paymentId(request.getPaymentId())
                .enrollmentStatus(enrollmentStatus)
                .build();
    }

    @Override
    public PaymentVerificationResponse reVerifyPayment(PaymentReVerificationRequest request) {
        PaymentOrder order = paymentOrderRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found"));
        assertOwnedAndMatchesCourse(order, request.getCourseId());

        if (order.getStatus() != PaymentOrderStatus.PAID) {
            log.warn("Re-verification rejected - order {} has no verified payment", request.getOrderId());
            throw new DuplicateResourceException(
                    "No verified payment exists for this order. Please complete a new payment.");
        }

        // Defense in depth: re-validate the stored signature (deterministic
        // local HMAC - this can only fail if the ledger was tampered with).
        if (order.getPaymentId() != null && order.getSignature() != null) {
            boolean stillValid;
            try {
                stillValid = Utils.verifyPaymentSignature(
                        verificationAttributes(order.getOrderId(), order.getPaymentId(), order.getSignature()),
                        razorpayProperties.getKeySecret());
            } catch (RazorpayException ex) {
                log.warn("Re-verification aborted for order {}: {}", request.getOrderId(), ex.getMessage());
                throw new IllegalArgumentException("Payment verification failed: invalid stored payment data");
            }
            if (!stillValid) {
                log.warn("Stored payment signature failed re-verification for order {}", request.getOrderId());
                throw new IllegalArgumentException("Payment signature verification failed");
            }
        }

        EnrollmentStatus enrollmentStatus =
                request.getCourseId() != null ? activateEnrollment(request.getCourseId()) : null;
        log.info("Payment re-verified for order {} (payment {}), enrollment status: {}",
                order.getOrderId(), order.getPaymentId(), enrollmentStatus);
        return PaymentVerificationResponse.builder()
                .verified(true)
                .orderId(order.getOrderId())
                .paymentId(order.getPaymentId())
                .enrollmentStatus(enrollmentStatus)
                .build();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Binds the persisted order to the authenticated user and (when given) the course. */
    private void assertOwnedAndMatchesCourse(PaymentOrder order, Long courseId) {
        Long userId = SecurityUtils.currentUser().getUser().getId();
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Payment order does not belong to the authenticated user");
        }
        if (courseId != null && !order.getCourseId().equals(courseId)) {
            throw new IllegalArgumentException("Payment order does not match the course");
        }
    }

    /** Records the verified payment on the order (idempotent). */
    private void markPaid(PaymentOrder order, String paymentId, String signature) {
        order.setPaymentId(paymentId);
        order.setSignature(signature);
        order.setStatus(PaymentOrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        paymentOrderRepository.save(order);
    }

    /**
     * Activates the authenticated user's PENDING enrollment for the given
     * course via the existing enrollment service API. Idempotent: a
     * non-PENDING enrollment is reported but never modified.
     */
    private EnrollmentStatus activateEnrollment(Long courseId) {
        Long userId = SecurityUtils.currentUser().getUser().getId();
        EnrollmentResponse enrollment = enrollmentService.getEnrollmentsByUserId(userId).stream()
                .filter(e -> courseId.equals(e.getCourseId()))
                .findFirst()
                .orElse(null);
        if (enrollment == null) {
            return null;
        }
        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            return enrollment.getStatus();
        }
        EnrollmentResponse updated = enrollmentService.updateEnrollmentStatus(
                enrollment.getId(),
                EnrollmentRequest.builder()
                        .userId(userId)
                        .courseId(courseId)
                        .status(EnrollmentStatus.ACTIVE)
                        .build());
        return updated.getStatus();
    }

    /** The exact attribute keys the Razorpay SDK expects for signature checks. */
    private static JSONObject verificationAttributes(String orderId, String paymentId, String signature) {
        return new JSONObject()
                .put("razorpay_order_id", orderId)
                .put("razorpay_payment_id", paymentId)
                .put("razorpay_signature", signature);
    }

    /** True when the order is recent enough to be reused for a fresh checkout. */
    private static boolean isFreshForReuse(PaymentOrder order) {
        return order.getCreatedAt() != null
                && order.getCreatedAt().isAfter(LocalDateTime.now().minusHours(ORDER_REUSE_MAX_AGE_HOURS));
    }

    /**
     * Converts a rupee price to the smallest currency unit (paise).
     * {@code long} is used because the price column (precision 10, scale 2)
     * can exceed Integer.MAX_VALUE paise for very high-priced courses.
     */
    private long toPaise(BigDecimal price) {
        return price.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private PaymentOrderResponse toResponse(PaymentOrder order, Course course) {
        return PaymentOrderResponse.builder()
                .orderId(order.getOrderId())
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .amount(order.getAmountPaise())
                .currency(order.getCurrency())
                .receipt(order.getReceipt())
                .status(order.getStatus().name().toLowerCase())
                .keyId(razorpayProperties.getKeyId())
                .createdAt(order.getCreatedAt() == null
                        ? null
                        : order.getCreatedAt().atZone(ZoneId.systemDefault()).toEpochSecond())
                .build();
    }

    private static Object opt(Order order, String key) {
        JSONObject json = order.toJson();
        return json == null ? null : json.opt(key);
    }

    private static String optString(Order order, String key) {
        Object value = opt(order, key);
        return value == null ? null : String.valueOf(value);
    }
}
