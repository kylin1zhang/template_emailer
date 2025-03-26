package com.citi.custody.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.citi.custody.dao")
public class MongoConfig {
    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);

    @Value("${spring.data.mongodb.uri}")
    private String connectionString;

    @Bean
    public MongoClient mongoClient() {
        try {
            logger.info("正在创建MongoDB客户端连接，连接字符串为: {}", connectionString);
            ConnectionString connString = new ConnectionString(connectionString);
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connString)
                    .build();
            
            MongoClient client = MongoClients.create(settings);
            logger.info("MongoDB客户端连接创建成功");
            
            // 测试连接
            try {
                String dbName = getDatabaseName();
                client.getDatabase(dbName).listCollectionNames().first();
                logger.info("MongoDB连接测试成功，可以访问数据库: {}", dbName);
            } catch (Exception e) {
                logger.error("MongoDB连接测试失败: {}", e.getMessage(), e);
            }
            
            return client;
        } catch (Exception e) {
            logger.error("创建MongoDB客户端连接失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory() {
        try {
            logger.info("创建MongoDatabaseFactory");
            MongoDatabaseFactory factory = new SimpleMongoClientDatabaseFactory(mongoClient(), getDatabaseName());
            logger.info("MongoDatabaseFactory创建成功");
            return factory;
        } catch (Exception e) {
            logger.error("创建MongoDatabaseFactory失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        try {
            logger.info("创建MongoTemplate");
            MongoDatabaseFactory factory = mongoDatabaseFactory();
            
            // Create custom converter to remove _class field
            MongoMappingContext context = new MongoMappingContext();
            context.setAutoIndexCreation(true); // 启用自动创建索引
            
            MappingMongoConverter converter = new MappingMongoConverter(
                    new DefaultDbRefResolver(factory), context);
            converter.setTypeMapper(new DefaultMongoTypeMapper(null)); // Removes _class field
            
            MongoTemplate template = new MongoTemplate(factory, converter);
            logger.info("MongoTemplate创建成功");
            
            // 验证模板
            try {
                template.getCollectionNames();
                logger.info("MongoTemplate验证成功，可以列出集合名称");
            } catch (Exception e) {
                logger.error("MongoTemplate验证失败: {}", e.getMessage(), e);
            }
            
            return template;
        } catch (Exception e) {
            logger.error("创建MongoTemplate失败: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private String getDatabaseName() {
        try {
            ConnectionString connString = new ConnectionString(connectionString);
            String database = connString.getDatabase();
            logger.info("从连接字符串中提取的数据库名称: {}", database);
            return database;
        } catch (Exception e) {
            logger.error("从连接字符串中提取数据库名称失败: {}", e.getMessage(), e);
            throw e;
        }
    }
} 