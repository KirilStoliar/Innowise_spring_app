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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = {"ADMIN"})
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
    void createPayment_ExternalApiSuccess_ReturnsCompleted() throws Exception {
        // Given
        wireMockServer.stubFor(get(urlMatching("/integers.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("42")));

        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .userId(1L)
                .paymentAmount(new BigDecimal("100.50"))
                .build();

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("COMPLETED"));

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payments.get(0).getId()).isNotNull();
    }

    @Test
    void getTotalSumByUserIdAndDateRange_Success() throws Exception {
        // Given
        // Создаем платежи для пользователя 1
        Payment payment1 = createPayment(1L, 1L, new BigDecimal("100.00"),
                LocalDateTime.of(2026, 1, 28, 12, 0, 0));
        Payment payment2 = createPayment(2L, 1L, new BigDecimal("200.00"),
                LocalDateTime.of(2026, 1, 28, 14, 0, 0));
        Payment payment3 = createPayment(3L, 1L, new BigDecimal("50.00"),
                LocalDateTime.of(2026, 1, 27, 10, 0, 0)); // Вне диапазона

        // Создаем платеж для другого пользователя
        Payment payment4 = createPayment(4L, 2L, new BigDecimal("300.00"),
                LocalDateTime.of(2026, 1, 28, 15, 0, 0));

        String startDate = "2026-01-28T10:00:00";
        String endDate = "2026-01-28T18:00:00";

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/user/{userId}/total", 1L)
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(300.00)); // 100 + 200
    }

    @Test
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
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(0.00));
    }

    @Test
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
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(300.00)); // 100 + 200
    }

    @Test
    void getTotalSumByDateRange_EmptyResult() throws Exception {
        // Given
        createPayment(1L, 1L, new BigDecimal("100.00"),
                LocalDateTime.of(2026, 1, 27, 10, 0, 0)); // Вне диапазона

        String startDate = "2026-01-28T10:00:00";
        String endDate = "2026-01-28T18:00:00";

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/total")
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(0.00));
    }

    @Test
    void searchPaymentsByCriteria_ReturnsFilteredResults() throws Exception {
        // Given
        Payment payment1 = createPayment(1L, 100L, new BigDecimal("100.00"),
                LocalDateTime.now(), PaymentStatus.COMPLETED);
        Payment payment2 = createPayment(1L, 200L, new BigDecimal("200.00"),
                LocalDateTime.now(), PaymentStatus.FAILED);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/search")
                        .param("orderId", "1")
                        .param("status", "COMPLETED")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].userId").value(100L));
    }

    @Test
    void getPaymentById_Exists_ReturnsPayment() throws Exception {
        // Given
        String paymentId = new ObjectId().toString();
        Payment payment = createPaymentWithId(paymentId, 1L, 5L, new BigDecimal("50.00"),
                LocalDateTime.now());

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/{id}", paymentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(paymentId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.orderId").value(1L));
    }

    @Test
    void getPaymentsByUserId_ReturnsFilteredPayments() throws Exception {
        // Given
        createPayment(1L, 100L, new BigDecimal("100.00"), LocalDateTime.now());
        createPayment(2L, 100L, new BigDecimal("200.00"), LocalDateTime.now());
        createPayment(3L, 200L, new BigDecimal("300.00"), LocalDateTime.now());

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/user/{userId}", 100L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(2));
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