package com.zxw.pojo;

import java.io.Serializable;

/**
 * @author zxw
 * @date 2019/8/25 14:08
 */
public class AdContent implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 广告内容Id
     */
    private int id;
    /**
     * 广告内容名称
     */
    private String name;
    /**
     * 广告连接URL
     */
    private String url;
    /**
     * 广告图片URL
     */
    private String imageUrl;
    /**
     * 广告序号
     */
    private int sequence;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }
}
