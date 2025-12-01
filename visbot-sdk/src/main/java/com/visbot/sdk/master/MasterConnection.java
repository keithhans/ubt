package com.visbot.sdk.master;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import com.ubtrobot.competition.CompetitionSessionInfo;
import com.ubtrobot.master.transport.message.parcel.ParcelMessage;
import com.ubtrobot.master.transport.message.parcel.SimpleParcelRequest;

/**
 * Master服务连接类
 * 
 * 通过ContentProvider + Binder方式与Master服务通信
 * 
 * 通信流程:
 * 1. connect() - 通过ContentProvider获取Binder
 * 2. call() - 通过Binder.transact()发送请求
 * 3. disconnect() - 断开连接
 */
public class MasterConnection {
    private static final String TAG = "MasterConnection";
    
    // ContentProvider配置
    private static final String AUTHORITY = "com.ubtrobot.provider.master";
    private static final Uri PROVIDER_URI = Uri.parse("content://" + AUTHORITY);
    
    // Provider方法名
    private static final String METHOD_CONNECT = "connect";
    
    // Bundle键名
    private static final String KEY_VERSION = "version";
    private static final String KEY_PACKAGE = "package";
    private static final String KEY_CODE = "code";
    private static final String KEY_ERROR_MESSAGE = "error_message";
    private static final String KEY_BINDER = "binder";
    
    // Binder事务代码
    private static final int TRANS_CODE_WRITE = 0x57524954;      // "WRIT"
    private static final int TRANS_CODE_DISCONNECT = 0x4453434e; // "DSCN"
    
    // 返回码
    private static final int CODE_SUCCESS = 0;
    private static final int CODE_UNSUPPORTED_METHOD = 1;
    private static final int BAD_CALL = 2;
    
    // 版本号 (从Version.smali中发现: LIST = {"v1"}, LATEST = "v1")
    private static final String VERSION = "v1";
    
    private final Context context;
    private IBinder binder;
    private IBinder clientBinder;  // 保存客户端Binder用于后续调用
    private boolean connected = false;
    
    public MasterConnection(Context context) {
        this.context = context;
    }
    
