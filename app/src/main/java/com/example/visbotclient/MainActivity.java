package com.example.visbotclient;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.visbot.sdk.motor.MotorControllerClient;
import com.visbot.sdk.servo.ServoControllerClient;

/**
 * 主Activity - Visbot机器人控制界面
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    // 控制器
    private ServoControllerClient servoController;
    private MotorControllerClient motorController;
    
    // UI组件
    private Button btnServoCenter;
    private Button btnHeadUp;
    private Button btnHeadDown;
    private Button btnMoveForward;
    private Button btnMoveBackward;
    private Button btnTurnLeft;
    private Button btnTurnRight;
    private Button btnStop;
    
    // 固定参数
    private static final float MOVE_SPEED = 50.0f;
    private static final float TURN_ANGLE = 90.0f;
    private static final float HEAD_UP_ANGLE = -10.0f;
    private static final float HEAD_DOWN_ANGLE = 10.0f;
    private static final int SERVO_SPEED = 50;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Log.i(TAG, "MainActivity onCreate");
        
        // 初始化UI
        initViews();
        
        // 初始化控制器
        initControllers();
        
        // 设置监听器
        setupListeners();
    }
    
    /**
     * 初始化UI组件
     */
    private void initViews() {
        btnServoCenter = findViewById(R.id.btn_servo_center);
        btnHeadUp = findViewById(R.id.btn_head_up);
        btnHeadDown = findViewById(R.id.btn_head_down);
        btnMoveForward = findViewById(R.id.btn_move_forward);
        btnMoveBackward = findViewById(R.id.btn_move_backward);
        btnTurnLeft = findViewById(R.id.btn_turn_left);
        btnTurnRight = findViewById(R.id.btn_turn_right);
        btnStop = findViewById(R.id.btn_stop);
    }
    
    /**
     * 初始化控制器
     */
    private void initControllers() {
        try {
            // 创建舵机控制器
            servoController = new ServoControllerClient(this);

            // 创建电机控制器
            motorController = new MotorControllerClient(this);

            // 检查连接状态
            if (servoController.isConnected()) {
                showToast("连接成功");
            } else {
                showToast("连接失败");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize controllers", e);
            showToast("初始化失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置按钮监听器
     */
    private void setupListeners() {
        // 抬头
        btnHeadUp.setOnClickListener(v -> {
            servoController.rotate("head", HEAD_UP_ANGLE, SERVO_SPEED);
        });
        
        // 低头
        btnHeadDown.setOnClickListener(v -> {
            servoController.rotate("head", HEAD_DOWN_ANGLE, SERVO_SPEED);
        });
        
        // 舵机归中
        btnServoCenter.setOnClickListener(v -> {
            servoController.rotate("head", 0.0f, SERVO_SPEED);
        });
        
        // 前进
        btnMoveForward.setOnClickListener(v -> {
            motorController.moveForward(MOVE_SPEED, 300);
        });

        // 后退
        btnMoveBackward.setOnClickListener(v -> {
            motorController.moveBackward(MOVE_SPEED, 300);
        });

        // 左转
        btnTurnLeft.setOnClickListener(v -> {
            motorController.turnLeft(MOVE_SPEED, TURN_ANGLE);
        });

        // 右转
        btnTurnRight.setOnClickListener(v -> {
            motorController.turnRight(MOVE_SPEED, TURN_ANGLE);
        });

        // 停止
        btnStop.setOnClickListener(v -> {
            motorController.stop();
        });
    }
    
    /**
     * 显示Toast消息
     */
    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 断开连接
        if (servoController != null) {
            servoController.disconnect();
        }

        // MotorControllerClient 使用 Robot.globalContext()，不需要手动断开连接

        Log.i(TAG, "MainActivity onDestroy");
    }
}

