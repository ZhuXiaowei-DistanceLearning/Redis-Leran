package guandao;

import com.zxw.config.RedisUtils;
import com.zxw.pojo.AdContent;
import com.zxw.pojo.Advertisement;
import com.zxw.pojo.Goods;
import com.zxw.pojo.Template;
import com.zxw.util.JsonUtil;
import com.zxw.util.TranscoderUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA.TIMEOUT;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.util.SafeEncoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GoodsTest {
    private Jedis jedis;

    @Before
    public void initJedis() throws IOException {
        jedis = RedisUtils.initPool().getResource();
    }

    @Test
    public void queryGoodsRecommendTest() {
        System.out.println(jedis.lrange("goods-recommend", 0, 10));
    }

    @Test(timeout = 1000)
    public void saveGoodsRecommendTest() {
        Pipeline pipeline = jedis.pipelined();
        pipeline.lpush("goods-recommend", this.initGoods());
        // 列表过期时间,10分钟
        pipeline.expire("goods-recommend", 10 * 60);
        // 提交本次操作
        System.out.println(pipeline.syncAndReturnAll());
    }

    @Test(timeout = 1000)
    public void saveAdTest() {
        Pipeline pipeline = jedis.pipelined();
        // SafeEncoder redis提供的解码器
        //
        pipeline.setex(SafeEncoder.encode("test"), 10 * 60, TranscoderUtils.encodeObject(this.initGoods()));
        System.out.println();
    }

    private String[] initGoods() {
        Goods goods = new Goods();
        goods.setAdInfo("<html></html>");
        goods.setGoodsInfo("商品名称：华硕Fx53VD商品编号：4380878");
        goods.setId(438087);
        goods.setSpecificationsInfo("主体系列飞行堡垒型号FX53VD颜色红黑平台");
        String[] goodsArray = new String[10];
        for (int i = 0; i < 10; i++) {
            goodsArray[i] = JsonUtil.toJson(goods);
        }
        return goodsArray;
    }

    private Advertisement initAdvertisement() {
        Template template = new Template();
        template.setId(20);
        template.setName("轮播模板");
        template.setScript("alert('轮播')");
        AdContent adContent = new AdContent();
        adContent.setId(1);
        adContent.setName("新年图书忒大促");
        adContent.setSequence(1);
        adContent.setUrl("https://books.Atest.com/");

        AdContent adContent2 = new AdContent();
        adContent2.setId(2);
        adContent2.setName("手机专场");
        adContent2.setSequence(2);
        adContent2.setUrl("https://books.Atest.com/");

        List<AdContent> adContents = new ArrayList<>();
        adContents.add(adContent);
        adContents.add(adContent2);
        Advertisement advertisement = new Advertisement();
        advertisement.setId(10001);
        advertisement.setPositionCode("home-01");
        advertisement.setTid(template.getId());
        advertisement.setAdContents(adContents);
        return advertisement;
    }

    private static void usePipeline(Jedis jedis) {
        Pipeline p1 = jedis.pipelined();
        for (int i = 0; i < 10000; i++) {
            p1.sadd("Sadd", String.valueOf(i));
        }
        p1.sync();
    }

    private static void noPipeline(Jedis jedis) {
        for (int i = 0; i < 10000; i++) {
            jedis.sadd("SetAdd", String.valueOf(i));
        }
    }

    @After
    public void closeJedis() {
        jedis.close();
    }
}
