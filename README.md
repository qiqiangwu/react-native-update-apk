# React Native Update APK

Fork from <https://github.com/mikehardy/react-native-update-apk>

## 安装

### 安卓需要手动处理

1. FileProviders

AndroidManifest.xml application 节点下添加

```
<!-- Define a FileProvider for API24+ -->
<!-- note this is the authority name used by other modules like rn-fetch-blob, easy to have conflicts -->
<provider
  android:name="androidx.core.content.FileProvider"
  android:authorities="${applicationId}.provider"
  android:exported="false"
  android:grantUriPermissions="true">
  <!-- you might need the tools:replace thing to workaround rn-fetch-blob or other definitions of provider -->
  <!-- just make sure if you "replace" here that you include all the paths you are replacing *plus* the cache path we use -->
  <meta-data tools:replace="android:resource"
    android:name="android.support.FILE_PROVIDER_PATHS"
    android:resource="@xml/filepaths" />
</provider>
```

res/xml/filepaths.xml

```
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
  <!-- Select one of the following based on your apk location -->

  <!-- cache dir is always available and requires no permissions, but space may be limited -->
  <cache-path name="cache" path="/" />
  <!-- <files-path name="name" path="/" />  -->

  <!-- External cache dir is maybe user-friendly for downloaded APKs, but you must be careful. -->
  <!-- 1) in API <19 (KitKat) this requires WRITE_EXTERNAL_STORAGE permission. >=19, no permission -->
  <!-- 2) this directory may not be available, check Environment.isExternalStorageEmulated(file) to see -->
  <!-- 3) there may be no beneifit versus cache-path if external storage is emulated. Check Environment.isExternalStorageEmulated(File) to verify -->
  <!-- 4) the path will change for each app 'com.example' must be replaced by your application package -->
  <!-- <external-cache-path name="external-cache" path="/data/user/0/com.example/cache" /> -->

  <!-- Note that these external paths require WRITE_EXTERNAL_STORAGE permission -->
  <!-- <external-path name="some_external_path" path="put-your-specific-external-path-here" />  -->
  <!-- <external-files-path name="external-files" path="/data/user/0/com.example/cache" />  -->
  <!-- <external-media-path name="external-media" path="put-your-path-to-media-here" />  -->
</paths>

```

2. 添加权限

```
<!-- use permission REQUEST_INSTALL_PACKAGES for target API25+ -->
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>
```

## Tasks

- android 更新对接蒲公英分发平台
- 下载进度条 UI 采用原生布局
- 下载确认框采用原生布局
- 更新托底方案
