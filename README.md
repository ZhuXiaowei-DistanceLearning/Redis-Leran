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
   2. ![1565510256604](D:\code\IDEA CODE\Redis-Learn\redis\1565510256604-1567248080158.png)

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

### 1.8 布隆过滤器(Bloom Filter)

1. bf.add 

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

![1565530122942](C:\Users\zxw\Desktop\个人项目笔记\redis\1565530122942.png)

![1565530141777](C:\Users\zxw\Desktop\个人项目笔记\redis\1565530141777.png)

![1565530151039](D:\code\IDEA CODE\Redis-Learn\redis\1565530151039-1567248080160.png)

![1565530158133](D:\code\IDEA CODE\Redis-Learn\redis\1565530158133-1567248080160.png)

### 2.3Server操作命令

![1566098620071](C:\Users\zxw\Desktop\个人项目笔记\redis\1566098620071.png)

![1566099083533](C:\Users\zxw\Desktop\个人项目笔记\redis\1566099083533.png)

![1566098644853](D:\code\IDEA CODE\Redis-Learn\redis\1566098644853-1567248080160.png)![1566098654730](C:\Users\zxw\Desktop\个人项目笔记\redis\1566098654730.png)

![1566098665807](D:\code\IDEA CODE\Redis-Learn\redis\1566098665807-1567248080160.png)

### 2.4 操作磁盘命令

![1566099068243](D:\code\IDEA CODE\Redis-Learn\redis\1566099068243-1567248080160.png)

### 2.5 脚本命令

![1566099113163](D:\code\IDEA CODE\Redis-Learn\redis\1566099113163-1567248080160.png)

### 2.6 键命令

![1566099151939](C:\Users\zxw\Desktop\个人项目笔记\redis\1566099151939.png)

### 2.7 地理空间操作命令

![1566100555034](D:\code\IDEA CODE\Redis-Learn\redis\1566100555034-1567248080160.png)

### 2.8 事务命令

![1566100583218](D:\code\IDEA CODE\Redis-Learn\redis\1566100583218-1567248080161.png)

```
multi // 开启事务
set count 10
decr count
decr count
exec // 提交事务

```

### 2.9 集群命令

1. 集群总共有16383个槽，需要分配完成才可以使集群进行在线状态

2. ```
   redis-trib.rb create --replicas 1 127.0.0.1:6481 127.0.0.1:6482 127.0.0.1:6483 127.0.0.1:6484 127.0.0.1:6485 127.0.0.1:6486
   
   # replicas 指定
   
   ```

3. ruby redis-trib.rb check 127.0.0.1:8000 检查集群是否成功

4. 通信流程

   1. 在分布式存储中需要提供维护节点元数据信息的机制，所谓元数据是指：节点负责哪些数据，是否出现故障等状态信息。常见的元数据维护方式分为：集中式和P2P方式。Redis集群采用P2P的Gossip（流言）协议，Gossip协议工作原理就是节点彼此不断通信交换信息，一段时间后所有的节点都会知道集群完整的信息，这种方式类似流言传播
   2. 集群中的每个节点都会单独开辟一个TCP通道，用于节点之间彼此通信，通信端口号在基础端口上加10000。
   3. 每个节点在固定周期内通过特定规则选择几个节点发送ping消息。
   4. 接收到ping消息的节点用pong消息作为响应。

