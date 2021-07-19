package com.rcloud.server.sealtalk.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import tk.mybatis.spring.annotation.MapperScan;

import javax.sql.DataSource;

/**
 * @Author: xiuwei.nie
 * @Date: 2020/7/6
 * @Description:
 * @Copyright (c) 2020, rongcloud.cn All Rights Reserved
 */
@MapperScan(basePackages = "com.rcloud.server.sealtalk.dao", sqlSessionTemplateRef  = Beans.SEALTALK_SQL_SESSION_TEMPLATE)
public class SingleDataSourceConfiguration {

    public static final int DEFAULT_STATEMENT_TIMEOUT = 10000;
    public static final int DEFAULT_FETCH_SIZE = 100;


    @Bean(Beans.SEALTALK_DATASOURCE_PROPERTIES)
    @Primary
    @ConfigurationProperties("sealtalk.datasource")
    public DataSourceProperties sealtalkDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(Beans.SEALTALK_DATASOURCE)
    @Primary
    public DataSource sealtalkDataSource(@Qualifier(Beans.SEALTALK_DATASOURCE_PROPERTIES) DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }



    @Bean(Beans.SEALTALK_SQL_SESSION_FACTORY)
    @Primary
    public SqlSessionFactory sealtalkSqlSessionFactory(@Qualifier(Beans.SEALTALK_DATASOURCE) DataSource flashDataSource)
        throws Exception {
        return buildSqlSessionFactory(flashDataSource);
    }

    @Bean
    @Primary
    public MapperScannerConfigurer sealtalkMapperScannerConfigurer() {
        MapperScannerConfigurer mapperScanner = new MapperScannerConfigurer();
        mapperScanner.setBasePackage("com.rcloud.server.sealtalk.dao");
        mapperScanner.setSqlSessionFactoryBeanName(Beans.SEALTALK_SQL_SESSION_FACTORY);
        return mapperScanner;
    }

    @Bean
    public ObjectMapper jacksonObjectMapper() {
        return new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.ALWAYS);
    }

    protected SqlSessionFactory buildSqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setDefaultFetchSize(DEFAULT_FETCH_SIZE);
        configuration.setDefaultStatementTimeout(DEFAULT_STATEMENT_TIMEOUT);
        sessionFactory.setConfiguration(configuration);

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sessionFactory.setMapperLocations(resolver.getResources("classpath:mybatis/sealtalk/*.xml"));
        sessionFactory.setTypeAliasesPackage("com.rcloud.server.sealtalk.domain");
        return sessionFactory.getObject();
    }


    @Bean(name = Beans.SEALTALK_SQL_SESSION_TEMPLATE)
    public SqlSessionTemplate sealtalkSqlSessionTemplate(@Qualifier(Beans.SEALTALK_SQL_SESSION_FACTORY) SqlSessionFactory sqlSessionFactory) throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
