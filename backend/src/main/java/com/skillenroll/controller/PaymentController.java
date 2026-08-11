package com.skillenroll.controller;

import com.razorpay.RazorpayException;
import com.skillenroll.dto.PaymentOrderRequest;
import com.skillenroll.dto.PaymentOrderResponse;
import com.skillenroll.dto.PaymentReVerificationRequest;
import com.skillenroll.dto.PaymentVerificationRequest;
import com.skillenroll.dto.PaymentVerificationResponse;
import com.skillenroll.service.interfaces.PaymentService;
import com.skillenroll.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for {@code /api/payment}. Thin controller - delegates all
 * business logic to {@link PaymentService}.
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(
            summary = "Create a Razorpay payment order for a course",
            description = "Creates a Razorpay order for the given course. The order amount is derived "
                    + "server-side from the course's price (in paise, INR) and never from the request body. "
                    + "The response carries the order id, amount, currency, receipt, status and the public "
                    + "Razorpay key id needed by the client checkout. The Razorpay key secret is never "
                    + "returned. Requires authentication.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Payment order details. Only courseId is required; the amount is taken "
                            + "from the course's server-side price.",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "CreatePaymentOrderRequest",
                                    summary = "Example request",
                                    value = """
                                            {"courseId":1}
                                            """))))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment order created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "CreatePaymentOrderResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Payment order created successfully","data":{"orderId":"order_P1abcXYZ123def","courseId":1,"courseTitle":"Spring Boot Masterclass","amount":4999,"currency":"INR","receipt":"course-1-1723351234567","status":"created","keyId":"rzp_test_xxxxxxxx","createdAt":1723351234},"timestamp":"2026-08-11T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or the course is free"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Course not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Payment gateway error - Razorpay could not create the order")
    })
    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> createOrder(
            @Valid @RequestBody PaymentOrderRequest request) throws RazorpayException {
        PaymentOrderResponse order = paymentService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment order created successfully", order));
    }

    @Operation(
            summary = "Verify a Razorpay payment",
            description = "Validates the Razorpay signature (HMAC-SHA256 of orderId|paymentId signed with the "
                    + "configured key secret). The signature is verified entirely server-side - it is never "
                    + "accepted on trust from the client. The order must exist in the persisted payment ledger "
                    + "and belong to the authenticated user and (when provided) the claimed course. On "
                    + "success the order is marked PAID and, when courseId is provided, the authenticated "
                    + "user's PENDING enrollment for that course is activated. Invalid signatures return 400. "
                    + "Requires authentication.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Verification details from the Razorpay Checkout response, plus the "
                            + "optional courseId to activate the enrollment.",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "VerifyPaymentRequest",
                                    summary = "Example request",
                                    value = """
                                            {"orderId":"order_P1abcXYZ123def","paymentId":"pay_9A2B3C4D5E6F7G","signature":"0d14a7f2c8e1b9d3f6a4c2e8b1d5f7a9c3e6d0b2f4a8c1e3b5d7f9a0c2e4b6d8","courseId":1}
                                            """))))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment verified successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "VerifyPaymentResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Payment verified successfully","data":{"verified":true,"orderId":"order_P1abcXYZ123def","paymentId":"pay_9A2B3C4D5E6F7G","enrollmentStatus":"ACTIVE"},"timestamp":"2026-08-11T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Order not found, order/user or order/course mismatch, invalid or malformed verification data, or signature mismatch"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<PaymentVerificationResponse>> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequest request) {
        PaymentVerificationResponse verification = paymentService.verifyPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Payment verified successfully", verification));
    }

    @Operation(
            summary = "Re-verify a captured Razorpay payment",
            description = "Re-confirms a payment that was captured but whose initial verification attempt may have "
                    + "failed (e.g. a transient error). Uses the persisted order record - the payment id and "
                    + "signature are read from the ledger, so the client only sends the order id and no new "
                    + "Razorpay order is created (no double charge). Retries the enrollment activation. "
                    + "Returns 409 when the order has no verified payment recorded. Requires authentication.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Order id plus the optional courseId whose PENDING enrollment should be activated.",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "ReVerifyPaymentRequest",
                                    summary = "Example request",
                                    value = """
                                            {"orderId":"order_P1abcXYZ123def","courseId":1}
                                            """))))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment re-verified successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "ReVerifyPaymentResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Payment re-verified successfully","data":{"verified":true,"orderId":"order_P1abcXYZ123def","paymentId":"pay_9A2B3C4D5E6F7G","enrollmentStatus":"ACTIVE"},"timestamp":"2026-08-11T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Order not found, order/user or order/course mismatch, or invalid stored payment data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "No verified payment recorded for this order")
    })
    @PostMapping("/re-verify")
    public ResponseEntity<ApiResponse<PaymentVerificationResponse>> reVerifyPayment(
            @Valid @RequestBody PaymentReVerificationRequest request) {
        PaymentVerificationResponse verification = paymentService.reVerifyPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Payment re-verified successfully", verification));
    }
}
