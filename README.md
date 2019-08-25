---
typora-copy-images-to: redis

---

# Redis

## 1.Redis存储模式

1. list,hash,set,zset属于容器型数据结构
   1. create if not exists:如果不存在就创建一个
   2. drop if no elements:如果没有元素了，就立即删除容器，释放内存
2. 过期时间
   1. 所有数据结构都可以设置过期时间，过期是以对象为单位。例：hash结构过期，整个hash对象过期，而非某个字key过期
   2. 如果设置了过期时间，调用set方法，则过期时间消失

### 1.1 字符串

1. 最简单的数据结构

2. 字符串由多个字节组成，每个字节又由8个bit组成，很多个bit的组合，这边是bitmap(位图)数据结构

3. 字符串内容是二进制安全的，这意味着程序可以把数字，文本，图片，视频等都给这个值

4. 扩容时一次只会多扩1MB的空间，最大长度不超过512MB

5. 可以使用IT:Bookid方式增加键的提示信息

6. 内部结构的实现类似于java的ArrayLsit,采用预分配冗余空间的方式来减少内存的频繁分配

7. 

   ```sql
   set name codehole("ok")
   get name("codehole")
   exists name(integer 1) // 是否存在
   del name(integer 0) // 删除
   get name(nil)
   strlen name // 获取长度,一个汉字两个字节
   ```

8. 批量键值对

   ```sql
   set name1 codehole
   set name2 holycoder
   mget name1 name2 name3("codehole","holycoder",("nil"))
   mset name1 boy name2 girl name3 unknown
   mget name1 name2 name3
   ```

9. 过期和set命令扩展

   ```sql
   // 可以对key设置过期时间，到时间会自动删除，这个功能常用来控制缓存失效时间。
   set name codehole
   get name
   expire name 5 // 5s后过期
   get name(nil)
   setex name 5 codehole // 5s后过期，等价于set+expire
   setnx name codehole // 如果不存在就创建，如果存在则创建不成功
   ```

10. 计数

    ```sql
    // 方位在signed long的最大值和最小值之间,超出了这个范围，Redis会报错
    set age 30
    incr age(integer 31)
    decr age(integer 30)
    incrby // 减指定数操作
    incrbyfloat // 对浮点数做指定数操作
    ```

11. 修改

    ```sql
    append name value(integer 10) // O(1)
    getrange name start end("codehole") // O(N),字符串截取Substr替代
    getset name value("新值") // O(1),获取旧值，设置新值 重置计数功能
    setrange name offset value // O(1),替换指定键字符串的一部分
    ```

    

### 1.2 list列表

1. 若干插入顺序排序的字符串组成，按照链表的插入顺序排序，在读写操作时只能从其两头

2. 相当于java的LinkedList，插入时间复杂度为O(1),索引定位很慢O(n)

3. 常用来做异步队列使用。将需要延后处理的任务结构体序列化成字符串，塞进Redis的列表，另一个线程从这个列表中轮询数据进行处理

   1. |   lpush    |  O(1)  |
      | :--------: | :----: |
      |   lrange   | O(S+N) |
      |   rpush    |  O(1)  |
      |    lpop    |  O(1)  |
      |    rpop    |  O(N)  |
      |    lrem    |  O(N)  |
      |   lindex   |  O(1)  |
      |    llen    |  O(1)  |
      |    lset    |  O(N)  |
      |   ltrim    |  O(N)  |
      |  linsert   |  O(N)  |
      |   lpushx   |  O(1)  |
      | rpoplpush  |  O(1)  |
      |   rpushx   |  O(1)  |
      |   blpop    |  O(1)  |
      |   brpop    |  O(1)  |
      | brpoplpush |  O(1)  |

4. 右边进左边出：队列

   ```sql
   // 先进先出
   rpush books python java golang
   llen books
   lpop books // python
   lpop books // java
   ```

5. 右边进右边出：栈

   ```sql
   // 后进先出
   rpush books python java golang
   rpop books // golang
   ```