5. 集群扩容

   1. 准备新节点。

   2. 加入集群。

      1. ```
         cluster meet
         
         ```

      2. ruby方式：redis-trib.rb add-node new_host:new_port existing_host:existing_port --slave --master-id <arg>

      3. redis-trib.rb add-node 127.0.0.1:6385 127.0.0.1:6379

   3. 迁移槽和数据。

      1. ruby方式：redis-trib.rb reshard host:port --from <arg> --to <arg> --slots <arg> --yes --timeout
         <arg> --pipeline <arg>

      2. ```
         host：port：必传参数，集群内任意节点地址，用来获取整个集群信息。
         --from：制定源节点的id，如果有多个源节点，使用逗号分隔，如果是all源节点变为集群内所有主节点，在迁移过程中提示用户输入。
         --to：需要迁移的目标节点的id，目标节点只能填写一个，在迁移过程中提示用户输入。
         ·--slots：需要迁移槽的总数量，在迁移过程中提示用户输入。
         --yes：当打印出reshard执行计划时，是否需要用户输入yes确认后再执行reshard。
         --timeout：控制每次migrate操作的超时时间，默认为60000毫秒。
         --pipeline：控制每次批量迁移键的数量，默认为10。
         
         ```

         

      ```
      1）对目标节点发送cluster setslot{slot}importing{sourceNodeId}命令，让
      目标节点准备导入槽的数据。
      2）对源节点发送cluster setslot{slot}migrating{targetNodeId}命令，让源
      节点准备迁出槽的数据。
      3）源节点循环执行cluster getkeysinslot{slot}{count}命令，获取count个属于槽{slot}的键。
      4）在源节点上执行migrate{targetIp}{targetPort}""0{timeout}keys{keys...}
      命令，把获取的键通过流水线（pipeline）机制批量迁移到目标节点，批量迁移版本的migrate命令在Redis3.0.6以上版本提供，之前的migrate命令只能单个键迁移。对于大量key的场景，批量键迁移将极大降低节点之间网络IO次数。
      5）重复执行步骤3）和步骤4）直到槽下所有的键值数据迁移到目标节点。
      6）向集群内所有主节点发送cluster setslot{slot}node{targetNodeId}命
      令，通知槽分配给目标节点。为了保证槽节点映射变更及时传播，需要遍历发送给所有主节点更新被迁移的槽指向新节点。
      
      ```

![1566102570483](C:\Users\zxw\Desktop\个人项目笔记\redis\1566102570483.png)

![1566102576171](D:\code\IDEA CODE\Redis-Learn\redis\1566102576171-1567248080161.png)

## 3.Redis配置及参数

### 3.1 Config配置文件

1. daemonize yes:守护进程，3.2版本之后默认为yes
2. pidfile /var/run/redis/pod
3. 
4. ![1566102875041](C:\Users\zxw\Desktop\个人项目笔记\redis\1566102875041.png)

## 4.Java API

1. ![1566104294618](D:\code\IDEA CODE\Redis-Learn\redis\1566104294618-1567248080161.png)
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

   3. ![1566724455762](D:\code\IDEA CODE\Redis-Learn\redis\1566724455762-1567248080161.png)

2. 从节点设置

   1. 从节点默认提供只读操作，并在配置文件开启持久化参数
   2. ![1566724582881](D:\code\IDEA CODE\Redis-Learn\redis\1566724582881-1567248080161.png)

7.3.2 内存配置优化

1. 压缩存储
   1. ZipList
      1. 仅适用于限制范围(限制存储数据量和数据大小)的数据进行操作
      2. 仅适用于列表、散列、有序集合和整数值集合

## 8.  哨兵模式

1. 定义

   1. 本质上也是一个redis服务，只是没有操作。对每个redis服务起监视作用,sentinel会对所有节点进行监控,可以从主节点获取有关从节点以及其余Sentinel节点的相关信息

2. 环境配置

   ```
   port 26379
   daemonize yes
   logfile "26379.log"
   dir /opt/soft/redis/data
   sentinel monitor mymaster 127.0.0.1 6379 2 # 2代表有2个Sentinel节点认为主节点不可达,设置越小条件越宽松，也和领导者选举有关
   sentinel down-after-milliseconds mymaster 30000 #定期发送ping命令来判断Redis数据节点和其余Sentinel节点是否可达，超时则判定不可达，单位为毫秒
   sentinel parallel-syncs mymaster 1 #当sentinel集合对主节点故障判定达成一致时，Sentinel领导者节点会做故障转移操作，选出新的主节点，原来的主节点会向新的主节点发起复制操作，此参数用来限制在一次故障转移之后，每次向新的主节点发起复制操作的节点个数，如果设置较大，多个从节点会向新主节点同时发起复制操作
   sentinel failover-timeout mymaster 180000 # 故障转移超时时间
   #sentinel auth-pass <master-name> <password>
   #sentinel notification-script <master-name> <script-path>
   #sentinel client-reconfig-script <master-name> <script-path>
   
   ```

