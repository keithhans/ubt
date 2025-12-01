package com.ubtrobot.master.transport.message.parcel;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * SimpleParcelRequest - 简化的请求对象
 * 
 * 包含path和params，用于Master服务通信
 * 必须使用正确的包名com.ubtrobot.master.transport.message.parcel
 */
public class SimpleParcelRequest implements Parcelable {
    private String path;
    private String params;
    
    public SimpleParcelRequest(String path, String params) {
        this.path = path;
        this.params = params;
    }
    
    protected SimpleParcelRequest(Parcel in) {
        path = in.readString();
        params = in.readString();
    }
    
    public String getPath() {
        return path;
    }
    
    public String getParams() {
        return params;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeString(params);
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    public static final Creator<SimpleParcelRequest> CREATOR = new Creator<SimpleParcelRequest>() {
        @Override
        public SimpleParcelRequest createFromParcel(Parcel in) {
            return new SimpleParcelRequest(in);
        }
        
        @Override
        public SimpleParcelRequest[] newArray(int size) {
            return new SimpleParcelRequest[size];
        }
    };
    
    @Override
    public String toString() {
        return "SimpleParcelRequest{path='" + path + "', params='" + params + "'}";
    }
}