6. 其他列表操作命令

   ```sql
   linsert key before|after pivot value // -1:pivot值不存在，key不存在则报错
   lpushx key value // 最左边插入值,0:key不存在，key不是列表则报错。key不存在时不执行:这是与push的区别
   rpoplpush 列表1 列表2 // 将列表1的最右一个值分割到列表2，如果列表2不存在，则自动创建
   blpop key[key...] timeout:指定阻塞的最大秒数
   ```

7. 慢操作

   1. lindex相当于java链表的get(int index)方法，它需要对链表进行遍历，性能随着参数index增大而变差
   2. ltrim(start_index,end_index)，在这个区间内的值要保留，可以通过ltrim来实现一个定长的链表
   3. index可以为负数，index=-1表示倒数第一个元素，同理index=-2表示倒数第二个元素

   ```sql
   lindex books 1 // O(n)，慎用,下标从0开始
   lrange books 0 -1 // 获取所有元素，O(n)慎用
   ltrim books 1 -1 // O(n) 慎用
   ltrim books 1 0 // 清空整个列表，因为区间长度为负
   
   ```

8. 快速列表

   1. Redis底层存储不是一个简单的linklist，而是快速链表(quicklist)
   2. ![1565510256604](D:\code\IDEA CODE\Redis-Learn\redis\1565510256604.png)

### 1.3 hash(字典)

1. 相当于java中的HashMap(链表+数组)

2. 无序字典，字典值只能是字符串

3. 可以用来存储用户信息，可以对用户结构中的每个字段单独存储

4. hash结构的存储消耗要高于单个字符串

5. |     hlen     |   O(1)    |
   | :----------: | :-------: |
   |     hset     |   O(1)    |
   |     hget     |   O(N)    |
   |    hmset     |   O(N)    |
   |    hmget     |   O(N)    |
   |   hgetall    |   O(N)    |
   |    hexist    |   O(1)    |
   |     hdel     |   O(N)    |
   |     hlen     |   O(1)    |
   |    hsetnx    |   O(1)    |
   |   hstrlen    |   O(1)    |
   |    hvals     |   O(N)    |
   |   hincrby    |   O(1)    |
   | hincrbyfloat |   O(1)    |
   |    hkeys     |   O(N)    |
   |    hscan     | O(1)-O(N) |

6. ```sql
   hset books java "think in java" // 空格需要加双引号
   hset books python python
   hgetall books
   hget books java
   hset books golang "learning go programming" // 更新操作，返回0
   hmset books java "efftice java" python "learning python" // 批量操作
   hset user-laoqian age 29
   hincrby user-laoqian age 1(integer 30)
   
   ```

### 1.4 set集合

1. 相当于java中的HashSet(基于HashMap实现)

2. 无序的、唯一的。内部实现相当于一个特殊的字典，字典中所有的value都是一个值null

3. 当集合中最后一个元素被移除之后，数据结构被自动删除，内存被回收

4. set结构可以用来存储在某活动中中奖的用户ID，因为有去重功能

5. |    sadd     |   O(N)    |
   | :---------: | :-------: |
   |  smembers   |   O(N)    |
   |    srem     |   O(N)    |
   |    scard    |   O(1)    |
   | srandmember | O(N)-O(N) |
   |    smove    |   O(1)    |
   |    spop     |   O(1)    |
   |  sismember  |   O(1)    |
   |    sscan    | O(1)-O(N) |
   |   sunion    |   O(N)    |
   | sunionstore |   O(N)    |
   |   sinter    |  O(N*m)   |
   | sinterstore |  O(N*m)   |
   |    sdiff    |   O(N)    |
   | sdiffstore  |   O(N)    |

6. 普通操作

   ```sql
   sadd book java
   sadd book java // 重复(integer)0
   smembers book
   sismember book java(integer 1)
   scard book // 获取长度
   spop book // 弹出一个
   spop book 2// 弹出2个
   srandmember sList count // 随机取值
   smove sList slist2 python // 将值从第一个集合移动到第二个集合，如果集合不存在，则自动创建
   sscan sList 0 match j* // "0":下次迭代游标值，0值代表迭代结束
   
   ```