3. 启动命令

   ```
   redis-server.exe sentinel.conf --sentinel
   
   ```

4. 动态设置参数

   1. ![1566898599732](D:\code\IDEA CODE\Redis-Learn\redis\1566898599732.png)
   2. sentinel set命令只对当前Sentinel节点有效。
   3. sentinel set命令如果执行成功会立即刷新配置文件，这点和Redis普通数据节点设置配置需要执行config rewrite刷新到配置文件不同。
   4. 建议所有Sentinel节点的配置尽可能一致，这样在故障发现和转移时比较容易达成一致。
   5. 表9-3中为sentinel set支持的参数，具体可以参考源码中的sentinel.c的sentinelSetCommand函数。
   6. Sentinel对外不支持config命令。

5. 部署技巧

   1. 不应该部署在一台物理机器：机器故障所有节点都会失效
   2. 奇数部署：领导者选举需要一半+1个节点
   3. 方案一：一套Sentinel，很明显这种方案在一定程度上降低了维护成本，因为只需要维护固定个数的Sentinel节点，集中对多个Redis数据节点进行管理就可以了。但是这同时也是它的缺点，如果这套Sentinel节点集合出现异常，可能会对多个Redis数据节点造成影响。还有如果监控的Redis数据节点较多，会造成Sentinel节点产生过多的网络连接，也会有一定的影响。
   4. 方案二：多套Sentinel，显然这种方案的优点和缺点和上面是相反的，每个Redis主节点都有自己的Sentinel节点集合，会造成资源浪费。但是优点也很明显，每套Redis Sentinel都是彼此隔离的。
   5. 如果Sentinel节点集合监控的是同一个业务的多个主节点集合，那么使用方案一、否则一般建议采用方案二。

### 8.1 API

1. sentinel masters:展示所有被监控的主节点状态以及相关的统计信息
2. sentinel master<master name>:展示指定<master name>的主节点状态以及相关的统计信息
3. sentinel slaves<master name>:展示指定<master name>的从节点状态以及相关的统计信息
4. sentinel sentinels<master name>:展示指定<master name>的Sentinel节点集合
5. sentinel get-master-addr-by-name<master name>:返回指定<master name>主节点的IP地址和端口
6. sentinel reset<pattern>:当前Sentinel节点对符合<pattern>（通配符风格）主节点的配置进行重置，包含清除主节点的相关状态（例如故障转移），重新发现从节点和Sentinel节点。
7. sentinel failover<master name>:对指定<master name>主节点进行强制故障转移（没有和其他Sentinel节点“协商”），当故障转移完成后，其他Sentinel节点按照故障转移的结果更新自身配置，这个命令在Redis Sentinel的日常运维中非常有用。
8. sentinel ckquorum<master name>：检测当前可达的Sentinel节点总数是否达到<quorum>的个数。例如quorum=3，而当前可达的Sentinel节点个数为2个，那么将无法进行故障转移，Redis Sentinel的高可用特性也将失去。
9. sentinel flushconfig：将Sentinel节点的配置强制刷到磁盘上，这个命令Sentinel节点自身用得比较多，对于开发和运维人员只有当外部原因（例如磁盘损坏）造成配置文件损坏或者丢失时，这个命令是很有用的。
10. sentinel remove<master name>：取消当前Sentinel节点对于指定<master name>主节点的监控。
11. sentinel monitor<master name><ip><port><quorum>：这个命令和配置文件中的含义是完全一样的，只不过是通过命令的形式来完成Sentinel节点对主节点的监控。
12. sentinel set<master name>：动态修改Sentinel节点配置选项
13. sentinel is-master-down-by-addr：Sentinel节点之间用来交换对主节点是否下线的判断，根据参数的不同，还可以作为Sentinel领导者选举的通信方式

