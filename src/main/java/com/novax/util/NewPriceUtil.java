package com.novax.util;

import com.alibaba.fastjson.JSONObject;
import lombok.val;
import redis.clients.jedis.JedisCluster;

import java.math.BigDecimal;
import java.util.Date;

public class NewPriceUtil {

    public static BigDecimal newPriceOfSymbol(String symbol) {
        var newPrice = BigDecimal.ZERO;
        val redisValue = RedisUtil.getJedis().hget("spot:new:price", symbol);
        val parsedObj = JSONObject.parseObject(redisValue);
        if (null != parsedObj && null != parsedObj.getBigDecimal("price")) {
            newPrice = parsedObj.getBigDecimal("price");
        }
        return newPrice;
    }


    // 更新最新价
    public static long updateSpotPrice(String key, String symbol, Double value) {
        JedisCluster jedis = RedisUtil.getJedis();
        String redisValue = jedis.hget(key, symbol);
        JSONObject parsedObj = JSONObject.parseObject(redisValue);
        BigDecimal newPrice = parsedObj.getBigDecimal("price");

        if (newPrice.compareTo(BigDecimal.valueOf(value)) != 0) {
            parsedObj.put("price", value);
            parsedObj.put("date", new Date().getTime());
            return jedis.hset(key, "aisi_usdt", parsedObj.toString());
        }
        return 1L;
    }

    public static void main(String[] args) {
        System.out.println(updateSpotPrice("spot:new:price", "aisi_usdt", 1.0));
        // System.out.println(newPriceOfSymbol("btc_usdt"));
    }
}
