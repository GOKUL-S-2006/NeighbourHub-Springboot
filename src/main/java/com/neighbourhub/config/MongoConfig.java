package com.neighbourhub.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

@Configuration
@EnableMongoAuditing // fills createdAt / updatedAt, like Mongoose { timestamps: true }
public class MongoConfig {

    // Spring Data normally adds a "_class" field to every document; Mongoose never did.
    @Autowired
    public void removeClassField(MappingMongoConverter converter) {
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
    }
}