## 9. 复制

1. slaveof{masterHost}{masterPort}
2. redis-server --slaveof{masterHost}{masterPort}
3. info replication

## 10. 缓存

### 10.1 概念

1. 目标
   1. 加快用户访问数据，提供用户体验
   2. 降低后端负载，减少潜在风险
   3. 保证数据"尽可能"及时更新
2. 缓存的好处
   1. 加速读写，基于全内存
   2. 降低后端负载均衡：帮助后端减少访问量和复杂计算(例如很复杂的SQL语句)
3. 缓存的弊端
   1. 数据不一致性：缓存层和存储层的数据存在着一定时间窗口的不一致性，时间窗口跟更新策略有关
   2. 代码维护成本
   3. 运维成本
4. 基本使用场景
   1. 开销大的复杂计算：一些复杂的操作或者计算（例如大量联表操作、一些分组计算），如果不加缓存，不但无法满足高并发量，同时也会给MySQL带来巨大的负担。
   2. 加速请求响应：即使查询单条后端数据足够快（例如select*from table where id=），那么依然可以使用缓存，以Redis为例子，每秒可以完成数万次读写，并且提供的批量操作可以优化整个IO链的响应时间。
5. 缓存更新策略
   1. LRU/LFU/FIFO算法
      1. 使用场景。剔除算法通常用于缓存使用量超过了预设的最大值时候，如何对现有的数据进行剔除。例如Redis使用maxmemory-policy这个配置作为内存最大值后对于数据的剔除策略。
   2. 超时剔除
      1. 使用场景。超时剔除通过给缓存数据设置过期时间，让其在过期时间后自动删除，例如Redis提供的expire命令。如果业务可以容忍一段时间内，缓存层数据和存储层数据不一致，那么可以为其设置过期时间。在数据过期后，再从真实数据源获取数据，重新放到缓存并设置过期时间。例如一个视频的描述信息，可以容忍几分钟内数据不一致，但是涉及交易方面的业务，后果可想而知。
   3. 主动更新
      1. 使用场景。应用方对于数据的一致性要求高，需要在真实数据更新后，立即更新缓存数据。例如可以利用消息系统或者其他方式通知缓存更新。

### 10.2 穿透优化

1. ![1567219714197](D:\code\IDEA CODE\Redis-Learn\redis\1567219714197.png)

2. 缓存穿透是指查询一个根本不存在的数据，缓存层和存储层都不会命中，通常出于容错的考虑，如果从存储层查不到数据则不写入缓存层

3. 过程

   1. 缓存层不命中。
   2. 存储层不命中，不将空结果写回缓存。
   3. 返回空结果。

4. 解决方案

   1. 缓存空对象，设置过期时间

      ```java
      if (StringUtils.isBlank(cacheValue)) {
      	// 从存储中获取
      	String storageValue = storage.get(key);
      	cache.set(key, storageValue);
      	// 如果存储数据为空，需要设置一个过期时间(300秒)
      	if (storageValue == null) {
      cache.expire(key, 60 * 5);
      }
      	return storageValue;
      } else {
      	// 缓存非空
      	return cacheValue;
      }
      }
      
      ```

   2. 布隆过滤器![1567220530010](D:\code\IDEA CODE\Redis-Learn\redis\1567220530010.png)

   3. 这种方法适用于数据命中不高、数据相对固定、实时性低（通常是数据集较大）的应用场景，代码维护较为复杂，但是缓存空间占用少。

5. 无底洞

   1. 问题分析
      1. 键值数据库由于通常采用哈希函数将key映射到各个节点上，造成key的分布与业务无关，但是由于数据量和访问量的持续增长，造成需要添加大量节点做水平扩容，导致键值分布到更多的节点上，所以无论是Memcache还是Redis的分布式，批量操作通常需要从不同节点上获取，相比于单机批量操作只涉及一次网络操作，分布式批量操作会涉及多次网络时间。
      2. 客户端一次批量操作会涉及多次网络操作，也就意味着批量操作会随着节点的增多，耗时会不断增大。
      3. 网络连接数变多，对节点的性能也有一定影响。