7. 集合并、交、差运算操作命令

   ```sql
   sunion // 只返回结果
   sunionstore 新集合 集合1 集合2// 返回新列表，不指定新集合，会将集合1变成交集
   sidff // 取差集
   
   ```

### 1.5 zset(有序列表)

1. 类似于java的SortedSet和HashMap的结合体

2. 一方面是一个set，保证value的唯一性。一方面可以给每个value赋予一个score，代表这个value的排序权重。

3. 内部实现"跳跃列表"数据结构

4. zset可以用来存储粉丝列表，value值是粉丝的用户ID，score是关注事件。对粉丝列表按关注事件进行排序

5. 存储学生的成绩，value值是学生的ID，score是他的考试成绩。对成绩按分数进行排序就可以得到他的名次

6. |       zadd       |             有序集合增加键值对             |             |
   | :--------------: | :----------------------------------------: | ----------- |
   |      zrange      |      返回指定范围有序集合的键或键值对      | O(log(N))   |
   |      zcount      |        返回有序集合指定值范围的个数        | O(log(N)+M) |
   |       zrem       |           删除有序集合指定键值对           | O(log(N))   |
   |      zcard       |           返回有序集合键值对个数           | O(M*log(N)) |
   |     zincrby      |      对有序集合指定键的值进行增量操作      | O(1)        |
   |    zlexcount     |        返回有序集合指定键范围的个数        | O(log(N))   |
   |      zscore      |         返回有序集合指定键对应的值         | O(N)        |
   |      zrank       |         返回有序集合指定值排名位号         | O(1)        |
   |   zunionstore    |        带存储功能的多有序集合并运算        | O(log(N))   |
   |   zinterstore    |        带存储功能的多有序集合交运算        |             |
   |  zremrangebylex  |     在同值情况下，删除指定范围的键值对     |             |
   |  zrangebyscore   |     返回指定范围有序集的键或键值对列表     |             |
   | zremrangebyscore |         删除指定值大小范围的键值对         |             |
   | sremrangebyrank  |         删除指定值下标范围的键值对         |             |
   |    zrevrange     |   返回指定值下标范围的固定排序键值对列表   |             |
   |  zrevrangebylex  |  在同值情况下，返回指定键范围倒排序键列表  |             |
   | zrerangebyscore  | 返回指定值大小范围的固定排序键或键值对列表 |             |
   |     srevrank     |      返回指定键在有序集合里的排名位数      |             |
   |      zscan       |    增量迭代式返回有序集合中的键值对列表    |             |

   

7. ```sql
   zadd book 9.0 "think in java"
   zadd book 8.9 "java concurrency"
   zadd book 8.6 "python"
   zrange book 0 -1 // 正序列出
   zrevrange book 0 -1 // 逆序列出
   zcard book
   zscore book "python" // 8.900000000004 内部使用double类型进行存储，所以存在小数点精度问题
   zrank book "java concurrency" // 排名
   zrangebyscore book 0 8.9 // 0-8.9排名之间的值
   zrangebyscore book -inf 8.9 withscores // 根据分值区间(负无穷，8.9)遍历zset，同时返回分值。inf代表infinite,无穷大的啥意思
   zrem book "python" // 删除值
   
   ```

8. 内部使用跳跃列表数据结构来实现

### 1.6 位图bitmap

1. bitcount:统计字符串指定位置的值为"1"Bit的位数

   ```
   f的二进制值是01100110,统计为4
   一个英文字符对应一个字节(Byte)8个比特为(Bit)
   
   ```

2. setbit:设置或者清空指定位置的Bit值

   ```
   setbit key offset value
   一个字节的偏移量为0从左到右数为0-7
   s:01110011,左边第一位偏移量为0，第二位为1，一次类推，最右位为7。
   value为比特位"1"或"0"
   当键不存在时，建立一个新的字符串值，并保证offset处有bit值。
   offset值的设置范围0-231
   
   ```

   

3. getbit:获取指定位置的bit值

4. bitop:对一个或多个二进制位的字符串进行BIT运算操作

