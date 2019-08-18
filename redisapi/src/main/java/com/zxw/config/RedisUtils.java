package com.zxw.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class RedisUtils {
    public static JedisPool initPool() throws IOException {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("redis-config.properties");
        Properties properties = new Properties();
        properties.load(stream);
        // 初始化Redis连接池配置，即把配置文件参数指定给连接代码
        JedisPoolConfig config = new JedisPoolConfig();
        String active = properties.getProperty("maxActive");
        // 最大连接数
        config.setMaxTotal(Integer.valueOf(properties.getProperty("maxActive")));
        // 最大空闲数
        config.setMaxIdle(Integer.valueOf(properties.getProperty("maxIdle")));
        // 最大等待时间
        config.setMaxWaitMillis(Integer.valueOf(properties.getProperty("maxWait")));
        String[] address = properties.getProperty("ip").split(":");
        JedisPool pool = new JedisPool(config, address[0], Integer.valueOf(address[1]), Integer.valueOf(Integer.valueOf(properties.getProperty("timeout"))));
        return pool;
    }

    public static void main(String[] args) {
        try {
            JedisPool pool = initPool();
            Jedis jedis = pool.getResource();
            jedis.set("bb", "张三");
            jedis.set("aa", "aa");
            String name = jedis.get("bb");
            String aa = jedis.get("aa");
            System.out.println(name);
            System.out.println(aa);
            System.out.println(jedis.mget("aa","bb"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
