package com.citi.custody.service;

import com.citi.custody.dao.UserDao;
import com.citi.custody.entity.Email;
import com.citi.custody.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class UserService {

    @Autowired
    UserDao userDao;

    public String saveUser(User user) {
        user.setSoeId(user.getSoeId().toUpperCase());
        // check if register
        user.setModifiedTime(new Date());
        if(StringUtils.isEmpty(user.getSoeId())) {
            user.setCreateTime(new Date());
        }
        userDao.saveUser(user);
        return user.getSoeId();
    }

    public Page<User> getUsersList(String name, Pageable pageable) {
        return userDao.findAllByName(name, pageable);
    }

    public User getUserById(String id) {
        return userDao.findUserById(id);
    }

    public boolean deleteUser(String id) {
        log.info("Deleting user with ID: {}", id);
        try {
            if (StringUtils.isBlank(id)) {
                log.error("Cannot delete user with null or empty ID");
                return false;
            }

            // Check if email exists
            User user = userDao.findUserById(id);
            if (user == null) {
                log.warn("User not found with ID: {}", id);
                return false;
            }

            userDao.deleteUser(id);
            log.info("User deleted successfully, ID: {}", id);
            return true;
        } catch (Exception e) {
            log.error("Error deleting user with ID: {}", id, e);
            return false;
        }
    }
}
