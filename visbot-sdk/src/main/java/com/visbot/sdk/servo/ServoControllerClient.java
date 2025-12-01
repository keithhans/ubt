package com.visbot.sdk.servo;

import android.content.Context;
import android.util.Log;

import com.visbot.sdk.master.MasterServiceProxy;
import com.ubtrobot.competition.CompetingItem;
import com.ubtrobot.competition.CompetitionSession;
import com.ubtrobot.competition.CompetitionSessionInfo;
import com.ubtrobot.competition.SessionAllocator;
import com.ubtrobot.servo.ServoConstants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 舵机控制器Client端实现
 * 通过Master服务控制舵机
 *
 * 使用 SessionAllocator 来正确分配会话，就像 ServoManager 一样
 */
public class ServoControllerClient {
    private static final String TAG = "ServoControllerClient";

    // 服务路径常量
    private static final String PATH_ROTATE = "/servo/rotate";
    private static final String PATH_ROTATE_SERIALLY = "/servo/rotate-serially";
    private static final String PATH_GET_ANGLE = "/servo/angle";
    private static final String PATH_RELEASE = "/servo/release";
    private static final String PATH_IS_ROTATING = "/servo/is-rotating";
    private static final String PATH_GET_DEVICE = "/servo/device";
    private static final String PATH_GET_DEVICE_LIST = "/servo/device-list";

    private MasterServiceProxy master;
    private Context context;
    private SessionAllocator sessionAllocator;

    /**
     * 构造函数
     * @param context Android上下文
     */
    public ServoControllerClient(Context context) {
        this.context = context;
        this.master = new MasterServiceProxy(context);

        // 创建 SessionAllocator，就像 ServoManager 一样
        this.sessionAllocator = new SessionAllocator("servo", ServoConstants.COMPETING_ITEM_PREFIX_SERVO);
        Log.i(TAG, "ServoControllerClient initialized with SessionAllocator");

        if (!master.isConnected()) {
            Log.e(TAG, "Failed to connect to Master service");
        }
    }

    /**
     * 为指定的舵机分配会话
     * @param servoId 舵机ID，例如 "head_servo_1"
     * @return 会话信息
     */
    private CompetitionSessionInfo allocateSessionForServo(String servoId) {
        // 使用 SessionAllocator 分配会话
        HashSet<String> servoIds = new HashSet<>();
        servoIds.add(servoId);

        CompetitionSession session = sessionAllocator.allocate(servoIds);

        // 转换为 CompetitionSessionInfo
        CompetitionSessionInfo.Builder builder = new CompetitionSessionInfo.Builder()
            .setSessionId(session.getSessionId());

        for (CompetingItem item : session.getCompetingItems()) {
            builder.addCompetingItem(item);
        }

        CompetitionSessionInfo sessionInfo = builder.build();

        Log.i(TAG, "Allocated session for servo " + servoId + ": " + sessionInfo.getSessionId());
        Log.i(TAG, "Competing items: " + sessionInfo.getCompetingItems());

        return sessionInfo;
    }
    
    /**
     * 旋转舵机到指定角度
     * @param servoId 舵机ID (例如: "head_servo_1")
     * @param angle 目标角度 (-90 到 90 度)
     * @param speed 旋转速度 (0 到 100)
     * @return 是否成功发送命令
     */
    public boolean rotate(String servoId, float angle, int speed) {
        return rotate(servoId, angle, speed, 0, false);
    }
    
