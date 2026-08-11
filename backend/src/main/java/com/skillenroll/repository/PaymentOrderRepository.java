package com.skillenroll.repository;

import com.skillenroll.entity.PaymentOrder;
import com.skillenroll.enums.PaymentOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access for {@link PaymentOrder}. Database access only - no business logic.
 */
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByOrderId(String orderId);

    /** Newest unpaid order for a user/course pair (used to reuse it on re-checkout). */
    Optional<PaymentOrder> findFirstByUserIdAndCourseIdAndStatusOrderByCreatedAtDesc(
            Long userId, Long courseId, PaymentOrderStatus status);
}
