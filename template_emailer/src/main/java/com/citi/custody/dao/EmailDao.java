package com.citi.custody.dao;

import com.citi.custody.entity.Email;
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
public class EmailDao {
    @Autowired
    private MongoTemplate mongoTemplate;

    public void saveEmail(Email email) {
        mongoTemplate.save(email);
    }

    public Page<Email> findAllByName(String name, Pageable pageable) {
        Query query = new Query();
        if (name != null && !name.isEmpty()) {
            query.addCriteria(Criteria.where("emailName").regex(name, "i"));
        }
        long count = mongoTemplate.count(query, Email.class);
        List<Email> emails = mongoTemplate.find(query.with(pageable), Email.class);
        return new PageImpl<>(emails, pageable, count);
    }

    public Email findById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return mongoTemplate.findOne(query, Email.class);
    }
}
