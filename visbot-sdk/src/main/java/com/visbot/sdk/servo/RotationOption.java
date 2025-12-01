package com.visbot.sdk.servo;

/**
 * 舵机旋转选项
 * 封装舵机旋转的所有参数
 */
public class RotationOption {
    private String servoId;      // 舵机ID
    private float angle;         // 目标角度 (-90 到 90 度)
    private int speed;           // 旋转速度 (0 到 100)
    private int duration;        // 持续时间 (毫秒，0表示自动计算)
    private boolean relative;    // 是否相对旋转
    
    /**
     * 构造函数
     */
    public RotationOption(String servoId, float angle, int speed) {
        this(servoId, angle, speed, 0, false);
    }
    
    /**
     * 完整构造函数
     */
    public RotationOption(String servoId, float angle, int speed, int duration, boolean relative) {
        this.servoId = servoId;
        this.angle = angle;
        this.speed = speed;
        this.duration = duration;
        this.relative = relative;
    }
    
    // Getters
    public String getServoId() {
        return servoId;
    }
    
    public float getAngle() {
        return angle;
    }
    
    public int getSpeed() {
        return speed;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public boolean isRelative() {
        return relative;
    }
    
    // Setters
    public void setServoId(String servoId) {
        this.servoId = servoId;
    }
    
    public void setAngle(float angle) {
        this.angle = angle;
    }
    
    public void setSpeed(int speed) {
        this.speed = speed;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public void setRelative(boolean relative) {
        this.relative = relative;
    }
    
    @Override
    public String toString() {
        return "RotationOption{" +
                "servoId='" + servoId + '\'' +
                ", angle=" + angle +
                ", speed=" + speed +
                ", duration=" + duration +
                ", relative=" + relative +
                '}';
    }
    
    /**
     * Builder模式
     */
    public static class Builder {
        private String servoId;
        private float angle;
        private int speed = 50;  // 默认速度
        private int duration = 0;
        private boolean relative = false;
        
        public Builder(String servoId) {
            this.servoId = servoId;
        }
        
        public Builder angle(float angle) {
            this.angle = angle;
            return this;
        }
        
        public Builder speed(int speed) {
            this.speed = speed;
            return this;
        }
        
        public Builder duration(int duration) {
            this.duration = duration;
            return this;
        }
        
        public Builder relative(boolean relative) {
            this.relative = relative;
            return this;
        }
        
        public RotationOption build() {
            return new RotationOption(servoId, angle, speed, duration, relative);
        }
    }
}

