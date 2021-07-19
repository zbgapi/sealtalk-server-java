package com.rcloud.server.sealtalk.configuration;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import tk.mybatis.spring.annotation.MapperScan;

import javax.sql.DataSource;

/**
 * Exchange数据库连接配置
 */
@MapperScan(basePackages = "com.rcloud.server.sealtalk.exchange.dao", sqlSessionTemplateRef = Beans.EXCHANGE_SQL_SESSION_TEMPLATE)
public class ExchangeDataSourceConfiguration {

    public static final int DEFAULT_STATEMENT_TIMEOUT = 10000;
    public static final int DEFAULT_FETCH_SIZE = 100;


    @Bean(Beans.EXCHANGE_DATASOURCE_PROPERTIES)
    @ConfigurationProperties("exchange.datasource")
    public DataSourceProperties exchangeDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(Beans.EXCHANGE_DATASOURCE)
    public DataSource exchangeDataSource(@Qualifier(Beans.EXCHANGE_DATASOURCE_PROPERTIES) DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Bean(Beans.EXCHANGE_SQL_SESSION_FACTORY)
    public SqlSessionFactory exchangeSqlSessionFactory(@Qualifier(Beans.EXCHANGE_DATASOURCE) DataSource flashDataSource)
            throws Exception {
        return buildSqlSessionFactory(flashDataSource);
    }

//    @Bean
//    public MapperScannerConfigurer exchangeMapperScannerConfigurer() {
//        MapperScannerConfigurer mapperScanner = new MapperScannerConfigurer();
//        mapperScanner.setBasePackage("com.rcloud.server.sealtalk.exchange.dao");
//        mapperScanner.setSqlSessionFactoryBeanName(Beans.EXCHANGE_SQL_SESSION_FACTORY);
//        return mapperScanner;
//    }

    protected SqlSessionFactory buildSqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setDefaultFetchSize(DEFAULT_FETCH_SIZE);
        configuration.setDefaultStatementTimeout(DEFAULT_STATEMENT_TIMEOUT);
        sessionFactory.setConfiguration(configuration);

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sessionFactory.setMapperLocations(resolver.getResources("classpath:mybatis/exchange/*.xml"));
        sessionFactory.setTypeAliasesPackage("com.rcloud.server.sealtalk.exchange.domain");
        return sessionFactory.getObject();
    }

    @Bean(name = Beans.EXCHANGE_SQL_SESSION_TEMPLATE)
    public SqlSessionTemplate exchangeSqlSessionTemplate(@Qualifier(Beans.EXCHANGE_SQL_SESSION_FACTORY) SqlSessionFactory sqlSessionFactory) throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
