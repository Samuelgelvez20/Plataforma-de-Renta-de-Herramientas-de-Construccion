package com.plataforma.renta.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.plataforma.renta.infrastructure.persistence.repository")
@ComponentScan(basePackages = "com.plataforma.renta.infrastructure.persistence.adapter")
public class InfrastructureConfig {

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}