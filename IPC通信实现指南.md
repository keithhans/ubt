# VisbotClient IPC 通信实现指南

## 📋 目录

1. [概述](#概述)
2. [技术架构](#技术架构)
3. [环境准备](#环境准备)
4. [构建流程](#构建流程)
5. [安装与测试](#安装与测试)
6. [日志读取](#日志读取)
7. [舵机控制详解](#舵机控制详解)
8. [电机控制详解](#电机控制详解)
9. [故障排查](#故障排查)

---

## 概述

VisbotClient 使用 **Robot SDK (rosa.jar)** 与 Visbot 机器人的系统服务进行 IPC 通信。通信基于 Android Binder 机制，通过 Master 服务作为中介，实现应用层与硬件层的交互。

### 核心概念

- **Robot SDK**: Visbot 提供的官方 SDK，封装了所有系统服务的访问接口
- **Master Service**: 系统级服务，负责路由和管理所有服务调用
- **GlobalContext**: SDK 提供的全局上下文，用于获取系统服务
- **ProgressivePromise**: 异步调用的返回对象，支持进度回调

---

## 技术架构

### 通信层次

```
┌─────────────────────────────────────────────────────────┐
│  应用层 (VisbotClient)                                  │
│  ┌──────────────────────────────────────────────┐      │
│  │  ServoControllerClient / MotorControllerClient      │
│  └────────────────┬─────────────────────────────┘      │
└───────────────────┼──────────────────────────────────────┘
                    │
┌───────────────────▼──────────────────────────────────────┐
│  SDK 层 (rosa.jar)                                       │
│  ┌──────────────────────────────────────────────┐      │
│  │  Robot.globalContext()                       │      │
│  │  ├─ ServoController                          │      │
│  │  ├─ LocomotionController                     │      │
│  │  └─ ParcelableCallAdapter                    │      │
│  └────────────────┬─────────────────────────────┘      │
└───────────────────┼──────────────────────────────────────┘
                    │ Binder IPC
┌───────────────────▼──────────────────────────────────────┐
│  系统服务层 (com.ubtrobot.systemservice)                │
│  ┌──────────────────────────────────────────────┐      │
│  │  Master Service                              │      │
│  │  ├─ Servo Service (com.ubtrobot.servo)      │      │
│  │  ├─ Locomotion Service (com.ubtrobot.locomotion)   │
│  │  └─ Competition Manager (资源竞争管理)      │      │
│  └────────────────┬─────────────────────────────┘      │
└───────────────────┼──────────────────────────────────────┘
                    │
┌───────────────────▼──────────────────────────────────────┐
│  硬件层                                                  │
│  ├─ 舵机硬件                                            │
│  └─ 电机硬件                                            │
└─────────────────────────────────────────────────────────┘
```

### 关键组件

#### 1. Robot SDK 初始化
```java
// 在 Application 或 Activity 中初始化
Robot.initialize(context.getApplicationContext());
```

#### 2. 获取系统服务
```java
// 获取舵机控制器
ServoController servoController = 
    Robot.globalContext().getSystemService("servo");

// 获取电机控制器
LocomotionController locomotionController = 
    Robot.globalContext().getSystemService("locomotion");
```

#### 3. 调用服务方法
```java
// 异步调用，返回 Promise
ProgressivePromise<Void, ServoException, ServoProgress> promise = 
    servoController.rotate(servoId, angle);
```

---

## 环境准备

### 必需工具

1. **JDK 1.8+**
   ```bash
   java -version
   ```

2. **Android SDK (API Level 22)**
   - 安装 Android SDK Platform 22
   - 安装 Android SDK Build-Tools

3. **Gradle 8.7**
   - 项目已包含 Gradle Wrapper

4. **ADB (Android Debug Bridge)**
   ```bash
   adb version
   ```

### 签名文件

项目使用 Android 系统签名，需要以下文件：

- `keystore/platform.pk8` - 系统签名私钥
- `keystore/platform.x509.pem` - 系统签名证书

这些文件已包含在项目中。

---

## 构建流程

### 1. 清理项目
```bash
cd VisbotClient
.\gradlew.bat clean
```

### 2. 构建 Debug 版本
```bash
.\gradlew.bat assembleDebug
```

### 3. 构建输出
成功后，APK 位于：
```
app/build/outputs/apk/debug/app-debug.apk
```

### 4. 查看构建日志
```bash
# 详细日志
.\gradlew.bat assembleDebug --info

# 调试日志
.\gradlew.bat assembleDebug --debug
```

### 常见构建问题

#### 问题 1: Gradle 版本不匹配
```
解决方案: 使用项目自带的 gradlew.bat
```

#### 问题 2: SDK 路径未配置
```
解决方案: 在 local.properties 中设置 sdk.dir
```

#### 问题 3: 签名文件缺失
```
解决方案: 确保 keystore/ 目录下有 platform.pk8 和 platform.x509.pem
```

---

## 安装与测试

### 安装流程

#### 1. 连接设备
```bash
# 检查设备连接
adb devices

# 应该看到类似输出:
# List of devices attached
# 192.168.1.100:5555    device
```

#### 2. 安装 APK
```bash
# 安装（覆盖旧版本）
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### 3. 启动应用
```bash
# 强制停止旧版本
adb shell am force-stop com.example.visbotclient

# 启动应用
adb shell am start -n com.example.visbotclient/.MainActivity
```

### 一键安装脚本

```bash
# Windows PowerShell
cd VisbotClient
.\gradlew.bat assembleDebug
if ($?) {
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    adb shell am force-stop com.example.visbotclient
    adb shell am start -n com.example.visbotclient/.MainActivity
}
```

### 测试流程

#### 1. 舵机测试
1. 在 UI 中输入舵机 ID (1-6)
2. 输入角度 (-180 到 180)
3. 点击"旋转舵机"按钮
4. 观察机器人头部是否旋转
5. 点击"归中"按钮，观察是否回到中间位置

#### 2. 电机测试
1. 点击"前进"按钮，观察机器人是否前进
2. 点击"后退"按钮，观察机器人是否后退
3. 点击"左转90°"按钮，观察机器人是否左转
4. 点击"右转90°"按钮，观察机器人是否右转
5. 点击"停止所有运动"按钮，观察机器人是否停止

---

## 日志读取

### 基础日志命令

#### 清除日志
```bash
adb logcat -c
```

#### 实时查看所有日志
```bash
adb logcat
```

#### 过滤应用日志
```bash
# Windows PowerShell
adb logcat | Select-String -Pattern "VisbotClient"

# Linux/Mac
adb logcat | grep "VisbotClient"
```

### 专项日志

#### 1. 应用层日志
```bash
adb logcat | Select-String -Pattern "VisbotClient|ServoController|MotorController|MainActivity"
```

#### 2. Master 服务日志
```bash
adb logcat | Select-String -Pattern "MST"
```

#### 3. 服务调用详情
```bash
adb logcat | Select-String -Pattern "MST\|LoggingHandler"
```

#### 4. 会话管理日志
```bash
adb logcat | Select-String -Pattern "MST\|CompetitionManager"
```

#### 5. 错误日志
```bash
adb logcat | Select-String -Pattern "ERROR|Exception|FATAL"
```

### 日志分析示例

#### 成功的舵机调用
```
11-08 23:10:15.123  MainActivity: 旋转舵机 1 到角度 90.00
11-08 23:10:15.125  ServoControllerClient: Rotating servo 1 to angle 90.00
11-08 23:10:15.130  MST|LoggingHandler: onRead. path=/servo/rotate
11-08 23:10:15.145  MST|LoggingHandler: write. resultType='success', code=0
```

#### 成功的电机调用
```
11-08 23:15:20.456  MainActivity: 前进
11-08 23:15:20.458  MotorControllerClient: Moving forward: speed=0.30
11-08 23:15:20.460  MotorControllerClient: locomote() promise created successfully
11-08 23:15:20.465  MST|LoggingHandler: onRead. path=/locomotor/locomote
11-08 23:15:20.480  MST|LoggingHandler: write. resultType='success', code=0
```

#### 失败的调用
```
11-08 23:20:30.789  MST|LoggingHandler: write. resultType='failure', code=-11, 
    message='Call NOT found. service = xxx, callPath=/xxx/xxx'
```

---

## 舵机控制详解

### API 接口

```java
public class ServoControllerClient {
    // 旋转指定舵机到指定角度
    public boolean rotate(int servoId, float angle);
    
    // 所有舵机归中
    public boolean center();
    
    // 检查是否已连接
    public boolean isConnected();
}
```

### 参数说明

- **servoId**: 舵机编号 (1-6)
- **angle**: 目标角度 (-180° 到 +180°)
- **speed**: 旋转速度 (0-100，百分比)

### 实现原理

```java
// 1. 初始化
Robot.initialize(context);

// 2. 获取 ServoController
ServoController controller = Robot.globalContext().getSystemService("servo");

// 3. 创建 ServoJoint
ServoJoint joint = new ServoJoint.Builder()
    .setId(servoId)
    .setAngle(angle)
    .build();

// 4. 调用 rotate
ProgressivePromise<Void, ServoException, ServoProgress> promise = 
    controller.rotate(joint);
```

### 日志追踪

完整的舵机调用日志：
```
ServoControllerClient: Rotating servo 1 to angle 90.00
ServoControllerClient: Created ServoJoint: id=1, angle=90.0
MST|LoggingHandler: onRead. path=/servo/rotate, 
    context=RequestContext{requesterType='global-context', responder='servo'}
MST|CompetitionManager: Acquire session success
MST|LoggingHandler: write. resultType='success', code=0
```

---

## 电机控制详解

### API 接口

```java
public class MotorControllerClient {
    // 前进
    public boolean moveForward(float speed, long duration);
    public boolean moveForward();  // 使用默认参数
    
    // 后退
    public boolean moveBackward(float speed, long duration);
    public boolean moveBackward();
    
    // 左转
    public boolean turnLeft(float speed, float angle);
    public boolean turnLeft();  // 默认转 90°
    
    // 右转
    public boolean turnRight(float speed, float angle);
    public boolean turnRight();
    
    // 停止
    public boolean stop();
    
    // 自定义运动
    public boolean customMove(float movingSpeed, float movingAngle,
                             float turningSpeed, float turningAngle,
                             long duration);
}
```

### 参数说明

#### movingSpeed (移动速度)
- **类型**: float
- **范围**: 0.0 - 1.0
- **符号含义**:
  - **正值** (+): 前进
  - **负值** (-): 后退
- **默认值**: 0.3

#### turningSpeed (转向速度)
- **类型**: float
- **单位**: 度/秒
- **符号含义**:
  - **正值** (+): 逆时针旋转（左转）
  - **负值** (-): 顺时针旋转（右转）
- **默认值**: 30.0

#### movingAngle (移动角度)
- **类型**: float
- **单位**: 度
- **范围**: 0° - 360°
- **说明**: 移动方向（相对于机器人正前方）
  - 0° = 正前方
  - 90° = 左侧
  - 180° = 正后方
  - 270° = 右侧

#### turningAngle (转向角度)
- **类型**: float
- **单位**: 度
- **说明**: 需要转动的角度（绝对值）

#### duration (持续时间)
- **类型**: long
- **单位**: 毫秒
- **说明**: 
  - 0 = 持续移动（直到下一个命令）
  - >0 = 移动指定时间后停止

### 关键实现

#### 前进实现
```java
public boolean moveForward(float speed, long duration) {
    LocomotionOption option = new LocomotionOption.Builder()
        .setMovingSpeed(Math.abs(speed))  // 正值 = 前进
        .setMovingAngle(0)                // 0° = 正前方
        .setDuration(duration)
        .build();
    
    return locomote(option);
}
```

#### 后退实现
```java
public boolean moveBackward(float speed, long duration) {
    LocomotionOption option = new LocomotionOption.Builder()
        .setMovingSpeed(-Math.abs(speed))  // 负值 = 后退
        .setMovingAngle(0)                 // 保持 0°
        .setDuration(duration)
        .build();
    
    return locomote(option);
}
```

#### 左转实现
```java
public boolean turnLeft(float speed, float angle) {
    LocomotionOption option = new LocomotionOption.Builder()
        .setTurningSpeed(Math.abs(speed))   // 正值 = 逆时针
        .setTurningAngle(Math.abs(angle))   // 角度绝对值
        .setTurningAxis(LocomotionOption.TURNING_AXIS_CENTER)
        .build();
    
    return locomote(option);
}
```

#### 右转实现
```java
public boolean turnRight(float speed, float angle) {
    LocomotionOption option = new LocomotionOption.Builder()
        .setTurningSpeed(-Math.abs(speed))  // 负值 = 顺时针
        .setTurningAngle(Math.abs(angle))   // 角度绝对值
        .setTurningAxis(LocomotionOption.TURNING_AXIS_CENTER)
        .build();
    
    return locomote(option);
}
```

### 日志追踪

完整的电机调用日志：
```
MotorControllerClient: Moving forward: speed=0.30, duration=1000 ms
MotorControllerClient: === locomote() called ===
MotorControllerClient: Option: LocomotionOption{movingSpeed=0.3, movingAngle=0.0, 
    turningSpeed=0.0, turningAngle=0.0, duration=1000}
MotorControllerClient: locomote() promise created successfully
MST|LoggingHandler: onRead. path=/locomotor/locomote
MST|CompetitionManager: Acquire session success. sessionId='xxx'
MST|LoggingHandler: write. resultType='success', code=0
MST|CompetitionManager: Remove session success
```

---

## 故障排查

### 常见问题

#### 1. 应用无法启动
**症状**: 安装后点击图标无反应

**排查步骤**:
```bash
# 查看崩溃日志
adb logcat | Select-String -Pattern "AndroidRuntime"

# 查看应用进程
adb shell ps | Select-String -Pattern "visbotclient"
```

**可能原因**:
- 签名不正确（需要系统签名）
- 权限不足（需要 android.uid.system）

#### 2. 服务调用失败
**症状**: 日志显示 "Call NOT found"

**排查步骤**:
```bash
# 查看 Master 服务状态
adb shell dumpsys activity services com.ubtrobot.systemservice

# 查看服务调用日志
adb logcat | Select-String -Pattern "MST\|LoggingHandler"
```

**可能原因**:
- Robot.initialize() 未调用
- 服务名称错误
- 系统服务未启动

#### 3. 舵机/电机不动作
**症状**: 命令发送成功但硬件无响应

**排查步骤**:
```bash
# 查看硬件层日志
adb logcat | Select-String -Pattern "servo|locomotion" -CaseSensitive:$false

# 检查参数是否正确
adb logcat | Select-String -Pattern "LocomotionOption|ServoJoint"
```

**可能原因**:
- 参数超出范围
- 硬件故障
- 电源不足

#### 4. 会话竞争失败
**症状**: 日志显示 "Interrupted by the later operation"

**说明**: 这是正常现象，表示新命令中断了旧命令

**解决方案**: 
- 等待当前操作完成再发送新命令
- 或使用 stop() 先停止当前操作

---

## 附录

### 相关文件

- `README.md` - 项目总览
- `app/src/main/java/com/example/visbotclient/servo/ServoControllerClient.java` - 舵机控制实现
- `app/src/main/java/com/example/visbotclient/motor/MotorControllerClient.java` - 电机控制实现
- `app/libs/rosa.jar` - Visbot SDK

### 参考资源

- Visbot Rosa SDK 文档
- Android Binder IPC 机制
- Gradle 构建系统文档

---

**文档版本**: 1.0  
**最后更新**: 2025-11-08  
**适用版本**: VisbotClient v1.0

