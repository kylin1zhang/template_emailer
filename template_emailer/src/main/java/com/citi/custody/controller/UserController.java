package com.citi.custody.controller;

import com.citi.custody.constant.ErrorCodeConstants;
import com.citi.custody.entity.Email;
import com.citi.custody.entity.FilterParams;
import com.citi.custody.entity.User;
import com.citi.custody.service.UserService;
import com.citi.custody.util.AssertUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/save")
    public String saveUser(@RequestBody User user) {
        return userService.saveUser(user);
    }

    @PostMapping("/usersList")
    public Page<User> getUsersList(@RequestBody FilterParams params) {
        Pageable pageable = PageRequest.of(params.getPage(), params.getSize());
        return userService.getUsersList(params.getName(), pageable);
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable String id) {
        return userService.getUserById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        log.info("Received request to delete user, ID: {}", id);

        if (id == null || id.trim().isEmpty()) {
            log.warn("Failed to delete user: ID is empty");
            return ResponseEntity.badRequest().body("User ID cannot be empty");
        }

        try {
            boolean deleted = userService.deleteUser(id);
            if (deleted) {
                log.info("User deleted successfully, ID: {}", id);
                return ResponseEntity.ok().body("User deleted successfully");
            } else {
                log.warn("Failed to delete user, ID: {}", id);
                return ResponseEntity.status(403).body("Failed to delete user, insufficient permissions or user does not exist");
            }
        } catch (Exception e) {
            log.error("Exception occurred while deleting user, ID: {}, error: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body("Error occurred while deleting user: " + e.getMessage());
        }
    }
}