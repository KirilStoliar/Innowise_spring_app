package com.stoliar.service.impl;

import com.stoliar.dto.UserCreateDTO;
import com.stoliar.dto.UserDTO;
import com.stoliar.entity.User;
import com.stoliar.exception.DuplicateResourceException;
import com.stoliar.exception.EntityNotFoundException;
import com.stoliar.mapper.UserMapper;
import com.stoliar.repository.UserRepository;
import com.stoliar.service.UserService;
import com.stoliar.specification.UserSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    @CachePut(value = "users", key = "#result.id")
    public UserDTO createUser(UserCreateDTO userCreateDTO) {
        log.info("Creating new user with email: {}", userCreateDTO.getEmail());

        if (userRepository.existsByEmail(userCreateDTO.getEmail())) {
            throw new DuplicateResourceException("User with email " + userCreateDTO.getEmail() + " already exists");
        }

        // Используем нативный запрос с RETURNING
        User createdUser = userRepository.createUser(
                userCreateDTO.getName(),
                userCreateDTO.getSurname(),
                userCreateDTO.getBirthDate(),
                userCreateDTO.getEmail()
        );

        return userMapper.toDTO(createdUser);
    }

    @Override
    @Transactional
    @Cacheable(value = "users", key = "#id")
    public UserDTO getUserById(Long id) {
        log.info("Fetching user by id: {}", id);
        User user = userRepository.findUserById(id);
        if (user == null) {
            throw new EntityNotFoundException("User not found with id: " + id);
        }
        return userMapper.toDTO(user);
    }

    @Override
    @Transactional
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        log.info("Fetching all users with pagination");
        return userRepository.findAll(pageable)
                .map(userMapper::toDTO);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#id")
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        log.info("Updating user with id: {}", id);

        User existingUser = userRepository.findUserById(id);
        if (existingUser == null) {
            throw new EntityNotFoundException("User not found with id: " + id);
        }

        // Проверка уникальности почты
        if (!existingUser.getEmail().equals(userDTO.getEmail()) &&
                userRepository.existsByEmail(userDTO.getEmail())) {
            throw new DuplicateResourceException("Email " + userDTO.getEmail() + " already exists");
        }

        existingUser.setName(userDTO.getName());
        existingUser.setSurname(userDTO.getSurname());
        existingUser.setBirthDate(userDTO.getBirthDate());
        existingUser.setEmail(userDTO.getEmail());

        User updatedUser = userRepository.save(existingUser);
        return userMapper.toDTO(updatedUser);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#id")
    public UserDTO updateUserStatus(Long id, boolean active) {
        log.info("Updating user status: {}", active);

        User existingUser = userRepository.findUserById(id);
        if (existingUser == null) {
            throw new EntityNotFoundException("User not found with id: " + id);
        }

        existingUser.setActive(active);
        User updatedUser = userRepository.save(existingUser);

        return userMapper.toDTO(updatedUser);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users", "paymentCards"}, allEntries = true)
    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);
        User user = userRepository.findUserById(id);
        if (user == null) {
            throw new EntityNotFoundException("User not found with id: " + id);
        }
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public Page<UserDTO> getUsersWithFilters(String firstName, String surname, Pageable pageable) {
        log.info("Fetching users with filters - firstName: {}, surname: {}", firstName, surname);

        Specification<User> spec = UserSpecifications.hasFirstName(firstName)
                .and(UserSpecifications.hasSurname(surname));

        return userRepository.findAll(spec, pageable).map(userMapper::toDTO);
    }

    @Override
    @Transactional
    public Long getUserIdByEmail(String email) {
        log.info("Getting user ID by email: {}", email);

        Long userId = userRepository.findIdByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));

        return userId;
    }
}