package com.ray.knowledgeflow.config;

import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * 显式隔离 MySQL 与 Neo4j 事务。
 *
 * <p>Neo4j 自动配置会在 JDBC 事务管理器之前注册名为
 * {@code transactionManager} 的 bean，导致默认 {@code @Transactional}
 * 错误地使用 Neo4j 事务。这里将 JDBC 设为默认，图仓库则显式
 * 绑定到独立的 Neo4j 事务管理器。</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
@EnableNeo4jRepositories(
        basePackages = "com.ray.knowledgeflow.module.graph.repository",
        transactionManagerRef = "neo4jTransactionManager")
public class TransactionConfig {

    @Bean(name = {"transactionManager", "jdbcTransactionManager"})
    @Primary
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    @Bean(name = "neo4jTransactionManager")
    public PlatformTransactionManager neo4jTransactionManager(
            Driver driver,
            DatabaseSelectionProvider databaseSelectionProvider) {
        return new Neo4jTransactionManager(driver, databaseSelectionProvider);
    }

    @Bean(name = "neo4jTransactionTemplate")
    public TransactionTemplate neo4jTransactionTemplate(
            @org.springframework.beans.factory.annotation.Qualifier("neo4jTransactionManager")
            PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
