package com.neighbourhub.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.bson.types.ObjectId;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    // Mongoose sends ObjectIds as plain hex strings in JSON; do the same.
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer objectIdAsString() {
        return builder -> builder.serializerByType(ObjectId.class, new ToStringSerializer());
    }
}
