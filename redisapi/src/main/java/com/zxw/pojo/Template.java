package com.zxw.pojo;

import java.io.Serializable;

/**
 * @author zxw
 * @date 2019/8/25 14:11
 */
public class Template implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 广告模板Id
     */
    private int id;
    /**
     * 广告模板名称
     */
    private String name;
    /**
     * 广告模板脚本
     */
    private String script;

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

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }
}
