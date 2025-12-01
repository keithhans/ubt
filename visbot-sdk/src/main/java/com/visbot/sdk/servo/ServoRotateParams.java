package com.visbot.sdk.servo;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * ServoRotateParams - 舵机旋转参数
 * 
 * 这个类用于传递舵机旋转的参数
 */
public class ServoRotateParams implements Parcelable {
    private String servoId;
    private float angle;
    private int speed;
    private int duration;
    private boolean relative;
    
    public ServoRotateParams(String servoId, float angle, int speed, int duration, boolean relative) {
        this.servoId = servoId;
        this.angle = angle;
        this.speed = speed;
        this.duration = duration;
        this.relative = relative;
    }
    
    protected ServoRotateParams(Parcel in) {
        servoId = in.readString();
        angle = in.readFloat();
        speed = in.readInt();
        duration = in.readInt();
        relative = in.readByte() != 0;
    }
    
    public static final Creator<ServoRotateParams> CREATOR = new Creator<ServoRotateParams>() {
        @Override
        public ServoRotateParams createFromParcel(Parcel in) {
            return new ServoRotateParams(in);
        }
        
        @Override
        public ServoRotateParams[] newArray(int size) {
            return new ServoRotateParams[size];
        }
    };
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(servoId);
        dest.writeFloat(angle);
        dest.writeInt(speed);
        dest.writeInt(duration);
        dest.writeByte((byte) (relative ? 1 : 0));
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
}

