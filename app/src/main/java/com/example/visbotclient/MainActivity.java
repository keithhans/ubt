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
    private TextView tvStatus;
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
        tvStatus = findViewById(R.id.tv_status);
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
        updateStatus("正在连接Master服务...");
        
        try {
            // 创建舵机控制器
            servoController = new ServoControllerClient(this);

            // 创建电机控制器
            motorController = new MotorControllerClient(this);

            // 检查连接状态
            if (servoController.isConnected()) {
                updateStatus("✓ Master服务连接成功");
                showToast("连接成功");
            } else {
                updateStatus("✗ Master服务连接失败");
                updateStatus("请确保：");
                updateStatus("1. APK使用系统签名");
                updateStatus("2. AndroidManifest.xml中设置了sharedUserId");
                updateStatus("3. 设备上已安装Visbot系统服务");
                showToast("连接失败");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize controllers", e);
            updateStatus("✗ 初始化失败: " + e.getMessage());
            showToast("初始化失败");
        }
    }
    
    /**
     * 设置按钮监听器
     */
    private void setupListeners() {
        // 抬头
        btnHeadUp.setOnClickListener(v -> {
            updateStatus("抬头 (" + HEAD_UP_ANGLE + "°)");
            boolean success = servoController.rotate("head", HEAD_UP_ANGLE, SERVO_SPEED);
            showToast(success ? "抬头命令已发送" : "抬头命令发送失败");
        });
        
        // 低头
        btnHeadDown.setOnClickListener(v -> {
            updateStatus("低头 (" + HEAD_DOWN_ANGLE + "°)");
            boolean success = servoController.rotate("head", HEAD_DOWN_ANGLE, SERVO_SPEED);
            showToast(success ? "低头命令已发送" : "低头命令发送失败");
        });
        
        // 舵机归中
        btnServoCenter.setOnClickListener(v -> {
            updateStatus("舵机归中");
            boolean success = servoController.rotate("head", 0.0f, SERVO_SPEED);
            showToast(success ? "归中命令已发送" : "归中命令发送失败");
        });
        
        // 前进
        btnMoveForward.setOnClickListener(v -> {
            updateStatus("前进 (速度: " + MOVE_SPEED + ")");
            boolean success = motorController.moveForward(MOVE_SPEED, 0);
            showToast(success ? "前进命令已发送" : "前进命令发送失败");
        });

        // 后退
        btnMoveBackward.setOnClickListener(v -> {
            updateStatus("后退 (速度: " + MOVE_SPEED + ")");
            boolean success = motorController.moveBackward(MOVE_SPEED, 0);
            showToast(success ? "后退命令已发送" : "后退命令发送失败");
        });

        // 左转
        btnTurnLeft.setOnClickListener(v -> {
            updateStatus("左转 " + TURN_ANGLE + "°");
            boolean success = motorController.turnLeft(MOVE_SPEED, TURN_ANGLE);
            showToast(success ? "左转命令已发送" : "左转命令发送失败");
        });

        // 右转
        btnTurnRight.setOnClickListener(v -> {
            updateStatus("右转 " + TURN_ANGLE + "°");
            boolean success = motorController.turnRight(MOVE_SPEED, TURN_ANGLE);
            showToast(success ? "右转命令已发送" : "右转命令发送失败");
        });

        // 停止
        btnStop.setOnClickListener(v -> {
            updateStatus("停止所有运动");
            boolean success = motorController.stop();
            showToast(success ? "停止命令已发送" : "停止命令发送失败");
        });
    }
    
    /**
     * 更新状态显示
     */
    private void updateStatus(String message) {
        runOnUiThread(() -> {
            String currentText = tvStatus.getText().toString();
            String newText = message + "\n" + currentText;
            
            // 限制显示行数
            String[] lines = newText.split("\n");
            if (lines.length > 20) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 20; i++) {
                    sb.append(lines[i]).append("\n");
                }
                newText = sb.toString();
            }
            
            tvStatus.setText(newText);
            Log.i(TAG, message);
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

