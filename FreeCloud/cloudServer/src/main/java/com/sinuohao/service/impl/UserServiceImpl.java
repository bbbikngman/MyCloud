package com.sinuohao.service.impl;

import com.sinuohao.model.User;
import com.sinuohao.repository.UserRepository;
import com.sinuohao.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public User createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + user.getUsername());
        }
        user.setLastActive(LocalDateTime.now());
        logger.debug("Creating new user: {}", user);
        return userRepository.save(user);
    }

    @Override
    public User updateUser(String id, User user) {
        User existingUser = getUserById(id);
        
        // Update fields
        if (user.getUsername() != null && !user.getUsername().equals(existingUser.getUsername())) {
            if (userRepository.existsByUsername(user.getUsername())) {
                throw new IllegalArgumentException("Username already exists: " + user.getUsername());
            }
            existingUser.setUsername(user.getUsername());
        }
        
        logger.debug("Updating user: {}", existingUser);
        return userRepository.save(existingUser);
    }

    @Override
    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found with id: " + id);
        }
        logger.debug("Deleting user with id: {}", id);
        userRepository.deleteById(id);
    }

    @Override
    public User getUserById(String id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));
    }

    @Override
    public List<User> getAllUsers(int start, int end) {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Invalid range: start must be non-negative and end must be greater than start");
        }
        
        logger.debug("Getting users from {} to {}", start, end);
        return userRepository.findAll()
            .stream()
            .skip(start)
            .limit(end - start)
            .collect(Collectors.toList());
    }

    @Override
    public void updateLastActive(String id) {
        User user = getUserById(id);
        user.setLastActive(LocalDateTime.now());
        logger.debug("Updating last active time for user: {}", user);
        userRepository.save(user);
    }
}
