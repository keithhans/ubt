package com.visbot.sdk.motor;

import android.content.Context;
import android.util.Log;

import com.ubtrobot.Robot;
import com.ubtrobot.async.ProgressivePromise;
import com.ubtrobot.locomotion.LocomotionController;
import com.ubtrobot.locomotion.LocomotionException;
import com.ubtrobot.locomotion.LocomotionOption;
import com.ubtrobot.locomotion.LocomotionProgress;

/**
 * 电机控制器Client端实现
 * 使用 rosa.jar 中的 LocomotionController (通过 Robot.globalContext().getSystemService())
 */
public class MotorControllerClient {
    private static final String TAG = "MotorControllerClient";

    // 默认速度和持续时间
    private static final float DEFAULT_MOVING_SPEED = 0.3f;  // 默认移动速度 (0-1)
    private static final float DEFAULT_TURNING_SPEED = 30.0f; // 默认转向速度 (度/秒)
    private static final long DEFAULT_DURATION = 1000; // 默认持续时间 1秒

    private LocomotionController locomotionController;
    private Context context;

    /**
     * 构造函数
     * @param context Android上下文
     */
    public MotorControllerClient(Context context) {
        this.context = context;

        try {
            // 初始化 Robot (如果还没有初始化)
            Robot.initialize(context.getApplicationContext());

            // 通过 Robot.globalContext() 获取 LocomotionController
            locomotionController = Robot.globalContext().getSystemService("locomotion");

            if (locomotionController != null) {
                Log.i(TAG, "MotorControllerClient initialized successfully with LocomotionController");
            } else {
                Log.e(TAG, "Failed to get LocomotionController from Robot.globalContext()");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MotorControllerClient", e);
        }
    }

    /**
     * 执行电机运动
     * @param option 运动选项
     * @return 是否成功发送命令
     */
    private boolean locomote(LocomotionOption option) {
        if (locomotionController == null) {
            Log.e(TAG, "LocomotionController is null, cannot execute locomote");
            return false;
        }

        Log.i(TAG, "=== locomote() called ===");
        Log.i(TAG, "Option: " + option);

        try {
            // 调用 LocomotionController.locomote()
            ProgressivePromise<Void, LocomotionException, LocomotionProgress> promise =
                locomotionController.locomote(option);

            if (promise != null) {
                Log.i(TAG, "locomote() promise created successfully");
                return true;
            } else {
                Log.e(TAG, "locomote() returned null promise");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calling locomote()", e);
            return false;
        }
    }

    /**
     * 前进
     * @param speed 移动速度 (0-1) - 必须为正值
     * @param duration 持续时间 (毫秒)
     * @return 是否成功发送命令
     */
    public boolean moveForward(float speed, long duration) {
        Log.i(TAG, String.format("Moving forward: speed=%.2f, duration=%d ms", speed, duration));

        // 前进：movingSpeed 为正值，movingAngle = 0
        LocomotionOption option = new LocomotionOption.Builder()
            .setMovingSpeed(Math.abs(speed))  // 确保为正值
            .setMovingAngle(0)  // 0度表示前进方向
            .setDuration(duration)
            .build();

        return locomote(option);
    }

    /**
     * 前进 (使用默认参数)
     * @return 是否成功发送命令
     */
    public boolean moveForward() {
        return moveForward(DEFAULT_MOVING_SPEED, DEFAULT_DURATION);
    }

    /**
     * 后退
     * @param speed 移动速度 (0-1) - 必须为正值
     * @param duration 持续时间 (毫秒)
     * @return 是否成功发送命令
     */
    public boolean moveBackward(float speed, long duration) {
        Log.i(TAG, String.format("Moving backward: speed=%.2f, duration=%d ms", speed, duration));

        // 后退：movingSpeed 为负值（或者 movingAngle = 180）
        // 尝试使用负速度来表示后退
        LocomotionOption option = new LocomotionOption.Builder()
            .setMovingSpeed(-Math.abs(speed))  // 负值表示后退
            .setMovingAngle(0)  // 保持角度为0
            .setDuration(duration)
            .build();

        return locomote(option);
    }

    /**
     * 后退 (使用默认参数)
     * @return 是否成功发送命令
     */
    public boolean moveBackward() {
        return moveBackward(DEFAULT_MOVING_SPEED, DEFAULT_DURATION);
    }

    /**
     * 左转
     * @param speed 转向速度 (度/秒) - 必须为正值
     * @param angle 转向角度 (度) - 必须为正值
     * @return 是否成功发送命令
     */
    public boolean turnLeft(float speed, float angle) {
        Log.i(TAG, String.format("Turning left: speed=%.2f deg/s, angle=%.2f deg", speed, angle));

        // 左转 = 逆时针 = turningSpeed 为正值
        LocomotionOption option = new LocomotionOption.Builder()
            .setTurningSpeed(Math.abs(speed))  // 确保为正值（逆时针）
            .setTurningAngle(Math.abs(angle))  // 角度为正值
            .setTurningAxis(LocomotionOption.TURNING_AXIS_CENTER)
            .build();

        return locomote(option);
    }

    /**
     * 左转 (使用默认参数，转90度)
     * @return 是否成功发送命令
     */
    public boolean turnLeft() {
        return turnLeft(DEFAULT_TURNING_SPEED, 90.0f);
    }

    /**
     * 右转
     * @param speed 转向速度 (度/秒) - 必须为正值
     * @param angle 转向角度 (度) - 必须为正值
     * @return 是否成功发送命令
     */
    public boolean turnRight(float speed, float angle) {
        Log.i(TAG, String.format("Turning right: speed=%.2f deg/s, angle=%.2f deg", speed, angle));

        // 右转 = 顺时针 = turningSpeed 为负值
        LocomotionOption option = new LocomotionOption.Builder()
            .setTurningSpeed(-Math.abs(speed))  // 负值表示顺时针
            .setTurningAngle(Math.abs(angle))   // 角度为正值
            .setTurningAxis(LocomotionOption.TURNING_AXIS_CENTER)
            .build();

        return locomote(option);
    }

    /**
     * 右转 (使用默认参数，转90度)
     * @return 是否成功发送命令
     */
    public boolean turnRight() {
        return turnRight(DEFAULT_TURNING_SPEED, 90.0f);
    }

    /**
     * 停止运动
     * @return 是否成功发送命令
     */
    public boolean stop() {
        Log.i(TAG, "Stopping motor");

        // 创建一个速度为0的运动选项来停止
        LocomotionOption option = new LocomotionOption.Builder()
            .setMovingSpeed(0)
            .setTurningSpeed(0)
            .setEmergency(true)  // 紧急停止
            .build();

        return locomote(option);
    }

    /**
     * 自定义运动
     * @param movingSpeed 移动速度 (0-1)
     * @param movingAngle 移动角度 (度)
     * @param turningSpeed 转向速度 (度/秒)
     * @param turningAngle 转向角度 (度)
     * @param duration 持续时间 (毫秒)
     * @return 是否成功发送命令
     */
    public boolean customMove(float movingSpeed, float movingAngle,
                             float turningSpeed, float turningAngle,
                             long duration) {
        Log.i(TAG, String.format("Custom move: movingSpeed=%.2f, movingAngle=%.2f, " +
                "turningSpeed=%.2f, turningAngle=%.2f, duration=%d ms",
                movingSpeed, movingAngle, turningSpeed, turningAngle, duration));

        LocomotionOption option = new LocomotionOption.Builder()
            .setMovingSpeed(movingSpeed)
            .setMovingAngle(movingAngle)
            .setTurningSpeed(turningSpeed)
            .setTurningAngle(turningAngle)
            .setTurningAxis(LocomotionOption.TURNING_AXIS_CENTER)
            .setDuration(duration)
            .build();

        return locomote(option);
    }

    /**
     * 检查LocomotionController是否可用
     * @return 是否可用
     */
    public boolean isConnected() {
        return locomotionController != null;
    }
}

