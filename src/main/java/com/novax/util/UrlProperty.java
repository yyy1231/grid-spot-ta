package com.novax.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class UrlProperty {
    private static final Properties properties = new Properties();

    static {
        try (InputStream resourceAsStream = PropertyUtil.class.getResourceAsStream("/url.properties")) {
            properties.load(resourceAsStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getStrategyTemplates() {
        return properties.getProperty("strategyTemplates");
    }

    public static String getStrategyCreate() {
        return properties.getProperty("strategy.create");
    }

    public static String getStrategyQuery() {
        return properties.getProperty("strategy.query");
    }

    public static String getStrategyUpdate() {
        return properties.getProperty("strategy.update");
    }

    public static String getStrategyDetail() {
        return properties.getProperty("strategy.detail");
    }

    public static String getStrategyEntrust() {
        return properties.getProperty("strategy.entrust");
    }

    public static String getStrategyStartup() {
        return properties.getProperty("strategy.startup");
    }

    public static String getStrategyTerminate() {
        return properties.getProperty("strategy.terminate");
    }

    public static String getStrategyTransactions() {
        return properties.getProperty("strategy.transactions");
    }

    public static String getStrategyEvents() {
        return properties.getProperty("strategy.events");
    }

    public static String getTransactionsDetail() {
        return properties.getProperty("transactions.detail");
    }

    public static String getAssets() {
        return properties.getProperty("assets");
    }

    public static String getOrderBalance() {
        return properties.getProperty("order.balance");
    }

    public static String getTotalIncome() {
        return properties.getProperty("totalIncome");
    }

    public static String getAssetsList() {
        return properties.getProperty("assets.list");
    }

    public static String getAssetsCurrencyList() {
        return properties.getProperty("assets.currency.list");
    }

    public static String getSpotOrder() {
        return properties.getProperty("spot.order");
    }

    public static String getRepealOrder() {
        return properties.getProperty("order.repeal");
    }

    public static String getRepeaAlllOrders() {
        return properties.getProperty("order.repealAll");
    }
    public static String getRepeaAllGridOrders() {
        return properties.getProperty("grid.order.repealAll");
    }
}
