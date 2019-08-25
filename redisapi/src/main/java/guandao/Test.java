package guandao;

import com.zxw.config.RedisUtils;
import org.junit.After;
import org.junit.Before;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.io.IOException;

public class Test {
    private Jedis jedis;

    @Before
    public void initJedis() throws IOException {
        jedis = RedisUtils.initPool().getResource();
    }

    @org.junit.Test(timeout = 1000)
    public void PilecommendTest() {
        // 清除指定服务器上的0号数据库
        jedis.flushDB();
        long t1 = System.currentTimeMillis();
        noPipeline(jedis);
        long t2 = System.currentTimeMillis();
        System.out.printf("非管道方式用时：%d毫秒", t2 - t1);
        jedis.flushDB();
        t1 = System.currentTimeMillis();
        usePipeline(jedis);
        t2 = System.currentTimeMillis();
        System.out.printf("管道方式用时：%d毫秒", t2 - t1);
    }

    private static void usePipeline(Jedis jedis) {
        Pipeline p1 = jedis.pipelined();
        for (int i = 0; i < 10000; i++) {
            p1.sadd("Sadd",String.valueOf(i));
        }
        p1.sync();
    }

    private static void noPipeline(Jedis jedis) {
        for (int i = 0; i < 10000; i++) {
            jedis.sadd("SetAdd",String.valueOf(i));
        }
    }

    @After
    public void closeJedis() {
        jedis.close();
    }
}
