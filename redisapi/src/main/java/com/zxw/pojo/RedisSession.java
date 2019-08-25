package com.zxw.pojo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author zxw
 * @date 2019/8/25 16:00
 */
public class RedisSession implements Serializable {
    private static final long serialVersionUID = 1L;
    private String sessionId = UUID.randomUUID().toString();

    private Map<String,Object> map = new HashMap<>();
}