### 10.3 雪崩优化

1. 由于缓存层承载着大量请求，有效地保护了存储层，但是如果缓存层由于某些原因不能提供服务，于是所有的请求都会达到存储层，存储层的调用量会暴增，造成存储层也会级联宕机的情况。缓存雪崩的英文原意是stampeding herd（奔逃的野牛），指的是缓存层宕掉后，流量会像奔逃的野牛一样，打向后端存储。
2. 优化
   1. 保证缓存层服务高可用性
   2. 依赖隔离组件为后端限流并降级
      1. 在实际项目中，我们需要对重要的资源（例如Redis、MySQL、HBase、外部接口）都进行隔离，让每种资源都单独运行在自己的线程池中，即使个别资源出现了问题，对其他服务没有影响。
   3. 提前演练

### 10.4 热点key重建优化

1. ![1567222282089](D:\code\IDEA CODE\Redis-Learn\redis\1567222282089.png)

2. 开发人员使用“缓存+过期时间”的策略既可以加速数据读写，又保证数据的定期更新，这种模式基本能够满足绝大部分需求。但是有两个问题如果同时出现，可能就会对应用造成致命的危害：当前key是一个热点key（例如一个热门的娱乐新闻），并发量非常大。重建缓存不能在短时间完成，可能是一个复杂计算，例如复杂的SQL、多次IO、多个依赖等。在缓存失效的瞬间，有大量线程来重建缓存（如图11-16所示），造成后端负载加大，甚至可能会让应用崩溃。要解决这个问题也不是很复杂，但是不能为了解决这个问题给系统带来更多的麻烦，所以需要制定如下目标：

   1. 减少重建缓存的次数
   2. 数据尽可能一致。
   3. 较少的潜在危险。

3. 互斥锁

   1. 只允许一个线程重建缓存，其他线程等待重建缓存的线程执行完，重新从缓存获取数据即可

      ```java
      String get(String key) {
          // 从Redis中获取数据
          String value = redis.get(key);
          // 如果value为空，则开始重构缓存
          if (value == null) {
      		// 只允许一个线程重构缓存，使用nx，并设置过期时间ex
      		String mutexKey = "mutext:key:" + key;
              if (redis.set(mutexKey, "1", "ex 180", "nx")) {
              // 从数据源获取数据
              value = db.get(key);
              // 回写Redis，并设置过期时间
              redis.setex(key, timeout, value);
              // 删除key_mutex
              redis.delete(mutexKey);
      	}
      	// 其他线程休息50毫秒后重试
      	else {
      		Thread.sleep(50);
      		get(key);
      	}
      	}
      	return value;
      }
      
      ```

4. 永远不过期

   1. 从缓存层面来看，确实没有设置过期时间，所以不会出现热点key过期后产生的问题，也就是“物理”不过期。

   2. 从功能层面来看，为每个value设置一个逻辑过期时间，当发现超过逻辑过期时间后，会使用单独的线程去构建缓存。

      ```java
      long logicTimeout = v.getLogicTimeout();
      // 如果逻辑过期时间小于当前时间，开始后台构建
      if (v.logicTimeout <= 				System.currentTimeMillis()) {
      	String mutexKey = "mutex:key:" + key;
          if (redis.set(mutexKey, "1", "ex 180", "nx")) {
          // 重构缓存
          threadPool.execute(new Runnable() {
      	public void run() {
      		String dbValue = db.get(key);
      		redis.set(key, (dbvalue,newLogicTimeout));
      		redis.delete(mutexKey);
      	}
      	});
      	}
      }
      return value;
      }
      
      ```

## 11. 持久化

### 11.1 RDB

1. 把当前进程数据生成快照保存到硬盘，分为手动触发和自动触发
2. save:阻塞当前Redis服务器，直到RDB过程完成为止，对于内存
   比较大的实例会造成长时间阻塞
