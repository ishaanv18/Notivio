package com.notivio.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Global Jackson configuration.
 *
 * KEY FIX: Registers Hibernate6Module which prevents LazyInitializationException
 * when Jackson tries to serialize uninitialized Hibernate lazy proxy objects.
 * Instead of throwing, it serializes them as null (safe default).
 *
 * This eliminates the need to add @JsonIgnore to every @ManyToOne(LAZY) field.
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Handle Java 8+ date/time types (ZonedDateTime, LocalDate, etc.)
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Handle Hibernate lazy proxies — serialize as null if not loaded
        // instead of throwing LazyInitializationException
        Hibernate6Module hibernate6Module = new Hibernate6Module();
        hibernate6Module.configure(
                Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true);
        mapper.registerModule(hibernate6Module);

        return mapper;
    }
}
