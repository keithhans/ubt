package com.visbot.sdk.servo;

/**
 * 舵机设备信息
 */
public class ServoDevice {
    private String id;              // 舵机ID
    private String name;            // 舵机名称
    private float minAngle;         // 最小角度
    private float maxAngle;         // 最大角度
    private float currentAngle;     // 当前角度
    private boolean isRotating;     // 是否正在旋转
    private boolean isReleased;     // 是否已释放
    
    public ServoDevice(String id, String name) {
        this.id = id;
        this.name = name;
        this.minAngle = -90.0f;
        this.maxAngle = 90.0f;
        this.currentAngle = 0.0f;
        this.isRotating = false;
        this.isReleased = false;
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public float getMinAngle() {
        return minAngle;
    }
    
    public float getMaxAngle() {
        return maxAngle;
    }
    
    public float getCurrentAngle() {
        return currentAngle;
    }
    
    public boolean isRotating() {
        return isRotating;
    }
    
    public boolean isReleased() {
        return isReleased;
    }
    
    // Setters
    public void setId(String id) {
        this.id = id;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setMinAngle(float minAngle) {
        this.minAngle = minAngle;
    }
    
    public void setMaxAngle(float maxAngle) {
        this.maxAngle = maxAngle;
    }
    
    public void setCurrentAngle(float currentAngle) {
        this.currentAngle = currentAngle;
    }
    
    public void setRotating(boolean rotating) {
        isRotating = rotating;
    }
    
    public void setReleased(boolean released) {
        isReleased = released;
    }
    
    @Override
    public String toString() {
        return "ServoDevice{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", currentAngle=" + currentAngle +
                ", isRotating=" + isRotating +
                ", isReleased=" + isReleased +
                '}';
    }
}