5. bitpos:获取字符串里第一个被设置为1bit或0bit的位置

6. bitfield:对指定字符串数据进行位数组寻址、位值自增自减等操作

### 1.7 HyperLogLog

1. 是一种概率数据结构，用于统计唯一事物(访问人数)

2. | pfadd   | 将指定元素添加到HyperLogLog            | O(1)      |
   | ------- | -------------------------------------- | --------- |
   | pfcount | 返回指定Key的近似基数                  | O(1)-O(N) |
   | pfmerge | 将多个HyperLogLog合并为一个HyperLogLog | O(N)      |

3. ```
   pfadd code user1
   pfadd code user2
   pfcount code
   
   ```

   

## 2.其他

### 2.1发布订阅

1. publish channel message

2. subscribe channel

   ```
   1)"subscribe"
   2)"channel1"
   3)(integer) 1
   1)"message"
   2)"channel1"
   3)"xxx" // 接收到发布端发布的消息
   
   ```

3. psubscribe

   ```
   psubscribe pattern [parrern ...]
   ?,例:w?,只能两个字符wo
   *,例:w*,不限个数,want
   [],例:w[2e]are,返回wearem，第一个字母必须为w,第二个字母为[]里的任意一个
   
   ```

4. punsubscribe

   ```
   punsubscribe pattern [parrern ...]
   用法同上
   取消指定的订阅模式
   默认取消所有的订阅模式
   
   ```

5. unsubscribe

   ```
   unsubscribe [channel [channel...]]
   参数为需要退订的频道，默认取消所有
   返回值：当去取消某指定的频道后，将返回取消消息列表
   
   ```

6. pubsub

   ```
   pubsub subcommand [argument [argument...]]
   subcommand为子命令，包括channels,numsub,numpat,argument为子命令对应的参数
   pubsub channels [pattern]：列出当前活动的频道，默认列出所有频道
   pubsub numsub [channel,...,channel_n]:返回指定频道的订阅者数量，格式：频道名、计数、频道名 计数，默认返回空列表
   pubsub numpat:返回指定模式的订阅数量，这里是所有客户端订阅的模式总数
   
   ```

### 2.2.连接命令

![1565530122942](D:\code\IDEA CODE\Redis-Learn\redis\1565530122942.png)

![1565530141777](D:\code\IDEA CODE\Redis-Learn\redis\1565530141777.png)

![1565530151039](D:\code\IDEA CODE\Redis-Learn\redis\1565530151039.png)

![1565530158133](D:\code\IDEA CODE\Redis-Learn\redis\1565530158133.png)

### 2.3Server操作命令

![1566098620071](D:\code\IDEA CODE\Redis-Learn\redis\1566098620071.png)

![1566099083533](D:\code\IDEA CODE\Redis-Learn\redis\1566099083533.png)

![1566098644853](D:\code\IDEA CODE\Redis-Learn\redis\1566098644853.png)![1566098654730](D:\code\IDEA CODE\Redis-Learn\redis\1566098654730.png)

![1566098665807](D:\code\IDEA CODE\Redis-Learn\redis\1566098665807.png)

### 2.4 操作磁盘命令

![1566099068243](D:\code\IDEA CODE\Redis-Learn\redis\1566099068243.png)

### 2.5 脚本命令

![1566099113163](D:\code\IDEA CODE\Redis-Learn\redis\1566099113163.png)

### 2.6 键命令

![1566099151939](D:\code\IDEA CODE\Redis-Learn\redis\1566099151939.png)

### 2.7 地理空间操作命令

![1566100555034](D:\code\IDEA CODE\Redis-Learn\redis\1566100555034.png)

### 2.8 事务命令

![1566100583218](D:\code\IDEA CODE\Redis-Learn\redis\1566100583218.png)

```
multi // 开启事务
set count 10
decr count
decr count
exec // 提交事务

```

### 2.9 集群命令

![1566102570483](D:\code\IDEA CODE\Redis-Learn\redis\1566102570483.png)

