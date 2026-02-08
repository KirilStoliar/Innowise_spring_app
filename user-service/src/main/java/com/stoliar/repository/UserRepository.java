package com.stoliar.repository;

import com.stoliar.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    // NATIVE SQL QUERY WITH RETURNING
    @Query(value = """
    INSERT INTO users (name, surname, birth_date, email, active, created_at, updated_at)
    VALUES (:name, :surname, :birthDate, :email, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    RETURNING *
    """, nativeQuery = true)
    User createUser(@Param("name") String name,
                    @Param("surname") String surname,
                    @Param("birthDate") LocalDate birthDate,
                    @Param("email") String email);

    // Подсчет активных карт пользователя
    @Query(value = "SELECT COUNT(*) FROM payment_cards WHERE user_id = :userId AND active = true",
            nativeQuery = true)
    int countActiveCardsByUserId(@Param("userId") Long userId);

    // NAMED METHODS
    User findUserById(Long id);
    boolean existsByEmail(String email);

    // SPECIFICATION METHODS
    Page<User> findAll(Specification<User> spec, Pageable pageable);

    @Query("SELECT u.id FROM User u WHERE u.email = :email")
    Optional<Long> findIdByEmail(@Param("email") String email);
}