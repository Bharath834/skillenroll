package com.skillenroll.service.impl;

import com.razorpay.Order;
import com.razorpay.OrderClient;
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
import com.skillenroll.entity.User;
import com.skillenroll.enums.EnrollmentStatus;
import com.skillenroll.enums.PaymentOrderStatus;
import com.skillenroll.enums.Role;
import com.skillenroll.exception.DuplicateResourceException;
import com.skillenroll.exception.ResourceNotFoundException;
import com.skillenroll.repository.CourseRepository;
import com.skillenroll.repository.PaymentOrderRepository;
import com.skillenroll.security.service.CustomUserDetails;
import com.skillenroll.service.interfaces.EnrollmentService;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PaymentServiceImpl}. The Razorpay SDK is never called:
 * a mocked {@link OrderClient} is injected into the client's public
 * {@code orders} field and the static signature check is mocked, so no network
 * traffic ever happens. The payment ledger is a mocked repository.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final String KEY_ID = "rzp_test_xxxxxxxx";
    private static final String KEY_SECRET = "test-secret";

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private OrderClient orderClient;

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    private RazorpayClient razorpayClient;
    private RazorpayProperties razorpayProperties;
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() throws Exception {
        // Constructing the client performs no network calls; only the mocked
        // OrderClient is used for the order-creation call.
        razorpayClient = new RazorpayClient(KEY_ID, KEY_SECRET);
        Field ordersField = RazorpayClient.class.getField("orders");
        ordersField.set(razorpayClient, orderClient);

        razorpayProperties = new RazorpayProperties();
        razorpayProperties.setKeyId(KEY_ID);
        razorpayProperties.setKeySecret(KEY_SECRET);

        paymentService = new PaymentServiceImpl(courseRepository, razorpayClient,
                razorpayProperties, enrollmentService, paymentOrderRepository);

        // Authenticated learner (id 1) for the verification/enrollment tests.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        CustomUserDetails.from(User.builder().id(1L)
                                .email("learner@test.com")
                                .role(Role.STUDENT)
                                .build()),
                        null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // createOrder
    // ------------------------------------------------------------------

    @Test
    void createOrder_shouldPersistOrderAndMapResponse() throws Exception {
        Course course = course(1L, "Spring Boot Masterclass", new BigDecimal("49.99"));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        Order createdOrder = new Order(new JSONObject()
                .put("id", "order_test123")
                .put("amount", 4999)
                .put("currency", "INR")
                .put("receipt", "course-1-1723351234567")
                .put("status", "created")
                .put("created_at", 1723351234L));
        when(orderClient.create(any(JSONObject.class))).thenReturn(createdOrder);
        when(paymentOrderRepository.save(any(PaymentOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentOrderResponse response =
                paymentService.createOrder(PaymentOrderRequest.builder().courseId(1L).build());

        assertThat(response.getOrderId()).isEqualTo("order_test123");
        assertThat(response.getCourseId()).isEqualTo(1L);
        assertThat(response.getCourseTitle()).isEqualTo("Spring Boot Masterclass");
        assertThat(response.getAmount()).isEqualTo(4999L);
        assertThat(response.getCurrency()).isEqualTo("INR");
        assertThat(response.getReceipt()).startsWith("course-1-");
        assertThat(response.getKeyId()).isEqualTo(KEY_ID);

        // The request to Razorpay must carry the server-derived amount.
        ArgumentCaptor<JSONObject> requestCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(orderClient).create(requestCaptor.capture());
        JSONObject sent = requestCaptor.getValue();
        assertThat(sent.getLong("amount")).isEqualTo(4999L);
        assertThat(sent.getString("currency")).isEqualTo("INR");
        assertThat(sent.getJSONObject("notes").getLong("courseId")).isEqualTo(1L);

        // The order must be persisted with owner/course/amount and status CREATED.
        ArgumentCaptor<PaymentOrder> persistCaptor = ArgumentCaptor.forClass(PaymentOrder.class);
        verify(paymentOrderRepository).save(persistCaptor.capture());
        PaymentOrder persisted = persistCaptor.getValue();
        assertThat(persisted.getOrderId()).isEqualTo("order_test123");
        assertThat(persisted.getUserId()).isEqualTo(1L);
        assertThat(persisted.getCourseId()).isEqualTo(1L);
        assertThat(persisted.getAmountPaise()).isEqualTo(4999L);
        assertThat(persisted.getCurrency()).isEqualTo("INR");
        assertThat(persisted.getReceipt()).startsWith("course-1-");
        assertThat(persisted.getStatus()).isEqualTo(PaymentOrderStatus.CREATED);
    }

    @Test
    void createOrder_shouldReuseExistingUnpaidOrder() throws Exception {
        Course course = course(1L, "Spring Boot Masterclass", new BigDecimal("49.99"));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        PaymentOrder existing = persistedOrder(1L, 1L, PaymentOrderStatus.CREATED);
        when(paymentOrderRepository.findFirstByUserIdAndCourseIdAndStatusOrderByCreatedAtDesc(
                1L, 1L, PaymentOrderStatus.CREATED)).thenReturn(Optional.of(existing));

        PaymentOrderResponse response =
                paymentService.createOrder(PaymentOrderRequest.builder().courseId(1L).build());

        assertThat(response.getOrderId()).isEqualTo("order_test123");
        assertThat(response.getAmount()).isEqualTo(4999L);
        verify(orderClient, never()).create(any(JSONObject.class));
        verify(paymentOrderRepository, never()).save(any(PaymentOrder.class));
    }

    @Test
    void createOrder_priceChanged_shouldCreateNewOrderInsteadOfReusing() throws Exception {
        Course course = course(1L, "Spring Boot Masterclass", new BigDecimal("49.99"));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        // An older unpaid order exists but for a different amount (price changed).
        PaymentOrder stale = persistedOrder(1L, 1L, PaymentOrderStatus.CREATED);
        stale.setAmountPaise(2999L);
        when(paymentOrderRepository.findFirstByUserIdAndCourseIdAndStatusOrderByCreatedAtDesc(
                1L, 1L, PaymentOrderStatus.CREATED)).thenReturn(Optional.of(stale));

        Order createdOrder = new Order(new JSONObject()
                .put("id", "order_test456")
                .put("amount", 4999)
                .put("currency", "INR")
                .put("receipt", "course-1-abc")
                .put("status", "created")
                .put("created_at", 1723351234L));
        when(orderClient.create(any(JSONObject.class))).thenReturn(createdOrder);
        when(paymentOrderRepository.save(any(PaymentOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentOrderResponse response =
                paymentService.createOrder(PaymentOrderRequest.builder().courseId(1L).build());

        // A fresh order at the current price must be created, not the stale one reused.
        assertThat(response.getOrderId()).isEqualTo("order_test456");
        assertThat(response.getAmount()).isEqualTo(4999L);
        verify(orderClient).create(any(JSONObject.class));
    }

    @Test
    void createOrder_staleUnpaidOrder_shouldCreateFreshOrder() throws Exception {
        Course course = course(1L, "Spring Boot Masterclass", new BigDecimal("49.99"));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        // An unpaid order beyond the 24h reuse window must NOT be reused.
        PaymentOrder stale = persistedOrder(1L, 1L, PaymentOrderStatus.CREATED);
        stale.setCreatedAt(LocalDateTime.now().minusDays(3));
        when(paymentOrderRepository.findFirstByUserIdAndCourseIdAndStatusOrderByCreatedAtDesc(
                1L, 1L, PaymentOrderStatus.CREATED)).thenReturn(Optional.of(stale));

        Order createdOrder = new Order(new JSONObject()
                .put("id", "order_test789")
                .put("amount", 4999)
                .put("currency", "INR")
                .put("receipt", "course-1-abc")
                .put("status", "created")
                .put("created_at", 1723351234L));
        when(orderClient.create(any(JSONObject.class))).thenReturn(createdOrder);
        when(paymentOrderRepository.save(any(PaymentOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentOrderResponse response =
                paymentService.createOrder(PaymentOrderRequest.builder().courseId(1L).build());

        // A fresh order must be created, not the stale one reused.
        assertThat(response.getOrderId()).isEqualTo("order_test789");
        verify(orderClient).create(any(JSONObject.class));
    }

    @Test
    void createOrder_courseNotFound_shouldThrowResourceNotFound() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createOrder(PaymentOrderRequest.builder().courseId(99L).build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createOrder_freeCourse_shouldThrowIllegalArgument() {
        Course course = course(2L, "Free Course", BigDecimal.ZERO);
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> paymentService.createOrder(PaymentOrderRequest.builder().courseId(2L).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("free");
    }

    @Test
    void createOrder_razorpayFailure_shouldPropagateRazorpayException() throws Exception {
        Course course = course(1L, "Spring Boot Masterclass", new BigDecimal("49.99"));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(orderClient.create(any(JSONObject.class))).thenThrow(new RazorpayException("order creation failed"));

        assertThatThrownBy(() -> paymentService.createOrder(PaymentOrderRequest.builder().courseId(1L).build()))
                .isInstanceOf(RazorpayException.class);
    }

    // ------------------------------------------------------------------
    // verifyPayment
    // ------------------------------------------------------------------

    @Test
    void verifyPayment_validSignature_shouldMarkPaidAndActivateEnrollment() {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            JSONObject[] capturedAttributes = new JSONObject[1];
            utils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                    .thenAnswer(invocation -> {
                        capturedAttributes[0] = invocation.getArgument(0);
                        return true;
                    });
            when(paymentOrderRepository.findByOrderId("order_test123"))
                    .thenReturn(Optional.of(persistedOrder(1L, 1L, PaymentOrderStatus.CREATED)));

            EnrollmentResponse pending = EnrollmentResponse.builder()
                    .id(5L).userId(1L).courseId(1L).status(EnrollmentStatus.PENDING).build();
            EnrollmentResponse active = EnrollmentResponse.builder()
                    .id(5L).userId(1L).courseId(1L).status(EnrollmentStatus.ACTIVE).build();
            when(enrollmentService.getEnrollmentsByUserId(1L)).thenReturn(List.of(pending));
            when(enrollmentService.updateEnrollmentStatus(eq(5L), any(EnrollmentRequest.class)))
                    .thenReturn(active);

            PaymentVerificationResponse response =
                    paymentService.verifyPayment(verificationRequest(1L));

            assertThat(response.isVerified()).isTrue();
            assertThat(response.getOrderId()).isEqualTo("order_test123");
            assertThat(response.getPaymentId()).isEqualTo("pay_test123");
            assertThat(response.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.ACTIVE);

            // The SDK must receive the exact checkout attribute keys Razorpay expects.
            assertThat(capturedAttributes[0].getString("razorpay_order_id")).isEqualTo("order_test123");
            assertThat(capturedAttributes[0].getString("razorpay_payment_id")).isEqualTo("pay_test123");
            assertThat(capturedAttributes[0].getString("razorpay_signature")).isEqualTo("0123456789abcdef");

            // The order must be persisted as PAID with the payment details.
            ArgumentCaptor<PaymentOrder> persistCaptor = ArgumentCaptor.forClass(PaymentOrder.class);
            verify(paymentOrderRepository).save(persistCaptor.capture());
            assertThat(persistCaptor.getValue().getStatus()).isEqualTo(PaymentOrderStatus.PAID);
            assertThat(persistCaptor.getValue().getPaymentId()).isEqualTo("pay_test123");
            assertThat(persistCaptor.getValue().getSignature()).isEqualTo("0123456789abcdef");

            // The enrollment update must request ACTIVE for the verified course.
            ArgumentCaptor<EnrollmentRequest> captor = ArgumentCaptor.forClass(EnrollmentRequest.class);
            verify(enrollmentService).updateEnrollmentStatus(eq(5L), captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
            assertThat(captor.getValue().getCourseId()).isEqualTo(1L);
        }
    }

    @Test
    void verifyPayment_validSignature_withoutCourseId_shouldSkipEnrollment() {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                    .thenReturn(true);
            when(paymentOrderRepository.findByOrderId("order_test123"))
                    .thenReturn(Optional.of(persistedOrder(1L, 1L, PaymentOrderStatus.CREATED)));

            PaymentVerificationResponse response = paymentService.verifyPayment(verificationRequest(null));

            assertThat(response.isVerified()).isTrue();
            assertThat(response.getEnrollmentStatus()).isNull();
            verify(enrollmentService, never()).getEnrollmentsByUserId(any());
            verify(enrollmentService, never()).updateEnrollmentStatus(any(), any());
        }
    }

    @Test
    void verifyPayment_validSignature_withNoEnrollment_shouldNotActivate() {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                    .thenReturn(true);
            when(paymentOrderRepository.findByOrderId("order_test123"))
                    .thenReturn(Optional.of(persistedOrder(1L, 1L, PaymentOrderStatus.CREATED)));
            when(enrollmentService.getEnrollmentsByUserId(1L)).thenReturn(List.of());

            PaymentVerificationResponse response = paymentService.verifyPayment(verificationRequest(1L));

            assertThat(response.isVerified()).isTrue();
            assertThat(response.getEnrollmentStatus()).isNull();
            verify(enrollmentService, never()).updateEnrollmentStatus(any(), any());
        }
    }

    @Test
    void verifyPayment_validSignature_withAlreadyActiveEnrollment_shouldNotModify() {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                    .thenReturn(true);
            when(paymentOrderRepository.findByOrderId("order_test123"))
                    .thenReturn(Optional.of(persistedOrder(1L, 1L, PaymentOrderStatus.CREATED)));
            EnrollmentResponse active = EnrollmentResponse.builder()
                    .id(5L).userId(1L).courseId(1L).status(EnrollmentStatus.ACTIVE).build();
            when(enrollmentService.getEnrollmentsByUserId(1L)).thenReturn(List.of(active));

            PaymentVerificationResponse response = paymentService.verifyPayment(verificationRequest(1L));

            assertThat(response.isVerified()).isTrue();
            assertThat(response.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
            verify(enrollmentService, never()).updateEnrollmentStatus(any(), any());
        }
    }

    @Test
    void verifyPayment_orderNotFound_shouldThrowIllegalArgument() {
        when(paymentOrderRepository.findByOrderId("unknown_order")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.verifyPayment(
                PaymentVerificationRequest.builder()
                        .orderId("unknown_order").paymentId("pay").signature("sig").courseId(1L).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void verifyPayment_wrongUser_shouldThrowIllegalArgument() {
        when(paymentOrderRepository.findByOrderId("order_test123"))
                .thenReturn(Optional.of(persistedOrder(2L, 1L, PaymentOrderStatus.CREATED)));

        assertThatThrownBy(() -> paymentService.verifyPayment(verificationRequest(1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void verifyPayment_orderCourseMismatch_shouldThrowIllegalArgument() {
        when(paymentOrderRepository.findByOrderId("order_test123"))
                .thenReturn(Optional.of(persistedOrder(1L, 2L, PaymentOrderStatus.CREATED)));

        assertThatThrownBy(() -> paymentService.verifyPayment(verificationRequest(1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void verifyPayment_invalidSignature_shouldThrowIllegalArgument() {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                    .thenReturn(false);
            when(paymentOrderRepository.findByOrderId("order_test123"))
                    .thenReturn(Optional.of(persistedOrder(1L, 1L, PaymentOrderStatus.CREATED)));

            assertThatThrownBy(() -> paymentService.verifyPayment(verificationRequest(1L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("signature");
            verify(paymentOrderRepository, never()).save(any(PaymentOrder.class));
        }
    }

    @Test
    void verifyPayment_sdkError_shouldTranslateToIllegalArgument() {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                    .thenThrow(new RazorpayException("missing signature"));
            when(paymentOrderRepository.findByOrderId("order_test123"))
                    .thenReturn(Optional.of(persistedOrder(1L, 1L, PaymentOrderStatus.CREATED)));

            assertThatThrownBy(() -> paymentService.verifyPayment(verificationRequest(1L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("verification data");
        }
    }

    // ------------------------------------------------------------------
    // reVerifyPayment
    // ------------------------------------------------------------------

    @Test
    void reVerifyPayment_paidOrder_shouldRevalidateStoredSignatureAndActivateEnrollment() {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                    .thenReturn(true);
            PaymentOrder paid = persistedOrder(1L, 1L, PaymentOrderStatus.PAID);
            paid.setPaymentId("pay_test123");
            paid.setSignature("0123456789abcdef");
            when(paymentOrderRepository.findByOrderId("order_test123")).thenReturn(Optional.of(paid));

            EnrollmentResponse pending = EnrollmentResponse.builder()
                    .id(5L).userId(1L).courseId(1L).status(EnrollmentStatus.PENDING).build();
            EnrollmentResponse active = EnrollmentResponse.builder()
                    .id(5L).userId(1L).courseId(1L).status(EnrollmentStatus.ACTIVE).build();
            when(enrollmentService.getEnrollmentsByUserId(1L)).thenReturn(List.of(pending));
            when(enrollmentService.updateEnrollmentStatus(eq(5L), any(EnrollmentRequest.class)))
                    .thenReturn(active);

            PaymentVerificationResponse response =
                    paymentService.reVerifyPayment(reVerificationRequest(1L));

            assertThat(response.isVerified()).isTrue();
            assertThat(response.getPaymentId()).isEqualTo("pay_test123");
            assertThat(response.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
            verify(paymentOrderRepository, never()).save(any(PaymentOrder.class));
        }
    }

    @Test
    void reVerifyPayment_unpaidOrder_shouldThrowConflict() {
        when(paymentOrderRepository.findByOrderId("order_test123"))
                .thenReturn(Optional.of(persistedOrder(1L, 1L, PaymentOrderStatus.CREATED)));

        assertThatThrownBy(() -> paymentService.reVerifyPayment(reVerificationRequest(1L)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("No verified payment");
        verify(enrollmentService, never()).getEnrollmentsByUserId(any());
    }

    @Test
    void reVerifyPayment_paidOrder_withoutCourseId_shouldSkipEnrollment() {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                    .thenReturn(true);
            PaymentOrder paid = persistedOrder(1L, 1L, PaymentOrderStatus.PAID);
            paid.setPaymentId("pay_test123");
            paid.setSignature("0123456789abcdef");
            when(paymentOrderRepository.findByOrderId("order_test123")).thenReturn(Optional.of(paid));

            PaymentVerificationResponse response =
                    paymentService.reVerifyPayment(reVerificationRequest(null));

            assertThat(response.isVerified()).isTrue();
            assertThat(response.getEnrollmentStatus()).isNull();
            verify(enrollmentService, never()).getEnrollmentsByUserId(any());
        }
    }

    @Test
    void reVerifyPayment_paidOrderWithoutStoredPaymentDetails_shouldSkipSdkRevalidation() {
        // A PAID order whose payment details were not recorded must still
        // re-verify (defense-in-depth re-validation is skipped) and activate.
        PaymentOrder paid = persistedOrder(1L, 1L, PaymentOrderStatus.PAID);
        when(paymentOrderRepository.findByOrderId("order_test123")).thenReturn(Optional.of(paid));
        when(enrollmentService.getEnrollmentsByUserId(1L)).thenReturn(List.of());

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            PaymentVerificationResponse response =
                    paymentService.reVerifyPayment(reVerificationRequest(1L));

            assertThat(response.isVerified()).isTrue();
            utils.verify(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString()),
                    never());
        }
    }

    @Test
    void reVerifyPayment_wrongUser_shouldThrowIllegalArgument() {
        when(paymentOrderRepository.findByOrderId("order_test123"))
                .thenReturn(Optional.of(persistedOrder(2L, 1L, PaymentOrderStatus.PAID)));

        assertThatThrownBy(() -> paymentService.reVerifyPayment(reVerificationRequest(1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void reVerifyPayment_paidOrder_withNoEnrollment_shouldNotActivate() {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                    .thenReturn(true);
            PaymentOrder paid = persistedOrder(1L, 1L, PaymentOrderStatus.PAID);
            paid.setPaymentId("pay_test123");
            paid.setSignature("0123456789abcdef");
            when(paymentOrderRepository.findByOrderId("order_test123")).thenReturn(Optional.of(paid));
            when(enrollmentService.getEnrollmentsByUserId(1L)).thenReturn(List.of());

            PaymentVerificationResponse response =
                    paymentService.reVerifyPayment(reVerificationRequest(1L));

            assertThat(response.isVerified()).isTrue();
            assertThat(response.getEnrollmentStatus()).isNull();
            verify(enrollmentService, never()).updateEnrollmentStatus(any(), any());
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private PaymentVerificationRequest verificationRequest(Long courseId) {
        return PaymentVerificationRequest.builder()
                .orderId("order_test123")
                .paymentId("pay_test123")
                .signature("0123456789abcdef")
                .courseId(courseId)
                .build();
    }

    private PaymentReVerificationRequest reVerificationRequest(Long courseId) {
        return PaymentReVerificationRequest.builder()
                .orderId("order_test123")
                .courseId(courseId)
                .build();
    }

    private PaymentOrder persistedOrder(Long userId, Long courseId, PaymentOrderStatus status) {
        return PaymentOrder.builder()
                .id(1L)
                .orderId("order_test123")
                .userId(userId)
                .courseId(courseId)
                .amountPaise(4999L)
                .currency("INR")
                .receipt("course-1-abc")
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Course course(Long id, String title, BigDecimal price) {
        return Course.builder()
                .id(id)
                .title(title)
                .price(price)
                .build();
    }
}
