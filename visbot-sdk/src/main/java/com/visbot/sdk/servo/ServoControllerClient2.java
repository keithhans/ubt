package com.visbot.sdk.servo;

import android.util.Log;

import com.ubtrobot.competition.CompetitionSession;
import com.ubtrobot.competition.SessionAllocator;
import com.ubtrobot.context.RobotContext;
import com.ubtrobot.master.adapter.ParcelableCallAdapter;
import com.ubtrobot.master.adapter.CallAdapter;
import com.ubtrobot.servo.RotationOption;
import com.ubtrobot.servo.RotationOptionList;
import com.ubtrobot.servo.RotationProgress;
import com.ubtrobot.servo.ServoConstants;
import com.ubtrobot.servo.ServoException;
import com.ubtrobot.exception.CallExceptionTranslator;
import com.ubtrobot.transport.message.CallException;
import com.ubtrobot.async.ProgressivePromise;

import java.util.Collections;
import java.util.HashSet;

/**
 * 舵机控制客户端 - 使用 ParcelableCallAdapter
 * 
 * 这个实现模仿 ServoManager 的方式，使用 SessionAllocator 和 ParcelableCallAdapter
 */
public class ServoControllerClient2 {
    private static final String TAG = "ServoControllerClient2";
    
    private final RobotContext robotContext;
    private final SessionAllocator sessionAllocator;
    
    public ServoControllerClient2(RobotContext robotContext) {
        this.robotContext = robotContext;
        // 创建 SessionAllocator，就像 ServoManager 一样
        this.sessionAllocator = new SessionAllocator("servo", ServoConstants.COMPETING_ITEM_PREFIX_SERVO);
        Log.i(TAG, "ServoControllerClient2 initialized with SessionAllocator");
    }
    
    /**
     * 旋转舵机到指定角度
     * @param servoId 舵机ID (例如: "head_servo_1")
     * @param angle 目标角度 (-90 到 90 度)
     * @param speed 旋转速度 (0 到 100)
     * @return Promise
     */
    public ProgressivePromise<Void, ServoException, RotationProgress> rotate(String servoId, float angle, int speed) {
        Log.i(TAG, "=== rotate() called ===");
        Log.i(TAG, String.format("Rotating servo %s to %.1f degrees at speed %d", servoId, angle, speed));
        
        // 创建 RotationOption
        RotationOption option = new RotationOption.Builder(servoId)
            .setAngleAbsolute(true)  // 绝对角度
            .setAngle(angle)
            .setSpeed(speed)
            .build();
        
        // 分配会话 - 传入舵机ID集合
        HashSet<String> servoIds = new HashSet<>();
        servoIds.add(servoId);
        CompetitionSession session = sessionAllocator.allocate(servoIds);
        
        Log.i(TAG, "Allocated session for servo: " + servoId);
        
        // 创建 ParcelableCallAdapter 并传入会话
        ParcelableCallAdapter adapter = new ParcelableCallAdapter(robotContext, "servo", session);
        
        // 调用服务
        return adapter.callStickily(
            ServoConstants.CALL_PATH_ROTATE,
            new RotationOptionList(Collections.singletonList(option)),
            RotationProgress.class,
            new CallAdapter.FConverter<ServoException>() {
                @Override
                public ServoException convertFail(CallException e) {
                    return new ServoException(
                        CallExceptionTranslator.translate(e),
                        e.getSubCode(),
                        e.getMessage()
                    );
                }
            }
        );
    }
}

