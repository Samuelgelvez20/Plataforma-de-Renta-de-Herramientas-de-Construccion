package com.plataforma.renta.infrastructure.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;

import javax.sql.DataSource;
import java.util.Arrays;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.plataforma.renta.infrastructure.persistence.repository")
@ComponentScan(basePackages = "com.plataforma.renta.infrastructure.persistence.adapter")
public class InfrastructureConfig {

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "flyway", initMethod = "migrate")
    @ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
    public static BeanFactoryPostProcessor flywayEntityManagerDependsOnPostProcessor() {
        return beanFactory -> {
            for (String name : beanFactory.getBeanNamesForType(jakarta.persistence.EntityManagerFactory.class, true, false)) {
                var bd = beanFactory.getBeanDefinition(name);
                String[] current = bd.getDependsOn();
                if (current == null || current.length == 0) {
                    bd.setDependsOn("flyway");
                } else if (!Arrays.asList(current).contains("flyway")) {
                    String[] updated = Arrays.copyOf(current, current.length + 1);
                    updated[current.length] = "flyway";
                    bd.setDependsOn(updated);
                }
            }
        };
    }
}
