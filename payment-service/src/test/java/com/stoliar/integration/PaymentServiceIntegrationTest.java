package com.stoliar.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoliar.dto.PaymentRequest;
import com.stoliar.entity.Payment;
import com.stoliar.entity.enums.PaymentStatus;
import com.stoliar.repository.PaymentRepository;
import com.stoliar.security.PaymentSecurity;
import com.stoliar.util.JwtTokenProvider;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private PaymentSecurity paymentSecurity;

    private void setAuthentication(Long userId, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @BeforeEach
    void setUpMocks() {
        Mockito.when(jwtTokenProvider.validateToken(any())).thenReturn(true);
        Mockito.when(jwtTokenProvider.getUsernameFromToken(any())).thenReturn("test-user");
        Mockito.when(jwtTokenProvider.getUserIdFromToken(any())).thenReturn(5L);
        Mockito.when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("USER");

        // Мокируем PaymentSecurity
        Mockito.when(paymentSecurity.getCurrentUserId(any())).thenReturn(5L);
        Mockito.when(paymentSecurity.isAdmin(any())).thenReturn(false);

        // Для checkPaymentAccess:
        Mockito.doAnswer(invocation -> {
            String paymentId = invocation.getArgument(0, String.class);
            if ("6993640ef89c0c02e0ec61f9".equals(paymentId)) {
                throw new SecurityException("Access denied");
            }
            return null;
        }).when(paymentSecurity).checkPaymentAccess(any(), any());
    }

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(Payment.class);
        SecurityContextHolder.clearContext();
        if (wireMockServer != null) {
            wireMockServer.resetAll();
        }
    }

    // === CREATE PAYMENT TESTS ===

    @Test
    void createPayment_AsUser_ReturnsCompleted() throws Exception {
        setAuthentication(1L, "ROLE_USER");

        // Мокируем PaymentSecurity под текущего пользователя
        Mockito.when(paymentSecurity.getCurrentUserId(any())).thenReturn(1L);
        Mockito.when(paymentSecurity.isAdmin(any())).thenReturn(false);

        wireMockServer.stubFor(get(urlMatching("/integers.*"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("42")));

        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .paymentAmount(new BigDecimal("100.50"))
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.userId").value(1L));

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payments.get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    void createPayment_AsAdminWithTargetUserId() throws Exception {
        setAuthentication(1L, "ROLE_ADMIN");

        // Мокируем PaymentSecurity для админа
        Mockito.when(paymentSecurity.getCurrentUserId(any())).thenReturn(1L);
        Mockito.when(paymentSecurity.isAdmin(any())).thenReturn(true);

        wireMockServer.stubFor(get(urlMatching("/integers.*"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("42")));

        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .paymentAmount(new BigDecimal("100.50"))
                .build();

        Long targetUserId = 100L;

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/payments")
                        .param("userId", targetUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(targetUserId));

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getUserId()).isEqualTo(targetUserId);
    }

    // === GET TOTAL SUM TESTS ===

    @Test
    void getTotalSumByUserId_Success() throws Exception {
        setAuthentication(1L, "ROLE_USER");

        createPayment(1L, 1L, new BigDecimal("100.00"),
                LocalDateTime.of(2026, 1, 28, 12, 0));
        createPayment(2L, 1L, new BigDecimal("200.00"),
                LocalDateTime.of(2026, 1, 28, 14, 0));
        createPayment(3L, 1L, new BigDecimal("50.00"),
                LocalDateTime.of(2026, 1, 27, 10, 0));
        createPayment(4L, 2L, new BigDecimal("300.00"),
                LocalDateTime.of(2026, 1, 28, 15, 0));

        String startDate = "2026-01-28T10:00:00";
        String endDate = "2026-01-28T18:00:00";

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/user/{userId}/total", 1L)
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(300.00));
    }

    // === GET PAYMENT BY ID ===

    @Test
    void getPaymentById_OwnPayment() throws Exception {
        setAuthentication(5L, "ROLE_USER");

        String paymentId = new ObjectId().toString();
        createPaymentWithId(paymentId, 1L, 5L, new BigDecimal("50.00"), LocalDateTime.now());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/{id}", paymentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(paymentId))
                .andExpect(jsonPath("$.data.userId").value(5L));
    }

    @Test
    void getPaymentById_AccessDenied_OtherUsersPayment() throws Exception {
        String paymentId = "6993640ef89c0c02e0ec61f9"; // id платежа другого пользователя
        createPaymentWithId(paymentId, 1L, 10L, new BigDecimal("50.00"), LocalDateTime.now());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/{id}", paymentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // === HELPERS ===

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