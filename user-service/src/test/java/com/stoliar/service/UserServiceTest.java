package com.stoliar.service;

import com.stoliar.dto.UserCreateDTO;
import com.stoliar.dto.UserDTO;
import com.stoliar.entity.User;
import com.stoliar.exception.DuplicateResourceException;
import com.stoliar.mapper.UserMapper;
import com.stoliar.repository.UserRepository;
import com.stoliar.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void testCreateUser_ValidData_ShouldReturnUserDTO() {
        // Given
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setName("John");
        createDTO.setSurname("Doe");
        createDTO.setBirthDate(LocalDate.of(1990, 1, 1));
        createDTO.setEmail("john.doe@example.com");

        User user = new User();
        user.setId(1L);
        user.setName("John");
        user.setSurname("Doe");
        user.setEmail("john.doe@example.com");

        UserDTO expectedDTO = new UserDTO();
        expectedDTO.setId(1L);
        expectedDTO.setName("John");
        expectedDTO.setEmail("john.doe@example.com");

        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(userRepository.createUser(
                eq("John"),
                eq("Doe"),
                eq(LocalDate.of(1990, 1, 1)),
                eq("john.doe@example.com")
        )).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(expectedDTO);

        // When
        UserDTO result = userService.createUser(createDTO);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John", result.getName());
        assertEquals("john.doe@example.com", result.getEmail());
    }

    @Test
    void testCreateUser_DuplicateEmail_ShouldThrowException() {
        // Given
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail("duplicate@example.com");

        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        // When & Then
        assertThrows(DuplicateResourceException.class,
                () -> userService.createUser(createDTO));
    }

    @Test
    void testUpdateUser_ValidData_ShouldReturnUpdatedUserDTO() {
        // Given
        Long userId = 1L;
        UserDTO updateDTO = new UserDTO();
        updateDTO.setName("Jane");
        updateDTO.setSurname("Smith");
        updateDTO.setBirthDate(LocalDate.of(1995, 1, 1));
        updateDTO.setEmail("jane.smith@example.com");

        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setName("John");
        existingUser.setSurname("Doe");
        existingUser.setEmail("john.doe@example.com");

        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setName("Jane");
        updatedUser.setSurname("Smith");
        updatedUser.setEmail("jane.smith@example.com");

        UserDTO expectedDTO = new UserDTO();
        expectedDTO.setId(userId);
        expectedDTO.setName("Jane");

        when(userRepository.findUserById(userId)).thenReturn(existingUser);
        when(userRepository.existsByEmail("jane.smith@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toDTO(updatedUser)).thenReturn(expectedDTO);

        // When
        UserDTO result = userService.updateUser(userId, updateDTO);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("Jane", result.getName());
    }

    @Test
    void testUpdateUserStatus_WhenUserExists_ShouldUpdateStatus() {
        // Given
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setActive(true);

        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setActive(false);

        UserDTO expectedDTO = new UserDTO();
        expectedDTO.setId(userId);
        expectedDTO.setActive(false);

        when(userRepository.findUserById(userId)).thenReturn(existingUser);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toDTO(updatedUser)).thenReturn(expectedDTO);

        // When
        UserDTO result = userService.updateUserStatus(userId, false);

        // Then
        assertNotNull(result);
        assertFalse(result.getActive());
    }

    @Test
    void testGetUserById_WhenUserExists_ShouldReturnUserDTO() {
        // Given
        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        UserDTO expectedDTO = new UserDTO();
        expectedDTO.setId(userId);

        when(userRepository.findUserById(userId)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(expectedDTO);

        // When
        UserDTO result = userService.getUserById(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getId());
    }

    @Test
    void testGetAllUsers_ShouldReturnPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        User user = new User();
        user.setId(1L);
        Page<User> userPage = new PageImpl<>(List.of(user));

        UserDTO userDTO = new UserDTO();
        userDTO.setId(1L);

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        // When
        Page<UserDTO> result = userService.getAllUsers(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetUsersWithFilters_ShouldReturnFilteredUsers() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        String firstName = "John";
        String surname = "Doe";

        User user = new User();
        user.setId(1L);
        Page<User> userPage = new PageImpl<>(List.of(user));

        UserDTO userDTO = new UserDTO();
        userDTO.setId(1L);

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        // When
        Page<UserDTO> result = userService.getUsersWithFilters(firstName, surname, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void testDeleteUser_WhenUserExists_ShouldDeleteUser() {
        // Given
        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        when(userRepository.findUserById(userId)).thenReturn(user);

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository).delete(user);
    }

    @Test
    void testUpdateUser_DuplicateEmail_ShouldThrowException() {
        // Given
        Long userId = 1L;
        UserDTO updateDTO = new UserDTO();
        updateDTO.setName("Updated");
        updateDTO.setEmail("duplicate@example.com");

        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("old@example.com");

        when(userRepository.findUserById(userId)).thenReturn(existingUser);
        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        // When & Then
        assertThrows(DuplicateResourceException.class,
                () -> userService.updateUser(userId, updateDTO));
    }

    @Test
    void testGetUsersWithFilters_WithNullParameters_ShouldReturnAllUsers() {
        //Given
        Pageable pageable = PageRequest.of(0, 10);

        User user = new User();
        user.setId(1L);
        Page<User> userPage = new PageImpl<>(List.of(user));
        UserDTO userDTO = new UserDTO();
        userDTO.setId(1L);

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        // When
        Page<UserDTO> result = userService.getUsersWithFilters(null, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}