    /**
     * 旋转舵机到指定角度 (完整参数)
     * @param servoId 舵机ID
     * @param angle 目标角度
     * @param speed 旋转速度 (0-100)
     * @param duration 持续时间 (毫秒，0表示自动计算)
     * @param relative 是否相对旋转
     * @return 是否成功发送命令
     */
    public boolean rotate(String servoId, float angle, int speed, int duration, boolean relative) {
        Log.i(TAG, "=== rotate() called ===");
        Log.i(TAG, String.format("Rotating servo %s to %.1f degrees at speed %d",
            servoId, angle, speed));

        // 使用 SessionAllocator 分配会话
        CompetitionSessionInfo sessionInfo = allocateSessionForServo(servoId);

        // 创建 RotationOption - 使用 SDK 的标准方式
        com.ubtrobot.servo.RotationOption.Builder optionBuilder =
            new com.ubtrobot.servo.RotationOption.Builder(servoId);

        optionBuilder.setAngle(angle)
                     .setSpeed(speed);

        if (duration > 0) {
            optionBuilder.setDuration(duration);
        }

        // 设置角度模式：angleAbsolute=true 表示绝对角度，false 表示相对角度
        // relative=true 时，angleAbsolute=false
        optionBuilder.setAngleAbsolute(!relative);

        com.ubtrobot.servo.RotationOption option = optionBuilder.build();

        // 创建 RotationOptionList
        java.util.List<com.ubtrobot.servo.RotationOption> optionList = new java.util.ArrayList<>();
        optionList.add(option);
        com.ubtrobot.servo.RotationOptionList rotationOptionList =
            new com.ubtrobot.servo.RotationOptionList(optionList);

        Log.i(TAG, "Created RotationOptionList with " + optionList.size() + " options");
        Log.i(TAG, "Calling master.callWithParcelable() with path: " + ServoConstants.CALL_PATH_ROTATE);

        // 调用Master服务 - 使用 Parcelable 参数
        String result = master.callWithParcelable(ServoConstants.CALL_PATH_ROTATE, rotationOptionList, sessionInfo);

        Log.i(TAG, "master.callWithParcelable() returned: " + result);

        return result != null;
    }
    

    
    /**
     * 获取舵机当前角度
     * @param servoId 舵机ID
     * @return 当前角度，失败返回0.0f
     */
    public float getAngle(String servoId) {
        Log.d(TAG, "Getting angle for servo: " + servoId);

        // 分配会话
        CompetitionSessionInfo sessionInfo = allocateSessionForServo(servoId);

        // 构建参数
        Map<String, Object> params = new HashMap<>();
        params.put("servoId", servoId);

        // 调用Master服务
        Object result = master.call(PATH_GET_ANGLE, params, sessionInfo);

        if (result instanceof Float) {
            return (Float) result;
        } else if (result instanceof Double) {
            return ((Double) result).floatValue();
        }

        Log.w(TAG, "Failed to get angle for servo: " + servoId);
        return 0.0f;
    }

    /**
     * 检查舵机是否正在旋转
     * @param servoId 舵机ID
     * @return 是否正在旋转
     */
    public boolean isRotating(String servoId) {
        Log.d(TAG, "Checking if servo is rotating: " + servoId);

        // 分配会话
        CompetitionSessionInfo sessionInfo = allocateSessionForServo(servoId);

        // 构建参数
        Map<String, Object> params = new HashMap<>();
        params.put("servoId", servoId);

        // 调用Master服务
        Object result = master.call(PATH_IS_ROTATING, params, sessionInfo);

        if (result instanceof Boolean) {
            return (Boolean) result;
        }

        return false;
    }

    /**
     * 释放舵机 (断电)
     * @param servoId 舵机ID
     * @return 是否成功
     */
    public boolean release(String servoId) {
        Log.d(TAG, "Releasing servo: " + servoId);

        // 分配会话
        CompetitionSessionInfo sessionInfo = allocateSessionForServo(servoId);

        // 构建参数
        Map<String, Object> params = new HashMap<>();
        params.put("servoId", servoId);

        // 调用Master服务
        Object result = master.call(PATH_RELEASE, params, sessionInfo);

        return result != null;
    }
    
    /**
     * 停止舵机旋转
     * @param servoId 舵机ID
     * @return 是否成功
     */
    public boolean stop(String servoId) {
        Log.d(TAG, "Stopping servo: " + servoId);

        // 通过释放舵机来停止
        return release(servoId);
    }

    /**
     * 获取所有舵机设备列表
     * @return 舵机设备列表的 JSON 字符串，失败返回null
     */
    public String getDeviceListJson() {
        Log.i(TAG, "Getting device list");

        // 调用Master服务 - 不需要会话
        Object result = master.call(ServoConstants.CALL_PATH_GET_DEVICE_LIST, new HashMap<>(), null);

        Log.i(TAG, "Device list result: " + result);

        if (result != null) {
            return result.toString();
        }

        return null;
    }

    /**
     * 检查Master服务是否已连接
     * @return 是否已连接
     */
    public boolean isConnected() {
        return master.isConnected();
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        master.disconnect();
    }
}

