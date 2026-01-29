package com.stoliar.repository;

import com.stoliar.entity.Payment;
import com.stoliar.entity.enums.PaymentStatus;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    List<Payment> findByUserId(Long userId);

    List<Payment> findByOrderId(Long orderId);

    List<Payment> findByStatus(PaymentStatus status);

    // Поиск по user_id ИЛИ order_id ИЛИ status
    @Query("{" +
            "$and: [" +
            "?#{ [0] == null ? { $where: 'true' } : { 'userId' : [0] } }," +
            "?#{ [1] == null ? { $where: 'true' } : { 'orderId' : [1] } }," +
            "?#{ [2] == null ? { $where: 'true' } : { 'status' : [2] } }" +
            "]" +
            "}")
    List<Payment> findPaymentsByCriteria(Long userId, Long orderId, PaymentStatus status);

    // Для подсчета суммы по пользователю и диапазону дат
    @Query("{ userId: ?0, timestamp: { $gte: ?1, $lte: ?2 } }")
    List<Payment> findByUserIdAndTimestampBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    // Для подсчета суммы по диапазону дат для всех пользователей
    @Query("{ timestamp: { $gte: ?0, $lte: ?1 } }")
    List<Payment> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);
}