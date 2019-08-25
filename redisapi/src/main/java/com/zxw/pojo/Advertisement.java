package com.zxw.pojo;

import java.io.Serializable;
import java.util.List;

/**
 * @author zxw
 * @date 2019/8/25 14:06
 */
public class Advertisement implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id; // 广告Id
    private String positionCode; // 广告位代码
    private int tid; // 广告模板Id
    private List<AdContent> adContents; // 广告内容集合

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPositionCode() {
        return positionCode;
    }

    public void setPositionCode(String positionCode) {
        this.positionCode = positionCode;
    }

    public int getTid() {
        return tid;
    }

    public void setTid(int tid) {
        this.tid = tid;
    }

    public List<AdContent> getAdContents() {
        return adContents;
    }

    public void setAdContents(List<AdContent> adContents) {
        this.adContents = adContents;
    }
}
