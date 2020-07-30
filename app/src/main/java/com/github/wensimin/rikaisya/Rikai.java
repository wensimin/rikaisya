package com.github.wensimin.rikaisya;

import com.github.wensimin.rikaisya.api.RikaiType;

/**
 * 理解实体
 */
public class Rikai {
    /**
     * 文本
     */
    private String text;
    /**
     * 类型
     */
    private RikaiType type;

    public Rikai(RikaiType type, String text) {
        this.type = type;
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public RikaiType getType() {
        return type;
    }
}
