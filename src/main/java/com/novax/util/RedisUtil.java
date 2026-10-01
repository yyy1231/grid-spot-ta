package com.novax.util;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RedisUtil {

    static int connectionTimeout = 2000;
    static int soTimeout = 3000;
    static int maxAttempts = 5;

    private static volatile JedisCluster jedisCluster;

    // public static Jedis getJedis() {
    //     String host = PropertyUtil.getRedisHost();
    //     int port = Integer.parseInt(PropertyUtil.getRedisPort());
    //     Jedis jedis = new Jedis(host, port);
    //     jedis.auth(PropertyUtil.getRedisPassword());
    //     return jedis;
    // }

    public static List<RedisNode> getRedisNodes() {
        List<RedisNode> redisNodeList = new ArrayList<>();
        String redisNodes = PropertyUtil.getRedisNodes();
        String[] nodes = redisNodes.split(",");
        for (String node : nodes) {
            RedisNode redisNode = new RedisNode();
            String[] nodeStr = node.split(":");
            redisNode.setHost(nodeStr[0]);
            redisNode.setPort(Integer.parseInt(nodeStr[1]));
            redisNodeList.add(redisNode);
        }
        return redisNodeList;
    }

    public static JedisCluster getJedis() {
        if (jedisCluster == null) {
            synchronized (RedisUtil.class) {
                if (jedisCluster == null) {
                    jedisCluster = createJedisCluster();
                }
            }
        }
        return jedisCluster;
    }

    private static JedisCluster createJedisCluster() {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(100);
        poolConfig.setMaxIdle(100);
        poolConfig.setMinIdle(20);
        poolConfig.setMaxWaitMillis(3000);

        Set<HostAndPort> clusterNodes = new HashSet<>();
        for (RedisNode node : getRedisNodes()) {
            clusterNodes.add(new HostAndPort(node.getHost(), node.getPort()));
        }
        return new JedisCluster(clusterNodes, connectionTimeout, soTimeout, maxAttempts, poolConfig);
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class RedisNode {
        String host;
        int port;
    }

    public static void main(String[] args) {
        System.out.println(getJedis().hget("spot:new:price", "btc_usdt"));
    }
}
