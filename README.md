# VisbotClient - Visbot 机器人控制客户端

## 📋 项目概述

VisbotClient 是一个 Android 应用程序，用于控制 Visbot 机器人（Rosa 版本）的舵机和电机。该应用通过 IPC（进程间通信）与机器人的系统服务进行交互，实现了完整的机器人控制功能。

### 主要特性

- ✅ **舵机控制** - 控制机器人头部舵机旋转
- ✅ **电机控制** - 控制机器人移动（前进、后退、左转、右转）
- ✅ **IPC 通信** - 通过 Master 服务与系统服务通信
- ✅ **会话管理** - 使用 Robot.globalContext() 管理资源竞争
- ✅ **小屏幕适配** - 针对 320x240 分辨率优化的 UI

### 技术栈

- **开发语言**: Java
- **目标平台**: Android 5.1 (API Level 22)
- **构建工具**: Gradle 8.7
- **SDK**: Visbot Rosa SDK (rosa.jar)
- **签名**: Android 系统签名 (platform.pk8/platform.x509.pem)

---

## 🏗️ 技术架构

### 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                     VisbotClient App                        │
│  ┌──────────────────┐          ┌──────────────────┐        │
│  │ ServoController  │          │ MotorController  │        │
│  │     Client       │          │     Client       │        │
│  └────────┬─────────┘          └────────┬─────────┘        │
│           │                              │                  │
│           └──────────┬───────────────────┘                  │
│                      │                                      │
│           ┌──────────▼──────────┐                          │
│           │  Robot.globalContext()                         │
│           │  (rosa.jar SDK)     │                          │
│           └──────────┬──────────┘                          │
└──────────────────────┼──────────────────────────────────────┘
                       │ IPC (Binder)
┌──────────────────────▼──────────────────────────────────────┐
│              Master Service (系统服务)                       │
│  ┌──────────────────┐          ┌──────────────────┐        │
│  │  Servo Service   │          │ Locomotion       │        │
│  │                  │          │ Service          │        │
│  └────────┬─────────┘          └────────┬─────────┘        │
└───────────┼──────────────────────────────┼──────────────────┘
            │                              │
┌───────────▼──────────────────────────────▼──────────────────┐
│                    硬件层                                    │
│         舵机硬件                    电机硬件                 │
└─────────────────────────────────────────────────────────────┘
```

### 核心组件

#### 1. ServoControllerClient
- **功能**: 控制舵机旋转
- **实现方式**: 使用 `Robot.globalContext().getSystemService("servo")`
- **主要方法**:
  - `rotate(int servoId, float angle)` - 旋转到指定角度
  - `center()` - 归中所有舵机

#### 2. MotorControllerClient
- **功能**: 控制机器人移动
- **实现方式**: 使用 `Robot.globalContext().getSystemService("locomotion")`
- **主要方法**:
  - `moveForward(speed, duration)` - 前进
  - `moveBackward(speed, duration)` - 后退
  - `turnLeft(speed, angle)` - 左转
  - `turnRight(speed, angle)` - 右转
  - `stop()` - 紧急停止

#### 3. Robot SDK (rosa.jar)
- **来源**: `/system/framework/rosa.jar`
- **核心类**:
  - `Robot` - SDK 入口，提供服务注册和初始化
  - `GlobalContext` - 全局上下文，用于获取系统服务
  - `ServoController` - 舵机控制接口
  - `LocomotionController` - 电机控制接口

---

## 🚀 快速开始

### 环境要求

- **JDK**: 1.8 或更高
- **Android SDK**: API Level 22 (Android 5.1)
- **Gradle**: 8.7
- **ADB**: 用于安装和调试

### 构建项目

```bash
# 进入项目目录
cd VisbotClient

# 清理并构建 Debug 版本
.\gradlew.bat clean assembleDebug

# 构建成功后，APK 位于：
# app/build/outputs/apk/debug/app-debug.apk
```

### 安装到设备

```bash
# 安装 APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 强制停止旧版本
adb shell am force-stop com.example.visbotclient

# 启动应用
adb shell am start -n com.example.visbotclient/.MainActivity
```

### 查看日志

```bash
# 清除旧日志
adb logcat -c

# 实时查看应用日志
adb logcat | Select-String -Pattern "VisbotClient|ServoController|MotorController|MainActivity"

# 或者使用 grep (Linux/Mac)
adb logcat | grep -E "VisbotClient|ServoController|MotorController|MainActivity"
```

---

## 📱 功能说明

### 舵机控制

#### 参数说明
- **舵机 ID**: 1-6（对应不同的舵机）
- **角度范围**: -180° 到 +180°
- **速度**: 0-100（百分比）

#### 使用示例
```java
ServoControllerClient servoController = new ServoControllerClient(context);

// 旋转舵机 1 到 90 度
servoController.rotate(1, 90.0f);

// 所有舵机归中
servoController.center();
```

#### 日志示例
```
ServoControllerClient: Rotating servo 1 to angle 90.00
MST|LoggingHandler: resultType='success', code=0
```

### 电机控制

#### 参数说明

**移动速度 (movingSpeed)**:
- 范围: 0.0 - 1.0
- 正值 = 前进，负值 = 后退
- 默认: 0.3

**转向速度 (turningSpeed)**:
- 范围: 度/秒
- 正值 = 逆时针（左转），负值 = 顺时针（右转）
- 默认: 30.0 度/秒

**持续时间 (duration)**:
- 单位: 毫秒
- 0 = 持续移动（直到下一个命令）
- 默认: 1000ms

#### 使用示例
```java
MotorControllerClient motorController = new MotorControllerClient(context);

