package com.stoliar.repository;

import com.stoliar.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    // GET BY ID (только неудаленные)
    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.deleted = false")
    Optional<Order> findByIdAndNotDeleted(@Param("id") Long id);

    // GET ORDERS BY USER ID (только неудаленные)
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.deleted = false")
    Page<Order> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.deleted = false")
    List<Order> findAllByUserId(@Param("userId") Long userId);

    // DELETE BY ID (soft delete)
    @Modifying
    @Query("UPDATE Order o SET o.deleted = true WHERE o.id = :id")
    void softDeleteById(@Param("id") Long id);

    // Проверка существования (только неудаленные)
    @Query("SELECT COUNT(o) > 0 FROM Order o WHERE o.id = :id AND o.deleted = false")
    boolean existsByIdAndNotDeleted(@Param("id") Long id);
}