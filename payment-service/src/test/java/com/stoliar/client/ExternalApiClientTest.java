package com.stoliar.client;

import com.stoliar.entity.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class ExternalApiClientTest {

    @Test
    void shouldReturnCompletedWhenFallbackRandomIsEven() {
        // Given
        RestTemplate restTemplate = mock(RestTemplate.class);
        Random random = mock(Random.class);
        when(random.nextInt(100)).thenReturn(1); // 1 + 1 = 2 (четное)

        ExternalApiClient client = new ExternalApiClient(restTemplate, random);
        ReflectionTestUtils.setField(client, "fallbackEnabled", true);

        // When
        PaymentStatus status = client.determinePaymentStatus();

        // Then
        assertEquals(PaymentStatus.COMPLETED, status);
    }

    @Test
    void shouldReturnFailedWhenFallbackRandomIsOdd() {
        // Given
        RestTemplate restTemplate = mock(RestTemplate.class);
        Random random = mock(Random.class);
        when(random.nextInt(100)).thenReturn(0); // 0 + 1 = 1 (нечетное)

        ExternalApiClient client = new ExternalApiClient(restTemplate, random);
        ReflectionTestUtils.setField(client, "fallbackEnabled", true);

        // When
        PaymentStatus status = client.determinePaymentStatus();

        // Then
        assertEquals(PaymentStatus.FAILED, status);
    }

    @Test
    void shouldReturnFailedWhenFallbackDisabled() {
        // Given
        RestTemplate restTemplate = mock(RestTemplate.class);
        Random random = mock(Random.class);

        ExternalApiClient client = new ExternalApiClient(restTemplate, random);
        ReflectionTestUtils.setField(client, "fallbackEnabled", false);

        // When
        PaymentStatus status = client.determinePaymentStatus();

        // Then
        assertEquals(PaymentStatus.FAILED, status);
    }

    @Test
    void shouldReturnCompletedWhenExternalApiReturnsEvenNumber() throws Exception {
        // Given
        RestTemplate restTemplate = mock(RestTemplate.class);
        Random random = mock(Random.class);

        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok("42"));

        ExternalApiClient client = new ExternalApiClient(restTemplate, random);
        ReflectionTestUtils.setField(client, "externalApiUrl", "https://test.com");
        ReflectionTestUtils.setField(client, "fallbackEnabled", true);

        // When
        PaymentStatus status = client.determinePaymentStatus();

        // Then
        assertEquals(PaymentStatus.COMPLETED, status);
    }

    @Test
    void shouldReturnFailedWhenExternalApiReturnsOddNumber() throws Exception {
        // Given
        RestTemplate restTemplate = mock(RestTemplate.class);
        Random random = mock(Random.class);

        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok("13"));

        ExternalApiClient client = new ExternalApiClient(restTemplate, random);
        ReflectionTestUtils.setField(client, "externalApiUrl", "https://test.com");
        ReflectionTestUtils.setField(client, "fallbackEnabled", true);

        // When
        PaymentStatus status = client.determinePaymentStatus();

        // Then
        assertEquals(PaymentStatus.FAILED, status);
    }

    @Test
    void shouldUseFallbackWhenExternalApiFails() throws Exception {
        // Given
        RestTemplate restTemplate = mock(RestTemplate.class);
        Random random = mock(Random.class);

        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("API недоступен"));
        when(random.nextInt(100)).thenReturn(1); // 1 + 1 = 2 (четное)

        ExternalApiClient client = new ExternalApiClient(restTemplate, random);
        ReflectionTestUtils.setField(client, "externalApiUrl", "https://test.com");
        ReflectionTestUtils.setField(client, "fallbackEnabled", true);

        // When
        PaymentStatus status = client.determinePaymentStatus();

        // Then
        assertEquals(PaymentStatus.COMPLETED, status);
    }

    @Test
    void shouldReturnFailedWhenExternalApiReturnsNull() throws Exception {
        // Given
        RestTemplate restTemplate = mock(RestTemplate.class);
        Random random = mock(Random.class);

        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok(null));
        when(random.nextInt(100)).thenReturn(0); // 0 + 1 = 1 (нечетное)

        ExternalApiClient client = new ExternalApiClient(restTemplate, random);
        ReflectionTestUtils.setField(client, "externalApiUrl", "https://test.com");
        ReflectionTestUtils.setField(client, "fallbackEnabled", true);

        // When
        PaymentStatus status = client.determinePaymentStatus();

        // Then
        assertEquals(PaymentStatus.FAILED, status);
    }

    @Test
    void shouldReturnFailedWhenExternalApiReturnsInvalidNumber() throws Exception {
        // Given
        RestTemplate restTemplate = mock(RestTemplate.class);
        Random random = mock(Random.class);

        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok("not-a-number"));
        when(random.nextInt(100)).thenReturn(0); // 0 + 1 = 1 (нечетное)

        ExternalApiClient client = new ExternalApiClient(restTemplate, random);
        ReflectionTestUtils.setField(client, "externalApiUrl", "https://test.com");
        ReflectionTestUtils.setField(client, "fallbackEnabled", true);

        // When
        PaymentStatus status = client.determinePaymentStatus();

        // Then
        assertEquals(PaymentStatus.FAILED, status);
    }
}