    /**
     * 连接到Master服务
     * 
     * @return true if connected successfully
     */
    public boolean connect() {
        if (connected && binder != null) {
            Log.i(TAG, "Already connected");
            return true;
        }

        Log.i(TAG, "Connecting to Master service...");

        // 创建客户端Binder用于接收Master服务的回调
        this.clientBinder = new android.os.Binder();

        // 准备连接参数
        Bundle args = new Bundle();
        args.putString(KEY_VERSION, VERSION);
        args.putString(KEY_PACKAGE, context.getPackageName());
        args.putBinder(KEY_BINDER, clientBinder);  // 提供客户端Binder
        
        try {
            // 调用ContentProvider的connect方法
            Bundle result = context.getContentResolver().call(
                PROVIDER_URI,
                METHOD_CONNECT,
                null,  // arg参数不使用
                args   // extras参数
            );
            
            if (result == null) {
                Log.e(TAG, "Provider returned null");
                return false;
            }
            
            // 检查返回码
            int code = result.getInt(KEY_CODE, -1);
            Log.i(TAG, "Provider returned code: " + code);
            
            if (code == CODE_SUCCESS) {
                // 获取Binder
                binder = result.getBinder(KEY_BINDER);
                
                if (binder != null) {
                    connected = true;
                    Log.i(TAG, "✓ Connected to Master service successfully");
                    Log.i(TAG, "Binder: " + binder);
                    return true;
                } else {
                    Log.e(TAG, "Binder is null in result");
                    return false;
                }
            } else {
                String errorMsg = result.getString(KEY_ERROR_MESSAGE, "Unknown error");
                Log.e(TAG, "Connect failed with code " + code + ": " + errorMsg);
                return false;
            }
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception. Missing permission?", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to Master service", e);
            return false;
        }
    }
    
    /**
     * 调用Master服务（不带会话信息）
     *
     * @param path 服务路径，如 "/servo/rotate"
     * @param paramsJson JSON格式的参数
     * @return 响应字符串，失败返回null
     */
    public String call(String path, String paramsJson) {
        return call(path, paramsJson, null);
    }

    /**
     * 调用Master服务（使用Parcelable参数）
     *
     * @param path 服务路径，如 "/servo/rotate"
     * @param param Parcelable参数对象
     * @param sessionInfo 会话信息，可以为null
     * @return 响应字符串，失败返回null
     */
    public String callWithParcelable(String path, android.os.Parcelable param, CompetitionSessionInfo sessionInfo) {
        if (!connected || binder == null) {
            Log.e(TAG, "Not connected. Call connect() first.");
            return null;
        }

        Log.i(TAG, "=== MasterConnection.callWithParcelable() START ===");
        Log.i(TAG, "Calling Master service: " + path);
        Log.i(TAG, "Param type: " + (param != null ? param.getClass().getName() : "null"));

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();

        try {
            // 第一个参数必须是客户端Binder
            data.writeStrongBinder(clientBinder);

            // 从路径中提取服务名称
            // 路径格式: /servo/rotate 或 /locomotor/locomote
            String serviceName = "servo"; // 默认值
            String servicePackage = "com.ubtrobot.servo"; // 默认值

            if (path != null && path.startsWith("/")) {
                String[] parts = path.substring(1).split("/");
                if (parts.length > 0) {
                    serviceName = parts[0];
                    // 根据服务名称确定包名
                    if ("locomotor".equals(serviceName)) {
                        servicePackage = "com.ubtrobot.locomotion";
                    } else if ("servo".equals(serviceName)) {
                        servicePackage = "com.ubtrobot.servo";
                    } else {
                        // 其他服务使用通用格式
                        servicePackage = "com.ubtrobot." + serviceName;
                    }
                }
            }

            Log.i(TAG, "Extracted service name: " + serviceName + ", package: " + servicePackage);

            // 创建请求上下文
            com.ubtrobot.master.transport.message.parcel.ParcelRequestContext.Builder builder =
                new com.ubtrobot.master.transport.message.parcel.ParcelRequestContext.Builder(
                    com.ubtrobot.master.transport.message.parcel.ParcelRequestContext.RESPONDER_TYPE_SERVICE
                );

            // 使用反射设置responder字段
            try {
                java.lang.reflect.Field responderField = builder.getClass().getDeclaredField("responder");
                responderField.setAccessible(true);
                responderField.set(builder, serviceName);
                Log.i(TAG, "Set responder to: " + serviceName);
            } catch (Exception e) {
                Log.e(TAG, "Failed to set responder field via reflection", e);
            }

            // 构建上下文
            builder.setRequester("com.example.visbotclient")
                    .setRequesterType(com.ubtrobot.master.transport.message.parcel.ParcelRequestContext.REQUESTER_TYPE_SERVICE)
                    .setResponderPackage(servicePackage);

            if (sessionInfo != null) {
                Log.i(TAG, "Setting competing session: " + sessionInfo.getSessionId());
                builder.setCompetingSession(sessionInfo);
            }

            com.ubtrobot.master.transport.message.parcel.ParcelRequestContext context = builder.build();

            // 创建请求配置
            com.ubtrobot.master.transport.message.parcel.ParcelRequestConfig config =
                new com.ubtrobot.master.transport.message.parcel.ParcelRequestConfig.Builder()
                    .setHasCallback(false)
                    .setStickily(false)
                    .setTimeout(30000)
                    .setCancelPrevious(false)
                    .setPreviousRequestId(null)
                    .build();

            // 创建ParcelableParam - 包装Parcelable对象
            com.ubtrobot.master.transport.message.parcel.ParcelableParam parcelableParam =
                com.ubtrobot.master.transport.message.parcel.ParcelableParam.create(param);
            Log.i(TAG, "Created ParcelableParam");

            // 创建ParcelRequest
            com.ubtrobot.master.transport.message.parcel.ParcelRequest request =
                new com.ubtrobot.master.transport.message.parcel.ParcelRequest(context, config, path, parcelableParam);

            // 创建ParcelMessage包装request
            ParcelMessage message = new ParcelMessage(request);

            // 写入ParcelMessage
            data.writeParcelable(message, 0);

            Log.i(TAG, "Parcel data size: " + data.dataSize() + " bytes");
            Log.i(TAG, "Sending Binder transact...");

            // 发送Binder事务
            boolean success = binder.transact(TRANS_CODE_WRITE, data, reply, 0);

            Log.i(TAG, "Binder transact returned: " + success);

            if (success) {
                // 读取响应
                Log.i(TAG, "Reading reply...");

                try {
                    reply.readException();
                    Log.i(TAG, "No exception in reply");
                } catch (Exception e) {
                    Log.e(TAG, "Exception in reply", e);
                    return null;
                }

                // 读取响应字符串（可能为 null，因为响应是异步的）
                String response = reply.readString();
                Log.i(TAG, "Response: " + response);
                Log.i(TAG, "=== MasterConnection.callWithParcelable() END ===");

                // 如果 transact 成功且没有异常，返回 "success" 表示请求已发送
                // 实际的响应会通过异步回调返回
                return "success";
            } else {
                Log.e(TAG, "Binder transact failed");
                return null;
            }

        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during call", e);
            return null;
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    /**
     * 调用Master服务（带会话信息）
     *
     * @param path 服务路径，如 "/servo/rotate"
     * @param paramsJson JSON格式的参数
     * @param sessionInfo 会话信息，可以为null
     * @return 响应字符串，失败返回null
     */
    public String call(String path, String paramsJson, CompetitionSessionInfo sessionInfo) {
        if (!connected || binder == null) {
            Log.e(TAG, "Not connected. Call connect() first.");
            return null;
        }

        Log.i(TAG, "=== MasterConnection.call() START ===");
        Log.i(TAG, "Calling Master service: " + path);
        Log.i(TAG, "Params: " + paramsJson);

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();

        try {
            // 写入请求数据
            Log.i(TAG, "Writing to Parcel - path: " + path);
            Log.i(TAG, "Writing to Parcel - params: " + paramsJson);

            // 第一个参数必须是客户端Binder（从MasterSideBinder.onTransact看到）
            data.writeStrongBinder(clientBinder);

            // 创建请求上下文 - 使用rosa.jar中的SDK类
            // 使用单参数构造函数 Builder(String responderType)
            com.ubtrobot.master.transport.message.parcel.ParcelRequestContext.Builder builder =
                new com.ubtrobot.master.transport.message.parcel.ParcelRequestContext.Builder(
                    com.ubtrobot.master.transport.message.parcel.ParcelRequestContext.RESPONDER_TYPE_SERVICE
                );

            // 使用反射设置responder字段
            try {
                java.lang.reflect.Field responderField = builder.getClass().getDeclaredField("responder");
                responderField.setAccessible(true);
                responderField.set(builder, "servo");
            } catch (Exception e) {
                Log.e(TAG, "Failed to set responder field via reflection", e);
            }

            // 构建上下文 - 只有在 sessionInfo 不为 null 时才设置会话
            builder.setRequester("com.example.visbotclient")  // 请求者包名
                    .setRequesterType(com.ubtrobot.master.transport.message.parcel.ParcelRequestContext.REQUESTER_TYPE_SERVICE)  // 使用服务类型
                    .setResponderPackage("com.ubtrobot.servo");  // 响应者包名

            // 只有在 sessionInfo 不为 null 时才设置会话
            if (sessionInfo != null) {
                Log.i(TAG, "Setting competing session: " + sessionInfo.getSessionId());
                builder.setCompetingSession(sessionInfo);
            } else {
                Log.i(TAG, "No session info - calling without session");
            }

            com.ubtrobot.master.transport.message.parcel.ParcelRequestContext context = builder.build();

            // 创建请求配置 - 根据Master服务的字段要求
            com.ubtrobot.master.transport.message.parcel.ParcelRequestConfig config =
                new com.ubtrobot.master.transport.message.parcel.ParcelRequestConfig.Builder()
                    .setHasCallback(false)
                    .setStickily(false)
                    .setTimeout(30000)
                    .setCancelPrevious(false)
                    .setPreviousRequestId(null)
                    .build();

            // 创建JSON参数
            // 恢复使用 JsonParam - ParcelableParam 存在跨进程 ClassLoader 问题
            com.ubtrobot.master.transport.message.parcel.JsonParam param =
                new com.ubtrobot.master.transport.message.parcel.JsonParam(paramsJson);
            Log.i(TAG, "Created JsonParam with JSON: " + paramsJson);

            // 创建ParcelRequest
            com.ubtrobot.master.transport.message.parcel.ParcelRequest request =
                new com.ubtrobot.master.transport.message.parcel.ParcelRequest(context, config, path, param);

            // 创建ParcelMessage包装request
            ParcelMessage message = new ParcelMessage(request);

            // 写入ParcelMessage - 使用writeParcelable而不是直接调用writeToParcel
            // 因为Master服务使用readParcelable读取
            data.writeParcelable(message, 0);

            // 打印 Parcel 数据大小
            Log.i(TAG, "Parcel data size: " + data.dataSize() + " bytes");
            Log.i(TAG, "Parcel data position: " + data.dataPosition());

            Log.i(TAG, "Sending Binder transact with code: 0x" + Integer.toHexString(TRANS_CODE_WRITE));

            // 发送Binder事务
            boolean success = binder.transact(TRANS_CODE_WRITE, data, reply, 0);

            Log.i(TAG, "Binder transact returned: " + success);

            if (success) {
                // 读取响应
                Log.i(TAG, "Reading reply...");
                Log.i(TAG, "Reply data size: " + reply.dataSize());
                Log.i(TAG, "Reply data position: " + reply.dataPosition());

                // 检查是否有异常
                try {
                    reply.readException();
                    Log.i(TAG, "No exception in reply");
                } catch (Exception e) {
                    Log.e(TAG, "Exception from Master service: " + e.getMessage(), e);
                    throw new RuntimeException("Master service error: " + e.getMessage(), e);
                }

                // 尝试读取响应 - Master服务返回的是ParcelResponse
                if (reply.dataSize() > 0 && reply.dataPosition() < reply.dataSize()) {
                    try {
                        // 打印原始字节数据
                        reply.setDataPosition(0);
                        byte[] rawBytes = reply.marshall();
                        Log.i(TAG, "=== Raw Parcel bytes (first 140) ===");
                        StringBuilder hex = new StringBuilder();
                        for (int i = 0; i < Math.min(140, rawBytes.length); i++) {
                            hex.append(String.format("%02X ", rawBytes[i]));
                            if ((i + 1) % 16 == 0) {
                                Log.i(TAG, hex.toString());
                                hex = new StringBuilder();
                            }
                        }
                        if (hex.length() > 0) {
                            Log.i(TAG, hex.toString());
                        }

                        // 重新创建 Parcel 从字节数据
                        reply.setDataPosition(0);

                        // 尝试读取ParcelResponse
                        com.ubtrobot.master.transport.message.parcel.ParcelResponse response =
                            com.ubtrobot.master.transport.message.parcel.ParcelResponse.CREATOR.createFromParcel(reply);

                        if (response != null) {
                            Log.i(TAG, "ParcelResponse read successfully");
                            Log.i(TAG, "ParcelResponse: " + response.toString());
                            Log.i(TAG, "Result type: " + response.getResultType());
                            Log.i(TAG, "Code: " + response.getCode());
                            Log.i(TAG, "Response message: " + response.getMessage());

                            // 返回响应的JSON表示（简化处理）
                            String result = "{\"resultType\":\"" + response.getResultType() +
                                           "\",\"code\":" + response.getCode() +
                                           ",\"message\":\"" + response.getMessage() + "\"}";

                            Log.i(TAG, "=== MasterConnection.call() END (success) ===");
                            return result;
                        } else {
                            Log.w(TAG, "ParcelResponse is null");
                            return null;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to read ParcelResponse", e);
                        return null;
                    }
                } else {
                    Log.i(TAG, "Empty reply");
                    Log.i(TAG, "=== MasterConnection.call() END (empty) ===");
                    return "";
                }
            } else {
                Log.e(TAG, "Binder transact returned false");
                Log.e(TAG, "=== MasterConnection.call() END (failed) ===");
                return null;
            }

        } catch (RemoteException e) {
            Log.e(TAG, "Remote exception during call", e);
            // Binder可能已死亡
            connected = false;
            binder = null;
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Exception during call", e);
            return null;
        } finally {
            data.recycle();
            reply.recycle();
        }
    }
    
    /**
     * 断开与Master服务的连接
     */
    public void disconnect() {
        if (binder != null) {
            Log.i(TAG, "Disconnecting from Master service...");
            
            Parcel data = Parcel.obtain();
            try {
                // 发送断开连接事务（单向调用）
                binder.transact(TRANS_CODE_DISCONNECT, data, null, IBinder.FLAG_ONEWAY);
                Log.i(TAG, "Disconnected");
            } catch (RemoteException e) {
                Log.w(TAG, "Remote exception during disconnect", e);
            } catch (Exception e) {
                Log.w(TAG, "Exception during disconnect", e);
            } finally {
                data.recycle();
            }
            
            binder = null;
        }
        
        connected = false;
    }
    
    /**
     * 检查是否已连接
     * 
     * @return true if connected
     */
    public boolean isConnected() {
        return connected && binder != null;
    }
    
    /**
     * 获取Binder对象（用于高级用法）
     * 
     * @return IBinder对象，未连接时返回null
     */
    public IBinder getBinder() {
        return binder;
    }
}

