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
    private EditText etServoAngle;
    private EditText etServoSpeed;
    private EditText etMoveSpeed;
    private Button btnConnect;
    private Button btnServoRotate;
    private Button btnServoCenter;
    private Button btnMoveForward;
    private Button btnMoveBackward;
    private Button btnTurnLeft;
    private Button btnTurnRight;
    private Button btnStop;
    
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
        etServoAngle = findViewById(R.id.et_servo_angle);
        etServoSpeed = findViewById(R.id.et_servo_speed);
        etMoveSpeed = findViewById(R.id.et_move_speed);

        btnConnect = findViewById(R.id.btn_connect);
        btnServoRotate = findViewById(R.id.btn_servo_rotate);
        btnServoCenter = findViewById(R.id.btn_servo_center);
        btnMoveForward = findViewById(R.id.btn_move_forward);
        btnMoveBackward = findViewById(R.id.btn_move_backward);
        btnTurnLeft = findViewById(R.id.btn_turn_left);
        btnTurnRight = findViewById(R.id.btn_turn_right);
        btnStop = findViewById(R.id.btn_stop);

        // 设置默认值
        etServoAngle.setText("45");
        etServoSpeed.setText("50");
        etMoveSpeed.setText("50");
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
                btnConnect.setText("已连接");
                btnConnect.setEnabled(false);

                // 自动测试：延迟2秒后测试舵机旋转
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "=== 自动测试：触发舵机旋转 ===");
                        testServoRotation();
                    }
                }, 2000);
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
        // 舵机旋转
        btnServoRotate.setOnClickListener(v -> {
            Log.i(TAG, "=== 舵机旋转按钮被点击 ===");

            try {
                float angle = Float.parseFloat(etServoAngle.getText().toString());
                int speed = Integer.parseInt(etServoSpeed.getText().toString());

                Log.i(TAG, "解析参数成功 - angle: " + angle + ", speed: " + speed);

                // 检查 servoController 是否为 null
                if (servoController == null) {
                    Log.e(TAG, "servoController is NULL!");
                    showToast("舵机控制器未初始化");
                    return;
                }

                Log.i(TAG, "servoController 不为空，准备调用 rotate()");
                updateStatus("旋转舵机到 " + angle + "° (速度: " + speed + ")");

                // 使用正确的舵机 ID: "head"
                Log.i(TAG, "调用 servoController.rotate() - servoId: head, angle: " + angle + ", speed: " + speed);
                boolean success = servoController.rotate("head", angle, speed);
                Log.i(TAG, "servoController.rotate() 返回: " + success);

                if (success) {
                    showToast("舵机旋转命令已发送");
                } else {
                    showToast("舵机旋转命令发送失败");
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "数值解析失败", e);
                showToast("请输入有效的数值");
            } catch (Exception e) {
                Log.e(TAG, "舵机旋转异常", e);
                showToast("舵机旋转异常: " + e.getMessage());
            }
        });
        
        // 舵机归中
        btnServoCenter.setOnClickListener(v -> {
            updateStatus("舵机归中");
            // 使用正确的舵机 ID: "head"
            boolean success = servoController.rotate("head", 0.0f, 50);
            showToast(success ? "归中命令已发送" : "归中命令发送失败");
        });
        
        // 前进
        btnMoveForward.setOnClickListener(v -> {
            try {
                float speed = Float.parseFloat(etMoveSpeed.getText().toString());

                updateStatus("前进 (速度: " + speed + ")");
                // 持续移动，duration=0
                boolean success = motorController.moveForward(speed, 0);

                showToast(success ? "前进命令已发送" : "前进命令发送失败");
            } catch (NumberFormatException e) {
                showToast("请输入有效的数值");
            }
        });

        // 后退
        btnMoveBackward.setOnClickListener(v -> {
            try {
                float speed = Float.parseFloat(etMoveSpeed.getText().toString());

                updateStatus("后退 (速度: " + speed + ")");
                // 持续移动，duration=0
                boolean success = motorController.moveBackward(speed, 0);

                showToast(success ? "后退命令已发送" : "后退命令发送失败");
            } catch (NumberFormatException e) {
                showToast("请输入有效的数值");
            }
        });

        // 左转
        btnTurnLeft.setOnClickListener(v -> {
            updateStatus("左转 90°");
            boolean success = motorController.turnLeft(50.0f, 90.0f);
            showToast(success ? "左转命令已发送" : "左转命令发送失败");
        });

        // 右转
        btnTurnRight.setOnClickListener(v -> {
            updateStatus("右转 90°");
            boolean success = motorController.turnRight(50.0f, 90.0f);
            showToast(success ? "右转命令已发送" : "右转命令发送失败");
        });

        // 停止
        btnStop.setOnClickListener(v -> {
            updateStatus("停止所有运动");

            // 停止移动
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

    /**
     * 自动测试获取舵机列表
     */
    private void testGetServoList() {
        Log.i(TAG, "=== testGetServoList() called ===");
        try {
            String deviceList = servoController.getDeviceListJson();
            Log.i(TAG, "舵机列表: " + deviceList);
            updateStatus("舵机列表: " + deviceList);
        } catch (Exception e) {
            Log.e(TAG, "获取舵机列表失败", e);
        }
    }

    /**
     * 自动测试舵机旋转
     */
    private void testServoRotation() {
        Log.i(TAG, "=== testServoRotation() called ===");
        try {
            // 使用正确的舵机 ID: "head"
            String servoId = "head";
            float angle = 10.0f;  // 在范围内：-23.0 到 25.0
            int speed = 15;  // 使用默认速度

            Log.i(TAG, "调用 servoController.rotate() - servoId: " + servoId + ", angle: " + angle + ", speed: " + speed);
            boolean result = servoController.rotate(servoId, angle, speed);
            Log.i(TAG, "servoController.rotate() 返回: " + result);

            if (result) {
                updateStatus("✓ 舵机旋转命令已发送");
            } else {
                updateStatus("✗ 舵机旋转命令失败");
            }
        } catch (Exception e) {
            Log.e(TAG, "测试舵机旋转失败", e);
            updateStatus("✗ 测试失败: " + e.getMessage());
        }
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

