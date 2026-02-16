package com.stoliar.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoliar.dto.PaymentRequest;
import com.stoliar.entity.Payment;
import com.stoliar.entity.enums.PaymentStatus;
import com.stoliar.repository.PaymentRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "use.testcontainers", matches = "true")
class PaymentServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(Payment.class);
        if (wireMockServer != null) {
            wireMockServer.resetAll();
        }
    }

    @Test
    @WithMockUser(username = "1", authorities = {"ROLE_USER"})
    void createPayment_ExternalApiSuccess_ReturnsCompleted() throws Exception {
        // Given
        wireMockServer.stubFor(get(urlMatching("/integers.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("42")));

        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .paymentAmount(new BigDecimal("100.50"))
                .build();

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.userId").value(1L)); // userId из @WithMockUser

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payments.get(0).getId()).isNotNull();
        assertThat(payments.get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    @WithMockUser(username = "1", authorities = {"ROLE_ADMIN"})
    void createPayment_AsAdminWithTargetUserId() throws Exception {
        // Given
        wireMockServer.stubFor(get(urlMatching("/integers.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("42")));

        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .paymentAmount(new BigDecimal("100.50"))
                .build();

        Long targetUserId = 100L;

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/payments")
                        .param("userId", targetUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.userId").value(targetUserId)); // targetUserId из параметра

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getUserId()).isEqualTo(targetUserId);
    }

    @Test
    @WithMockUser(username = "1", authorities = {"ROLE_USER"})
    void getTotalSumByUserIdAndDateRange_Success() throws Exception {
        // Given
        Payment payment1 = createPayment(1L, 1L, new BigDecimal("100.00"),
                LocalDateTime.of(2026, 1, 28, 12, 0, 0));
        Payment payment2 = createPayment(2L, 1L, new BigDecimal("200.00"),
                LocalDateTime.of(2026, 1, 28, 14, 0, 0));
        Payment payment3 = createPayment(3L, 1L, new BigDecimal("50.00"),
                LocalDateTime.of(2026, 1, 27, 10, 0, 0)); // Вне диапазона

        // Создаем платеж для другого пользователя
        createPayment(4L, 2L, new BigDecimal("300.00"),
                LocalDateTime.of(2026, 1, 28, 15, 0, 0));

        String startDate = "2026-01-28T10:00:00";
        String endDate = "2026-01-28T18:00:00";

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/user/{userId}/total", 1L)
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(300.00)); // 100 + 200
    }

    @Test
    @WithMockUser(username = "1", authorities = {"ROLE_USER"})
    void getTotalSumByUserIdAndDateRange_NoPaymentsInRange() throws Exception {
        // Given
        createPayment(1L, 1L, new BigDecimal("100.00"),
                LocalDateTime.of(2026, 1, 27, 10, 0, 0)); // Вне диапазона

        String startDate = "2026-01-28T10:00:00";
        String endDate = "2026-01-28T18:00:00";

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/user/{userId}/total", 1L)
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(0.00));
    }

    @Test
    @WithMockUser(username = "1", authorities = {"ROLE_USER"})
    void getTotalSumByUserIdAndDateRange_AccessDenied_OtherUser() throws Exception {
        // Given
        Long userId = 1L;
        Long otherUserId = 2L;
        createPayment(1L, otherUserId, new BigDecimal("100.00"), LocalDateTime.now());

        String startDate = "2026-01-28T10:00:00";
        String endDate = "2026-01-28T18:00:00";

        // When & Then - пользователь пытается получить сумму другого пользователя
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/user/{userId}/total", otherUserId)
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", authorities = {"ROLE_ADMIN"})
    void getTotalSumByUserIdAndDateRange_AsAdmin() throws Exception {
        // Given
        Long adminUserId = 1L;
        Long targetUserId = 2L;

        createPayment(1L, targetUserId, new BigDecimal("100.00"),
                LocalDateTime.of(2026, 1, 28, 12, 0, 0));
        createPayment(2L, targetUserId, new BigDecimal("200.00"),
                LocalDateTime.of(2026, 1, 28, 14, 0, 0));

        String startDate = "2026-01-28T10:00:00";
        String endDate = "2026-01-28T18:00:00";

        // When & Then - Админ может получить сумму другого пользователя
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/user/{userId}/total", targetUserId)
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(300.00));
    }

    @Test
    @WithMockUser(username = "1", authorities = {"ROLE_ADMIN"})
    void getTotalSumByDateRange_Success() throws Exception {
        // Given
        Payment payment1 = createPayment(1L, 1L, new BigDecimal("100.00"),
                LocalDateTime.of(2026, 1, 28, 12, 0, 0));
        Payment payment2 = createPayment(2L, 2L, new BigDecimal("200.00"),
                LocalDateTime.of(2026, 1, 28, 14, 0, 0));
        Payment payment3 = createPayment(3L, 3L, new BigDecimal("50.00"),
                LocalDateTime.of(2026, 1, 27, 10, 0, 0)); // Вне диапазона

        String startDate = "2026-01-28T10:00:00";
        String endDate = "2026-01-28T18:00:00";

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/total")
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(300.00)); // 100 + 200
    }

    @Test
    @WithMockUser(username = "1", authorities = {"ROLE_USER"})
    void getTotalSumByDateRange_AccessDenied_NonAdmin() throws Exception {
        // Given
        String startDate = "2026-01-28T10:00:00";
        String endDate = "2026-01-28T18:00:00";

        // When & Then - USER не может получить общую сумму
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/total")
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", authorities = {"ROLE_USER"})
    void searchPaymentsByCriteria_ReturnsOnlyOwnPayments() throws Exception {
        // Given
        Long userId = 1L;
        Long otherUserId = 2L;

        Payment payment1 = createPayment(1L, userId, new BigDecimal("100.00"),
                LocalDateTime.now(), PaymentStatus.COMPLETED);
        Payment payment2 = createPayment(1L, otherUserId, new BigDecimal("200.00"),
                LocalDateTime.now(), PaymentStatus.COMPLETED);
        Payment payment3 = createPayment(2L, userId, new BigDecimal("300.00"),
                LocalDateTime.now(), PaymentStatus.FAILED);

        // When & Then - USER видит только свои платежи
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/search")
                        .param("status", "COMPLETED")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].userId").value(userId))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(username = "1", authorities = {"ROLE_ADMIN"})
    void searchPaymentsByCriteria_AsAdmin_ReturnsAllPayments() throws Exception {
        // Given
        Long userId = 1L;
        Long otherUserId = 2L;

        Payment payment1 = createPayment(1L, userId, new BigDecimal("100.00"),
                LocalDateTime.now(), PaymentStatus.COMPLETED);
        Payment payment2 = createPayment(1L, otherUserId, new BigDecimal("200.00"),
                LocalDateTime.now(), PaymentStatus.COMPLETED);
        Payment payment3 = createPayment(2L, userId, new BigDecimal("300.00"),
                LocalDateTime.now(), PaymentStatus.FAILED);

        // When & Then - ADMIN видит все платежи
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/search")
                        .param("status", "COMPLETED")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(username = "5", authorities = {"ROLE_USER"})
    void getPaymentById_Exists_ReturnsOwnPayment() throws Exception {
        // Given
        String paymentId = new ObjectId().toString();
        Long userId = 5L;
        Payment payment = createPaymentWithId(paymentId, 1L, userId, new BigDecimal("50.00"),
                LocalDateTime.now());

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/{id}", paymentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(paymentId))
                .andExpect(jsonPath("$.data.orderId").value(1L))
                .andExpect(jsonPath("$.data.userId").value(userId));
    }

    @Test
    @WithMockUser(username = "5", authorities = {"ROLE_USER"})
    void getPaymentById_AccessDenied_OtherUsersPayment() throws Exception {
        // Given
        String paymentId = new ObjectId().toString();
        Long otherUserId = 10L;
        createPaymentWithId(paymentId, 1L, otherUserId, new BigDecimal("50.00"),
                LocalDateTime.now());

        // When & Then - USER пытается получить чужой платеж
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/{id}", paymentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", authorities = {"ROLE_ADMIN"})
    void getPaymentById_AsAdmin_ReturnsAnyPayment() throws Exception {
        // Given
        String paymentId = new ObjectId().toString();
        Long otherUserId = 10L;
        Payment payment = createPaymentWithId(paymentId, 1L, otherUserId, new BigDecimal("50.00"),
                LocalDateTime.now());

        // When & Then - ADMIN может получить любой платеж
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/{id}", paymentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(paymentId));
    }

    @Test
    @WithMockUser(username = "100", authorities = {"ROLE_USER"})
    void getPaymentsByUserId_ReturnsOwnPayments() throws Exception {
        // Given
        Long userId = 100L;
        createPayment(1L, userId, new BigDecimal("100.00"), LocalDateTime.now());
        createPayment(2L, userId, new BigDecimal("200.00"), LocalDateTime.now());
        createPayment(3L, 200L, new BigDecimal("300.00"), LocalDateTime.now());

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/user/{userId}", userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(username = "100", authorities = {"ROLE_USER"})
    void getPaymentsByUserId_AccessDenied_OtherUser() throws Exception {
        // Given
        Long userId = 100L;
        Long otherUserId = 200L;
        createPayment(1L, otherUserId, new BigDecimal("100.00"), LocalDateTime.now());

        // When & Then - USER пытается получить платежи другого пользователя
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/user/{userId}", otherUserId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", authorities = {"ROLE_ADMIN"})
    void getPaymentsByUserId_AsAdmin_ReturnsAnyUserPayments() throws Exception {
        // Given
        Long userId = 100L;
        createPayment(1L, userId, new BigDecimal("100.00"), LocalDateTime.now());
        createPayment(2L, userId, new BigDecimal("200.00"), LocalDateTime.now());

        // When & Then - ADMIN может получить платежи любого пользователя
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/user/{userId}", userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(username = "1", authorities = {"ROLE_USER"})
    void getPaymentsByOrderId_ReturnsOnlyOwnPayments() throws Exception {
        // Given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long orderId = 100L;

        createPayment(orderId, userId, new BigDecimal("100.00"), LocalDateTime.now());
        createPayment(orderId, otherUserId, new BigDecimal("200.00"), LocalDateTime.now());

        // When & Then - USER видит только свои платежи по заказу
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/order/{orderId}", orderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].userId").value(userId));
    }

    @Test
    @WithMockUser(username = "1", authorities = {"ROLE_ADMIN"})
    void getPaymentsByOrderId_AsAdmin_ReturnsAllPayments() throws Exception {
        // Given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long orderId = 100L;

        createPayment(orderId, userId, new BigDecimal("100.00"), LocalDateTime.now());
        createPayment(orderId, otherUserId, new BigDecimal("200.00"), LocalDateTime.now());

        // When & Then - ADMIN видит все платежи по заказу
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/order/{orderId}", orderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    private Payment createPayment(Long orderId, Long userId, BigDecimal amount, LocalDateTime timestamp) {
        return createPayment(orderId, userId, amount, timestamp, PaymentStatus.COMPLETED);
    }

    private Payment createPayment(Long orderId, Long userId, BigDecimal amount,
                                  LocalDateTime timestamp, PaymentStatus status) {
        Payment payment = Payment.builder()
                .id(new ObjectId().toString())
                .orderId(orderId)
                .userId(userId)
                .status(status)
                .paymentAmount(amount)
                .timestamp(timestamp)
                .build();
        return mongoTemplate.save(payment);
    }

    private Payment createPaymentWithId(String id, Long orderId, Long userId,
                                        BigDecimal amount, LocalDateTime timestamp) {
        Payment payment = Payment.builder()
                .id(id)
                .orderId(orderId)
                .userId(userId)
                .status(PaymentStatus.COMPLETED)
                .paymentAmount(amount)
                .timestamp(timestamp)
                .build();
        return mongoTemplate.save(payment);
    }
}