package com.novax.util;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.Function;

public class MyBatisUtil {
    private static volatile SqlSessionFactory sqlSessionFactory;

    private static void init() {
        if (sqlSessionFactory == null) {
            synchronized (MyBatisUtil.class) {
                if (sqlSessionFactory == null) {
                    try {
                        InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
                        sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
                    } catch (IOException e) {
                        throw new RuntimeException("初始化数据库失败", e);
                    }
                }
            }
        }
    }

    public static SqlSession getSqlSession() {
        if (sqlSessionFactory == null) init();
        return sqlSessionFactory.openSession();
    }

    public static <T, R> R execute(Class<T> mapperClass, Function<T, R> action) {
        try (SqlSession session = getSqlSession()) {
            return action.apply(session.getMapper(mapperClass));
        }
    }

    public static <T> void executeVoid(Class<T> mapperClass, Consumer<T> action) {
        try (SqlSession session = getSqlSession()) {
            action.accept(session.getMapper(mapperClass));
            session.commit();
        }
    }

    public static void main(String[] args) {
        // System.out.println(BigDecimal.valueOf(20).stripTrailingZeros().scale());
    }
}
