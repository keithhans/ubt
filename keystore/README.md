# 系统签名文件说明

## 文件列表

- `platform.pk8` - AOSP系统私钥（DER格式）
- `platform.x509.pem` - AOSP系统证书（PEM格式）
- `platform.p12` - PKCS12格式（中间文件）
- `platform.jks` - Java Keystore格式（用于Android Studio）

## 签名信息

- **Keystore文件**: `platform.jks`
- **Keystore密码**: `android`
- **Key别名**: `platform`
- **Key密码**: `android`

## 证书信息

```
所有者: EMAILADDRESS=android@android.com, CN=Android, OU=Android, O=Android, L=Mountain View, ST=California, C=US
SHA256: C8:A2:E9:BC:CF:59:7C:2F:B6:DC:66:BE:E2:93:FC:13:F2:FC:47:EC:77:BC:6B:2B:0D:52:C1:1F:51:19:2A:B8
```

## 使用说明

这些签名文件已经在 `app/build.gradle` 中配置好了。

现在你可以直接在Android Studio中：
1. 点击 Run 按钮运行应用
2. 应用会自动使用系统签名
3. 可以成功安装到优必选小方头机器人上

## 重要提示

⚠️ **这是AOSP的测试签名，仅适用于使用默认AOSP签名的设备！**

- 适用于：优必选小方头机器人（已验证）
- 不适用于：商业Android手机、平板等

## 验证签名

安装APK后，可以使用以下命令验证签名：

```bash
adb shell pm list packages -s | grep visbotclient
keytool -printcert -jarfile app-debug.apk
```

## 安全说明

这些签名文件是公开的AOSP测试签名，不应用于生产环境。
仅用于开发和测试目的。

