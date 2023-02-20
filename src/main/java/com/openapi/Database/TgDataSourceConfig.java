package com.openapi.Database;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.alibaba.druid.pool.DruidDataSource;

import javax.sql.DataSource;


@Configuration
@PropertySource("classpath:conf/args.properties")
@MapperScan(basePackages = "com.openapi.Dao", sqlSessionFactoryRef = "huarongdaoSqlSessionFactory")
public class TgDataSourceConfig {
    private final static Logger log = LogManager.getLogger(TgDataSourceConfig.class.getName());
    private String resource = "mybatis-config.xml";
    private String mapper = "classpath:mapping/*.xml";

    @Value("${mysql.driver}")
    private String driverClass;

    @Value("${mysql.url}")
    private String url;

    @Value("${mysql.username}")
    private String user;

    @Value("${mysql.password}")
    private String password;

    @Value("${apikey}")
    private String apikey;

    @Primary
    @Bean(name = "huarongdaoDataSource")
    public DataSource clusterDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(driverClass);
        dataSource.setUrl(url);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setMinIdle(5);
        dataSource.setInitialSize(5);
        return dataSource;
    }

    @Primary
    @Bean(name = "huarongdaoTransactionManager")
    public DataSourceTransactionManager clusterTransactionManager() {
        return new DataSourceTransactionManager(clusterDataSource());
    }

    @Primary
    @Bean(name = "huarongdaoSqlSessionFactory")
    public SqlSessionFactory readDataSource(@Qualifier("huarongdaoDataSource") DataSource secondDataSource) {
        try {
            SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
            sqlSessionFactoryBean.setConfigLocation(new ClassPathResource(resource));
            sqlSessionFactoryBean.setDataSource(secondDataSource);
            sqlSessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                    .getResources(mapper));
            return sqlSessionFactoryBean.getObject();
        } catch (Exception e) {
            log.error("注解mybatis读取数据异常", e);
            return null;
        }
    }

    public String getApikey() {
        return apikey;
    }

    public void setApikey(String apikey) {
        this.apikey = apikey;
    }
}
