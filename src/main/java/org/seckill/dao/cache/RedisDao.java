package org.seckill.dao.cache;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import org.seckill.entity.Seckill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Created by huyiqing on 16/12/13.
 */
public class RedisDao {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final JedisPool jedisPool;
    //模式
    private RuntimeSchema<Seckill> schema = RuntimeSchema.createFrom(Seckill.class);

    public RedisDao (String ip, int port) {
        jedisPool = new JedisPool(ip, port);
    }

    public Seckill getSeckill(long seckillId){
        //redis
        Jedis jedis = new Jedis();
        try {
             jedis = jedisPool.getResource();
             String key = "seckill:" + seckillId;
             //并没有实现内部序列化
             //get->byte[]->反序列化->Object
             //采用自定义
             //protostuff : pojo  getter setter
             byte[] bytes = jedis.get(key.getBytes());
             if (bytes != null) {
                //空对象
                Seckill seckill = schema.newMessage();
                ProtostuffIOUtil.mergeFrom(bytes,seckill,schema);
                //seckill被反序列化
                return seckill;
             }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            jedis.close();
        }
        return null;
    }

    public String putSeckill(Seckill seckill) {
        // Object  -->  byte[] serialization
        Jedis jedis = new Jedis();
        try {
            String key = "seckill:" + seckill.getSeckillId();
            byte[] bytes = ProtostuffIOUtil.toByteArray(seckill,schema,
                    LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
            //超时缓存
            int timeout = 60 * 60;
            String result = jedis.setex(key.getBytes(), timeout, bytes);
            return result;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            jedis.close();
        }
        return null;
    }
}
