package com.sinuohao.service;

import com.sinuohao.model.User;
import java.util.List;

public interface UserService {
    User createUser(User user);
    User updateUser(String id, User user);
    void deleteUser(String id);
    User getUserById(String id);
    User getUserByUsername(String username);
    List<User> getAllUsers(int start, int end);
    void updateLastActive(String id);
}
