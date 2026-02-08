package com.stoliar.repository;

import com.stoliar.entity.Item;
import com.stoliar.entity.Order;
import com.stoliar.entity.OrderItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaAuditing
@EnabledIfSystemProperty(named = "use.testcontainers", matches = "true")
class OrderRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        boolean useTestcontainers = Boolean.parseBoolean(
                System.getProperty("use.testcontainers", "true")
        );

        if (useTestcontainers) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
            registry.add("spring.datasource.driver-class-name",
                    () -> "org.postgresql.Driver");
        }

        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration");
    }

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void saveOrder_ShouldPersistToDatabase() {
        // Arrange
        Item item = new Item();
        item.setName("Test Item");
        item.setPrice(50.0);
        entityManager.persist(item);

        Order order = new Order();
        order.setUserId(1L);
        order.setEmail("test@example.com");
        order.setStatus(Order.OrderStatus.PENDING);
        order.setTotalPrice(100.0);
        order.setDeleted(false);

        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setQuantity(2);
        orderItem.setOrder(order);

        order.setOrderItems(List.of(orderItem));

        // Act
        Order saved = orderRepository.save(order);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrderItems()).hasSize(1);
        assertThat(saved.getOrderItems().get(0).getItem().getName())
                .isEqualTo("Test Item");
    }
}