3. bgsave:Redis进程执行fork操作创建子进程，RDB持久化过程由子进程负责，完成后自动结束。阻塞只发生在fork阶段，一般时间很短。
4. 自动触发场景
   1. 使用save相关配置，如“save m n”。表示m秒内数据集存在n次修改时，自动触发bgsave。
   2. 如果从节点执行全量复制操作，主节点自动执行bgsave生成RDB文件并发送给从节点，更多细节见6.3节介绍的复制原理。
   3. 执行debug reload命令重新加载Redis时，也会自动触发save操作。
   4. 默认情况下执行shutdown命令时，如果没有开启AOF持久化功能则自动执行bgsave。

### 11.2 AOF

1. 以独立日志的方式记录每次写命令，重启时再重新执行AOF文件中的命令达到恢复数据的目的。主要作用解决了数据持久化的实时性，目前已经是Redis持久化的主流方式
2. 配置appeddonly yes，默认不开启,默认文件名是appendoly.aof
3. 流程
   1. 写入命令append
   2. 文件同步sync
   3. 文件重写rewrite
   4. 重启加载load
4. 重写机制：把Redis进程内的数据转化为写命令同步到新AOF文件的过程
5. 触发
   1. 手动触发：bgrewriteaof
   2. 自动触发：auto-aof-rewrite-min-size和auto-aof-rewirte-precentage

### 11.3 问题定位与优化

1. fork操作耗时定位问题
   1. 优先使用物理机或者高效支持fork操作的虚拟化技术，避免使用Xen。
   2. 控制Redis实例最大可用内存，fork耗时跟内存量成正比，线上建议每个Redis实例内存控制在10GB以内。
   3. 合理配置Linux内存分配策略，避免物理内存不足导致fork失败，具体细节见12.1节“Linux配置优化”。
   4. 降低fork操作的频率，如适度放宽AOF自动触发时机，避免不必要的全量复制等。
2. 子进程开销监控和优化
   1. CPU
   2. 内存
   3. 硬盘

### 11.4 多实例部署

![1567235263119](D:\code\IDEA CODE\Redis-Learn\redis\1567235263119.png)

## 12. 阻塞

1. ```java
   public class Redis Appender extends AppenderBase<ILoggingEvent> {
   // 使用guava的AtomicLongMap,用于并发计数
   public static final AtomicLongMap<String> ATOMIC_LONG_MAP = AtomicLongMap.create();
   static {
   // 自定义Appender加入到logback的rootLogger中
   LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
   Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
   ErrorStatisticsAppender errorStatisticsAppender = new ErrorStatisticsAppender();
   errorStatisticsAppender.setContext(loggerContext);
   errorStatisticsAppender.start();
   rootLogger.addAppender(errorStatisticsAppender);
   }
   // 重写接收日志事件方法
   protected void append(ILoggingEvent event) {
   // 只监控error级别日志
   if (event.getLevel() == Level.ERROR) {
   IThrowableProxy throwableProxy = event.getThrowableProxy();
   // 确认抛出异常
   if (throwableProxy != null) {
   // 以每分钟为key，记录每分钟异常数量
   String key = DateUtil.formatDate(new Date(), "yyyyMMddHHmm");
   long errorCount = ATOMIC_LONG_MAP.incrementAndGet(key);
   if (errorCount > 10) {
   // 超过10次触发报警代码
   }
   // 清理历史计数统计，防止极端情况下内存泄露
   for (String oldKey : ATOMIC_LONG_MAP.asMap().keySet()) {
   if (!StringUtils.equals(key, oldKey)) {
   ATOMIC_LONG_MAP.remove(oldKey);
   }
   }
   }
   }
   }
   
   ```

2. API或数据结构使用不合理

   1. 慢查询：slowlog get{n}获取最近的n条慢查询命令，默认对于执行超过10毫秒级以上的命令都会记录到一个定长队列中

3. 持久化阻塞