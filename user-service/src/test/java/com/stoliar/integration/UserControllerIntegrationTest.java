package com.stoliar.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoliar.dto.UserCreateDTO;
import com.stoliar.entity.User;
import com.stoliar.repository.PaymentCardRepository;
import com.stoliar.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@Transactional
@Import(TestIntegrationSecurityConfig.class)
class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Given - очистка базы и подготовка тестовых данных
        paymentCardRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setName("Integration");
        testUser.setSurname("Test");
        testUser.setBirthDate(LocalDate.of(1990, 1, 1));
        testUser.setEmail("integration.test@example.com");
        testUser.setActive(true);
        testUser = userRepository.save(testUser);
    }

    @Test
    void createUser_ValidData_ShouldReturnCreated() throws Exception {
        // Given
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setName("John");
        createDTO.setSurname("Doe");
        createDTO.setBirthDate(LocalDate.of(1990, 1, 1));
        createDTO.setEmail("john.doe@example.com");

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("John")))
                .andExpect(jsonPath("$.data.email", is("john.doe@example.com")));
    }

    @Test
    void getUserById_WhenUserExists_ShouldReturnUser() throws Exception {
        // Given - testUser уже создан в setUp

        // When & Then
        mockMvc.perform(get("/api/v1/users/{id}", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(testUser.getId().intValue())))
                .andExpect(jsonPath("$.data.name", is("Integration")));
    }

    @Test
    void getUserById_WhenUserNotExists_ShouldReturnNotFound() throws Exception {
        // Given - несуществующий userId
        Long nonExistentUserId = 999L;

        // When & Then
        mockMvc.perform(get("/api/v1/users/{id}", nonExistentUserId))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_ValidData_ShouldReturnUpdatedUser() throws Exception {
        // Given
        String updatedJson = """
            {
                "name": "Updated",
                "surname": "User",
                "birthDate": "1990-01-01",
                "email": "updated@example.com"
            }
            """;

        // When & Then
        mockMvc.perform(put("/api/v1/users/{id}", testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Updated")));
    }

    @Test
    void getAllUsers_ShouldReturnPaginatedUsers() throws Exception {
        // Given - testUser уже создан в setUp

        // When & Then
        mockMvc.perform(get("/api/v1/users")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void createUser_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Given
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setName("");
        createDTO.setSurname("");
        createDTO.setBirthDate(LocalDate.now().plusDays(1)); // будущая дата рождения
        createDTO.setEmail("invalid-email"); // невалидный email

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_WithDuplicateEmail_ShouldReturnConflict() throws Exception {
        // Given - дублирующий email существующего пользователя
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setName("Duplicate");
        createDTO.setSurname("User");
        createDTO.setBirthDate(LocalDate.of(1990, 1, 1));
        createDTO.setEmail("integration.test@example.com"); // дублирующий email

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateUserStatus_ShouldUpdateStatus() throws Exception {
        // Given - testUser уже создан в setUp

        // When & Then
        mockMvc.perform(patch("/api/v1/users/{id}/status", testUser.getId())
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.active", is(false))) // Проверяем, что вернулся объект с обновленным статусом
                .andExpect(jsonPath("$.message", containsString("deactivated")));
    }

    @Test
    void getUsersWithFilters_ShouldReturnFilteredUsers() throws Exception {
        // Given - testUser уже создан в setUp

        // When & Then
        mockMvc.perform(get("/api/v1/users/filter")
                        .param("firstName", "Integration")
                        .param("surname", "Test")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)));
    }

    @Test
    void deleteUser_ShouldDeleteUser() throws Exception {
        // Given - testUser уже создан в setUp

        // When & Then
        mockMvc.perform(delete("/api/v1/users/{id}", testUser.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void getUserByEmail_WhenUserExists_ShouldReturnUserId() throws Exception {
        // Given - testUser уже создан в setUp

        // When & Then
        mockMvc.perform(get("/api/v1/users/email/{email}", testUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(testUser.getId().intValue())));
    }
}