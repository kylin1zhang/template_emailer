package com.citi.custody.dao;

import com.citi.custody.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserDao {

    @Autowired
    MongoTemplate mongoTemplate;

    public void saveUser(User user) {
        mongoTemplate.save(user);
    }

    public User findUserById(String id) {
        Query query = new Query(Criteria.where("soeId").is(id));
        return this.mongoTemplate.findOne(query, User.class);
    }

    public Page<User> findAllByName(String name, Pageable pageable) {
        Query query = new Query();
        if (name != null && !name.isEmpty()) {
            query.addCriteria(Criteria.where("soeId").regex(name, "i"));
        }
        long count = mongoTemplate.count(query, User.class);
        List<User> users = mongoTemplate.find(query.with(pageable), User.class);
        return new PageImpl<>(users, pageable, count);
    }
}
