package com.citi.custody.controller;

import com.citi.custody.constant.ErrorCodeConstants;
import com.citi.custody.entity.FilterParams;
import com.citi.custody.entity.User;
import com.citi.custody.service.UserService;
import com.citi.custody.util.AssertUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/save")
    public String saveUser(@RequestBody User user) {
        AssertUtils.isTrue(StringUtils.isNotBlank(user.getFirstName()) && StringUtils.isNotBlank(user.getLastName()) &&
                StringUtils.isNotBlank(user.getEmail()),
                ErrorCodeConstants.INVALID_PARAMETER_ERROR, "firstName, lastName, and email can not be empty!");
        return userService.saveUser(user);
    }

    @PostMapping("/usersList")
    public Page<User> getUsersList(@RequestBody FilterParams params) {
        Pageable pageable = PageRequest.of(params.getPage(), params.getSize());
        return userService.getUsersList(params.getName(), pageable);
    }
}