![1566102576171](D:\code\IDEA CODE\Redis-Learn\redis\1566102576171.png)

## 3.Redis配置及参数

### 3.1 Config配置文件

1. daemonize yes:守护进程，3.2版本之后默认为yes
2. pidfile /var/run/redis/pod
3. 
4. ![1566102875041](D:\code\IDEA CODE\Redis-Learn\redis\1566102875041.png)

## 4.Java API

1. ![1566104294618](D:\code\IDEA CODE\Redis-Learn\redis\1566104294618.png)
2. redis-config.properties

```properties
ip=127.0.0.1:6379
maxActive=1000 // 单个应用连接池的最大数，默认为8。
maxIdle=100 // 连接池去连接时最大等待时间
maxWait=10000 // 最大等待时间
testOnBorrow=false // 设置在每一次取对象时测试ping
timeout=2000 // 设置redis connect request response timeout
cluster.ip=127.0.0.1:6379 // 集群连接地址，可以用分割符号连接提供多节点地址

```



## 2.分布式锁

1. 本质上要实现的目标就是在Redis里面占一个坑，当别的进程也要来占坑时，发现已有，则放弃或稍后再试。
2. 占坑一般使用setnx(set if not exists)指令，只允许被一个客户端占坑，先来先占，用完了，在调用del指令释放

## 3.管道

### 3.1 管道技术原理

1. 先批量发送请求，而不是一条一条地返回执行命令，等服务器端接收所有命令后，在服务器端一起执行，最后把执行结果一次性发送回客户端。这样可以减少命令的返回次数，并减少阻塞时间。

2. ```java
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
   //
   非管道方式用时：693毫秒管道方式用时：80毫秒
   
   ```

3. Redis在使用管道技术的情况下，会占用服务器端内存资源，所以一般建议一次管道最大发送命令限制在10000条以内

## 4 分布式集群

1. 启动六个节点
2. 配置ruby

## 5. Lua脚本应用

1. 减少网络开销:把部分特殊代码直接放到服务器端执行，则可以解决因交互而产生的额外的网络开销问题
2. 原子性操作：Lua脚本在服务器端执行时，将采用排它性行为，也就是脚本代码执行时，其他命令或脚本无法在同一个服务器端执行
3. 服务端快速代码替换：对于一些经常需要变化业务规则或算法的代码，可以考虑放到服务器端交给Lua脚本，因为Lua脚本第一次执行后，将一直保存在服务器端的脚本缓存中

## 6. Redis实战

### 6.1 广告访问

1. 建立数据集

### 6.2 商品推荐

### 6.3 购物车

### 6.4 记录浏览器行为

### 6.5 替代Session

### 6.6 分页缓存

## 7.电商大数据

### 7.1 速度问题

1. 纵向：单服务器内部挖掘潜力，优化数据库操作技术细节，如以优化数据库索引，提高查询速度等方面
2. 横向的：分布式多服务器，并进读写分离操作

### 7.2 MongoDB优化

### 7.3 Redis操作速度优化

#### 7.3.1 读写分离

1. 主节点设置

   1. 主节点默认是读写操作，要变为只写操作，需要在该节点的配置文件里设置如下操作

      ```
      min-slaves-to-write 1 // 在保证所有从节点连接的情况下，主节点接收写操作，默认值为0
      min-slaves-max-lag 10 // 从节点延迟时间，默认设置10秒
      
      ```

   2. 上面两个参数的含义：在一个从节点连接并且延迟时间大于10秒的情况，主节点不再接收外部写请求，等待从节点数据主从同步

   3. ![1566724455762](D:\code\IDEA CODE\Redis-Learn\redis\1566724455762.png)

2. 从节点设置

   1. 从节点默认提供只读操作，并在配置文件开启持久化参数
   2. ![1566724582881](D:\code\IDEA CODE\Redis-Learn\redis\1566724582881.png)

7.3.2 内存配置优化

1. 压缩存储
   1. ZipList
      1. 仅适用于限制范围(限制存储数据量和数据大小)的数据进行操作
      2. 仅适用于列表、散列、有序集合和整数值集合

