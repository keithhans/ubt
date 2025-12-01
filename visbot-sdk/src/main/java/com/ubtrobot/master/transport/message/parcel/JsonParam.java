package com.ubtrobot.master.transport.message.parcel;

import java.nio.charset.StandardCharsets;

/**
 * JsonParam - JSON参数类
 * 用于传递JSON格式的参数
 *
 * 根据 Master 服务的参数格式实现
 * type = "JsonParam", bytes = JSON字符串的UTF-8字节数组
 */
public class JsonParam extends AbstractParam {
    public static final String TYPE = "JsonParam";

    private final String json;

    public JsonParam(String json) {
        super(TYPE, json != null ? json.getBytes(StandardCharsets.UTF_8) : new byte[0]);
        this.json = json;
    }

    public String getJson() {
        return json;
    }

    @Override
    public String toString() {
        return "JsonParam{json='" + json + "'}";
    }
}

