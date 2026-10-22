package com.footballleague.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

import javax.sql.DataSource;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.stereotype.Component;

/**
 * Her bağlantının şemasını o anki odaya (RoomContext) ayarlar; oda yoksa varsayılan şema.
 * Hibernate, JdbcTemplate ve yedekleme bu DataSource'tan bağlantı aldığı için tablolar adlarıyla
 * (şemasız) sorgulandığında odanın tabloları bulunur. Spring'in oluşturduğu DataSource bean'i bununla sarılır.
 */
public class RoomAwareDataSource extends DelegatingDataSource {

    private volatile String defaultSchema;
    private volatile boolean upperCaseIdentifiers;

    public RoomAwareDataSource(DataSource target) {
        super(target);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return withSchema(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return withSchema(super.getConnection(username, password));
    }

    private Connection withSchema(Connection connection) throws SQLException {
        try {
            if (defaultSchema == null) {
                upperCaseIdentifiers = connection.getMetaData().storesUpperCaseIdentifiers();
                defaultSchema = connection.getSchema();
            }
            // Bağlantı havuza bir odanın şemasıyla dönmüş olabilir; varsayılana da her seferinde açıkça geçilir
            String room = RoomContext.schema();
            connection.setSchema(room == null ? defaultSchema : normalize(room));
            return connection;
        } catch (SQLException | RuntimeException e) {
            connection.close();
            throw e;
        }
    }

    /** Tırnaksız oluşturulan şema adı H2'de büyük, PostgreSQL'de küçük harfle saklanır. */
    private String normalize(String schema) {
        return upperCaseIdentifiers ? schema.toUpperCase(Locale.ROOT) : schema.toLowerCase(Locale.ROOT);
    }

    @Component
    static class Wrapper implements BeanPostProcessor {

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (bean instanceof DataSource dataSource && !(bean instanceof RoomAwareDataSource)) {
                return new RoomAwareDataSource(dataSource);
            }
            return bean;
        }
    }
}
