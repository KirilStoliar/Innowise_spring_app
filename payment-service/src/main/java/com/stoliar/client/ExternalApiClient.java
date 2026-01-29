package com.stoliar.client;

import com.stoliar.entity.enums.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Random;

@Slf4j
@Component
public class ExternalApiClient {
    
    private final RestTemplate restTemplate;
    private final Random random;
    private final String format = "?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new";

    @Value("${external.api.url:https://www.random.org/integers}")
    private String externalApiUrl;

    @Value("${payment.service.fallback.enabled:true}")
    private boolean fallbackEnabled;

    public ExternalApiClient(RestTemplate restTemplate, Random random) {
        this.restTemplate = restTemplate;
        this.random = random;
    }
    
    /**
     * Определяет статус платежа через внешний API
     * Если число четное - COMPLETED, если нечетное - FAILED
     */
    public PaymentStatus determinePaymentStatus() {
        try {
            // Вызов внешнего API для получения случайного числа
            String url = externalApiUrl + format;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                int randomNumber = Integer.parseInt(response.getBody().trim());
                log.info("Received random number from external API: {}", randomNumber);
                
                return (randomNumber % 2 == 0) ? PaymentStatus.COMPLETED : PaymentStatus.FAILED;
            }
        } catch (Exception e) {
            log.warn("Failed to call external API: {}. Using fallback.", e.getMessage());
        }
        
        // Fallback: если внешний API недоступен
        return getFallbackStatus();
    }
    
    private PaymentStatus getFallbackStatus() {
        if (fallbackEnabled) {
            // Локальная генерация случайного числа
            int randomNumber = random.nextInt(100) + 1;
            log.info("Using fallback random number: {}", randomNumber);
            
            return (randomNumber % 2 == 0) ? PaymentStatus.COMPLETED : PaymentStatus.FAILED;
        } else {
            // Или возвращаем FAILED по умолчанию
            log.warn("Fallback disabled, returning FAILED status");
            return PaymentStatus.FAILED;
        }
    }
}