// 前进 1 秒
motorController.moveForward(0.3f, 1000);

// 后退 1 秒
motorController.moveBackward(0.3f, 1000);

// 左转 90 度
motorController.turnLeft(30.0f, 90.0f);

// 右转 90 度
motorController.turnRight(30.0f, 90.0f);

// 紧急停止
motorController.stop();
```

#### 关键实现细节

**前进/后退**:
```java
// 前进: movingSpeed > 0, movingAngle = 0
new LocomotionOption.Builder()
    .setMovingSpeed(Math.abs(speed))
    .setMovingAngle(0)
    .build();

// 后退: movingSpeed < 0, movingAngle = 0
new LocomotionOption.Builder()
    .setMovingSpeed(-Math.abs(speed))
    .setMovingAngle(0)
    .build();
```

**左转/右转**:
```java
// 左转: turningSpeed > 0 (逆时针)
new LocomotionOption.Builder()
    .setTurningSpeed(Math.abs(speed))
    .setTurningAngle(Math.abs(angle))
    .build();

// 右转: turningSpeed < 0 (顺时针)
new LocomotionOption.Builder()
    .setTurningSpeed(-Math.abs(speed))
    .setTurningAngle(Math.abs(angle))
    .build();
```

#### 日志示例
```
MotorControllerClient: Moving forward: speed=0.30, duration=1000 ms
MotorControllerClient: locomote() promise created successfully
MST|LoggingHandler: resultType='success', code=0
```

---

## 🔧 IPC 通信详解

详细的 IPC 通信实现请参考 [IPC通信实现指南.md](IPC通信实现指南.md)

### 通信流程

1. **初始化 Robot SDK**
   ```java
   Robot.initialize(context.getApplicationContext());
   ```

2. **获取系统服务**
   ```java
   ServoController servoController = 
       Robot.globalContext().getSystemService("servo");
   
   LocomotionController locomotionController = 
       Robot.globalContext().getSystemService("locomotion");
   ```

3. **调用服务方法**
   ```java
   // 舵机控制
   ProgressivePromise<Void, ServoException, ServoProgress> promise = 
       servoController.rotate(servoId, angle);
   
   // 电机控制
   ProgressivePromise<Void, LocomotionException, LocomotionProgress> promise = 
       locomotionController.locomote(option);
   ```

4. **会话管理**
   - Robot.globalContext() 自动管理会话
   - 自动处理资源竞争（navigation, recharging）
   - 支持请求中断和优先级管理

---

## 📂 项目结构

```
VisbotClient/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/visbotclient/
│   │   │   ├── MainActivity.java              # 主界面
│   │   │   ├── servo/
│   │   │   │   └── ServoControllerClient.java # 舵机控制客户端
│   │   │   ├── motor/
│   │   │   │   └── MotorControllerClient.java # 电机控制客户端
│   │   │   └── master/
│   │   │       ├── MasterConnection.java      # Master 服务连接（已废弃）
│   │   │       └── MasterServiceProxy.java    # Master 服务代理（已废弃）
│   │   ├── res/
│   │   │   └── layout/
│   │   │       └── activity_main.xml          # UI 布局（320x240 优化）
│   │   └── AndroidManifest.xml                # 应用配置
│   ├── libs/
│   │   └── rosa.jar                           # Visbot SDK
│   └── build.gradle                           # 应用构建配置
├── keystore/
│   ├── platform.pk8                           # 系统签名私钥
│   └── platform.x509.pem                      # 系统签名证书
├── gradle/                                    # Gradle 配置
├── build.gradle                               # 项目构建配置
├── settings.gradle                            # 项目设置
├── README.md                                  # 本文档
└── IPC通信实现指南.md                         # IPC 通信详细文档
```

---

## 🐛 调试技巧

### 查看 Master 服务日志
```bash
adb logcat | Select-String -Pattern "MST"
```

### 查看服务调用详情
```bash
adb logcat | Select-String -Pattern "MST\|LoggingHandler"
```

### 查看会话管理
```bash
adb logcat | Select-String -Pattern "MST\|CompetitionManager"
```

### 查看应用崩溃
```bash
adb logcat | Select-String -Pattern "AndroidRuntime"
```

---

## 📝 开发历程

### 关键技术突破

1. **IPC 通信方案演进**
   - 初期：自定义 IPC 实现（失败）
   - 最终：使用 Robot.globalContext() SDK（成功）

2. **电机控制参数发现**
   - 前进/后退：通过 `movingSpeed` 符号控制（正=前进，负=后退）
   - 左转/右转：通过 `turningSpeed` 符号控制（正=逆时针，负=顺时针）

3. **UI 适配**
   - 针对 320x240 小屏幕优化
   - 添加 ScrollView 支持滚动
   - 减小字体和控件尺寸

---

## 📄 许可证

本项目为内部开发项目，版权归属于项目所有者。

---

## 👥 贡献者

- 开发者：通过 AI 辅助完成
- 测试：在 Visbot Rosa 机器人上验证

---

## 📞 联系方式

如有问题或建议，请联系项目维护者。

