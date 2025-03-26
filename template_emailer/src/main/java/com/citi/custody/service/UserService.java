package com.citi.custody.service;

import com.citi.custody.dao.UserDao;
import com.citi.custody.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class UserService {

    @Autowired
    UserDao userDao;

    public String saveUser(User user) {
        user.setSoeId(user.getSoeId().toUpperCase());
        // check if register
        user.setModifiedTime(new Date());
        if (StringUtils.isEmpty(user.getSoeId())) {
            user.setCreateTime(new Date());
        }
        userDao.saveUser(user);
        return user.getSoeId();
    }

    public Page<User> getUsersList(String name, Pageable pageable) {
        return userDao.findAllByName(name, pageable);
    }
}
