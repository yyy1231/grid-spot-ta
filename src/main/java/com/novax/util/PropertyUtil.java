package com.novax.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyUtil {
    private static final Properties properties = new Properties();

    static {
        try (InputStream resourceAsStream = PropertyUtil.class.getResourceAsStream("/application_test.properties")) {
            properties.load(resourceAsStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getServerUrl() {
        return properties.getProperty("server.url");
    }

    public static String getServerPort() {
        return properties.getProperty("server.port");
    }

    public static String getAppletServerUrl() {
        return properties.getProperty("server.applet.url");
    }

    public static String getAppletServerPort() {
        return properties.getProperty("server.applet.port");
    }

    public static String getJdbcDriver() {
        return properties.getProperty("jdbc.driver");
    }

    public static String getJdbcUrl() {
        return properties.getProperty("jdbc.url");
    }

    public static String getJdbcUser() {
        return properties.getProperty("jdbc.user");
    }

    public static String getJdbcPassword() {
        return properties.getProperty("jdbc.password");
    }

    public static String getRedisNodes() {
        return properties.getProperty("redis.nodes");
    }

    public static String getRedisHost() {
        return properties.getProperty("redis.host");
    }

    public static String getRedisPort() {
        return properties.getProperty("redis.port");
    }

    public static String getRedisPassword() {
        return properties.getProperty("redis.password");
    }

    public static String getMongoHost() {
        return properties.getProperty("mongo.host");
    }

    public static String getMongoPort() {
        return properties.getProperty("mongo.port");
    }

    public static String getMongoUser() {
        return properties.getProperty("mongo.username");
    }

    public static String getMongoPassword() {
        return properties.getProperty("mongo.password");
    }

    public static String getMongoDataBase() {
        return properties.getProperty("mongo.database");
    }

    public static String getBaseUrl() {
        return properties.getProperty("base_url");
    }

    public static String getIp() {
        return properties.getProperty("ip");
    }

    public static String getPort() {
        return properties.getProperty("port");
    }

    public static String getSpotServerUrl() {
        return properties.getProperty("spot.server.url");
    }

    public static String getSpotServerPort() {
        return properties.getProperty("spot.server.port");
    }
}
