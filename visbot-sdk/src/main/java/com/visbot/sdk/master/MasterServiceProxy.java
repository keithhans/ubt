package com.visbot.sdk.master;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.ubtrobot.competition.CompetitionSessionInfo;

import java.util.Map;

/**
 * Master服务代理类
 * 
 * 使用ContentProvider + Binder方式调用Visbot的Master服务
 * Master服务是Visbot系统的核心服务协调器
 */
public class MasterServiceProxy {
    private static final String TAG = "MasterServiceProxy";
    
    private final MasterConnection connection;
    private final Gson gson;
    
    public MasterServiceProxy(Context context) {
        this.connection = new MasterConnection(context);
        this.gson = new Gson();
        connect();
    }
    
    /**
     * 连接到Master服务
     */
    private void connect() {
        connection.connect();
    }
    
    /**
     * 调用Master服务（不带会话信息）
     *
     * @param path 服务路径，如 "/servo/rotate"
     * @param params 参数Map
     * @return 响应字符串，失败返回null
     */
    public String call(String path, Map<String, Object> params) {
        return call(path, params, null);
    }

    /**
     * 调用Master服务（带会话信息）
     *
     * @param path 服务路径，如 "/servo/rotate"
     * @param params 参数Map
     * @param sessionInfo 会话信息，可以为null
     * @return 响应字符串，失败返回null
     */
    public String call(String path, Map<String, Object> params, CompetitionSessionInfo sessionInfo) {
        Log.i(TAG, "=== call() START ===");
        Log.i(TAG, "call() - path: " + path + ", params: " + params);
        Log.i(TAG, "Session info: " + (sessionInfo != null ? sessionInfo.getSessionId() : "null"));

        if (!isConnected()) {
            Log.e(TAG, "Not connected to Master service");
            return null;
        }

        try {
            // 将参数转换为JSON
            String paramsJson = gson.toJson(params);
            Log.i(TAG, "Params JSON: " + paramsJson);

            // 调用Master服务
            Log.i(TAG, "Calling connection.call()...");
            String result = connection.call(path, paramsJson, sessionInfo);
            Log.i(TAG, "Call result: " + result);
            Log.i(TAG, "=== call() END ===");
            return result;

        } catch (Exception e) {
            Log.e(TAG, "Failed to call Master service", e);
            return null;
        }
    }
    
    /**
     * 调用Master服务（使用Parcelable参数）
     *
     * @param path 服务路径，如 "/servo/rotate"
     * @param param Parcelable参数对象
     * @param sessionInfo 会话信息，可以为null
     * @return 响应字符串，失败返回null
     */
    public String callWithParcelable(String path, android.os.Parcelable param, CompetitionSessionInfo sessionInfo) {
        Log.i(TAG, "=== callWithParcelable() START ===");
        Log.i(TAG, "callWithParcelable() - path: " + path + ", param type: " + (param != null ? param.getClass().getName() : "null"));
        Log.i(TAG, "Session info: " + (sessionInfo != null ? sessionInfo.getSessionId() : "null"));

        if (!isConnected()) {
            Log.e(TAG, "Not connected to Master service");
            return null;
        }

        try {
            // 调用Master服务
            Log.i(TAG, "Calling connection.callWithParcelable()...");
            String result = connection.callWithParcelable(path, param, sessionInfo);
            Log.i(TAG, "Call result: " + result);
            Log.i(TAG, "=== callWithParcelable() END ===");
            return result;

        } catch (Exception e) {
            Log.e(TAG, "Failed to call Master service with Parcelable", e);
            return null;
        }
    }

    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return connection.isConnected();
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        connection.disconnect();
    }
}

