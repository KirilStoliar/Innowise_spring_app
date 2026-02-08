package com.stoliar.service;

import com.stoliar.dto.UserCreateDTO;
import com.stoliar.dto.UserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserDTO createUser(UserCreateDTO userCreateDTO);
    UserDTO getUserById(Long id);
    Page<UserDTO> getAllUsers(Pageable pageable);
    UserDTO updateUser(Long id, UserDTO userDTO);
    UserDTO updateUserStatus(Long id, boolean active);
    void deleteUser(Long id);
    Page<UserDTO> getUsersWithFilters(String firstName, String surname, Pageable pageable);
    Long getUserIdByEmail(String email);
}