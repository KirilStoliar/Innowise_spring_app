package com.stoliar.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.stoliar.config.RestTemplateConfig;
import com.stoliar.dto.user.UserInfoDto;
import com.stoliar.util.ServiceTokenProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = {
                UserServiceClient.class,
                RestTemplateConfig.class
        },
        properties = {
                "user.service.url=http://localhost:9561",
                "service.auth.token=test-token",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
        }
)
@EnableAutoConfiguration
@ActiveProfiles("test")
class UserServiceClientWireMockTest {

    static WireMockServer wireMockServer;

    @Autowired
    private UserServiceClient userServiceClient;

    @MockBean
    private ServiceTokenProvider serviceTokenProvider;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(9561);
        wireMockServer.start();
        WireMock.configureFor("localhost", 9561);
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @Test
    void getUserById_existingUser_shouldReturnUserInfo() {
        // Arrange
        stubFor(get(urlEqualTo("/api/v1/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                {
                  "data": {
                    "id": 1,
                    "email": "test@example.com",
                    "name": "Test",
                    "surname": "User",
                    "active": true
                  }
                }
            """)));

        // Act
        UserInfoDto result = userServiceClient.getUserById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(-1L, result.getId());
    }

    @Test
    void getUserById_userNotFound_shouldReturnFallbackUser() {
        // Arrange
        stubFor(get(urlEqualTo("/api/v1/users/99"))
                .willReturn(aResponse().withStatus(404)));

        // Act
        UserInfoDto result = userServiceClient.getUserById(99L);

        // Assert
        assertNotNull(result);
        assertEquals(-1L, result.getId());
        assertEquals("service@unavailable.com", result.getEmail());
        assertFalse(result.getActive());
    }

    @Test
    void getUserById_userServiceDown_shouldReturnFallbackUser() {
        // Arrange
        stubFor(get(urlEqualTo("/api/v1/users/1"))
                .willReturn(aResponse().withStatus(500)));

        // Act
        UserInfoDto result = userServiceClient.getUserById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(-1L, result.getId());
        assertEquals("service@unavailable.com", result.getEmail());
        assertFalse(result.getActive());
    }
}
