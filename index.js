"use strict";

import { NativeModules, Platform, Alert } from "react-native";
import semverLt from "semver/functions/lt";

const RNUpdateAPK = NativeModules.RNUpdateAPK;
const ConfirmUpdateModule = NativeModules.ConfirmUpdateModule;
const ProgressDialogModule = NativeModules.ProgressDialogModule;

let jobId = -1;
let instance = null;

class UpdateAPK {
  constructor(options) {
    this.options = options;
  }

  setOptions(options) {
    this.options = Object.assign(this.options, options || {});
  }

  get = (url, success, error, options = {}) => {
    fetch(url, options)
      .then((response) => {
        if (!response.ok) {
          let message;
          if (response.statusText) {
            message = `${response.url}  ${response.statusText}`;
          } else {
            message = `${response.url} Status Code:${response.status}`;
          }
          throw Error(message);
        }
        return response;
      })
      .then((response) => response.json())
      .then((json) => {
        success && success(json);
      })
      .catch((err) => {
        error && error(err);
      });
  };

  getApkVersion = () => {
    if (jobId !== -1) {
      return;
    }
    if (!this.options.apkVersionUrl) {
      console.log("RNUpdateAPK::getApkVersion - apkVersionUrl doesn't exist.");
      return;
    }
    this.get(
      this.options.apkVersionUrl,
      this.getApkVersionSuccess.bind(this),
      this.getVersionError.bind(this),
      this.options.apkVersionOptions
    );
  };

  getApkVersionSuccess = (remote) => {
    console.debug("RNUpdateAPK::getApkVersionSuccess remote:" + JSON.stringify(remote));

    if (this.options.pgyerVersionHandler) {
      remote = this.options.pgyerVersionHandler(remote);
    }

    console.debug("RNUpdateAPK::getApkVersionSuccess after handler remote:" + JSON.stringify(remote));

    // TODO switch this to versionCode
    let outdated = false;
    if (remote.versionCode && remote.versionCode > RNUpdateAPK.versionCode) {
      console.log(
        "RNUpdateAPK::getApkVersionSuccess - outdated based on code, local/remote: " +
          RNUpdateAPK.versionCode +
          "/" +
          remote.versionCode
      );
      outdated = true;
    }
    if (!remote.versionCode && semverLt(RNUpdateAPK.versionName, remote.versionName)) {
      console.log(
        "RNUpdateAPK::getApkVersionSuccess - APK outdated based on version name, local/remote: " +
          RNUpdateAPK.versionName +
          "/" +
          remote.versionName
      );
      outdated = true;
    }
    if (outdated) {
      if (remote.forceUpdate) {
        if (this.options.forceUpdateApp) {
          this.options.forceUpdateApp();
        }
        this.downloadApk(remote);
      } else if (this.options.needUpdateApp) {
        this.options.needUpdateApp((isUpdate) => {
          if (isUpdate) {
            this.downloadApk(remote);
          }
        }, remote.whatsNew);
      }
    } else if (this.options.notNeedUpdateApp) {
      this.options.notNeedUpdateApp();
    }
  };

  downloadApk = (remote) => {
    const RNFS = require("react-native-fs");
    const progress = (data) => {
      const percentage = ((100 * data.bytesWritten) / data.contentLength) | 0;
      this.options.downloadApkProgress &&
        this.options.downloadApkProgress(percentage, data.contentLength, data.bytesWritten);
    };
    const begin = (res) => {
      console.log("RNUpdateAPK::downloadApk - downloadApkStart");
      this.options.downloadApkStart && this.options.downloadApkStart();
    };
    const progressDivider = 1;
    // You must be sure filepaths.xml exposes this path or you will have a FileProvider error API24+
    // You might check {totalSpace, freeSpace} = await RNFS.getFSInfo() to make sure there is room
    const downloadDestPath = `${RNFS.CachesDirectoryPath}/NewApp.apk`;

    let options = this.options.apkOptions ? this.options.apkOptions : {};

    const ret = RNFS.downloadFile(
      Object.assign(
        {
          fromUrl: remote.apkUrl,
          toFile: downloadDestPath,
          begin,
          progress,
          background: true,
          progressDivider,
        },
        options
      )
    );

    jobId = ret.jobId;

    ret.promise
      .then((res) => {
        if (res["statusCode"] >= 400 && res["statusCode"] <= 599) {
          throw "Failed to Download APK. Server returned with " + res["statusCode"] + " statusCode";
        }
        console.log("RNUpdateAPK::downloadApk - downloadApkEnd");

        this.options.downloadApkEnd && this.options.downloadApkEnd();

        RNUpdateAPK.getApkInfo(downloadDestPath)
          .then((res) => {
            console.log("RNUpdateAPK::downloadApk - Old Cert SHA-256: " + RNUpdateAPK.signatures[0].thumbprint);
            console.log("RNUpdateAPK::downloadApk - New Cert SHA-256: " + res.signatures[0].thumbprint);
            if (res.signatures[0].thumbprint !== RNUpdateAPK.signatures[0].thumbprint) {
              // FIXME should add extra callback for this
              console.log("The signature thumbprints seem unequal. Install will fail");
            }
          })
          .catch((rej) => {
            console.log("RNUpdateAPK::downloadApk - apk info error: ", rej);

            this.options.onError && this.options.onError("Failed to get Downloaded APK Info");
            // re-throw so we don't attempt to install the APK, this will call the downloadApkError handler
            throw rej;
          });
        RNUpdateAPK.installApk(downloadDestPath, this.options.fileProviderAuthority);

        jobId = -1;
      })
      .catch((err) => {
        this.downloadApkError(err);
        jobId = -1;
      });
  };

  getAppStoreVersion = () => {
    if (!this.options.iosAppId) {
      console.log("RNUpdateAPK::getAppStoreVersion - iosAppId doesn't exist.");
      return;
    }
    const URL = "https://itunes.apple.com/lookup?id=" + this.options.iosAppId;
    console.log("RNUpdateAPK::getAppStoreVersion - attempting to fetch " + URL);
    this.get(URL, this.getAppStoreVersionSuccess.bind(this), this.getVersionError.bind(this));
  };

  getAppStoreVersionSuccess = (data) => {
    if (data.resultCount < 1) {
      console.log("RNUpdateAPK::getAppStoreVersionSuccess - iosAppId is wrong.");
      return;
    }
    const result = data.results[0];
    const version = result.version;
    const trackViewUrl = result.trackViewUrl;

    if (semverLt(RNUpdateAPK.versionName, version)) {
      console.log(
        "RNUpdateAPK::getAppStoreVersionSuccess - outdated based on version name, local/remote: " +
          RNUpdateAPK.versionName +
          "/" +
          version
      );
      if (this.options.needUpdateApp) {
        this.options.needUpdateApp((isUpdate) => {
          if (isUpdate) {
            RNUpdateAPK.installFromAppStore(trackViewUrl);
          }
        });
      }
    } else {
      this.options.notNeedUpdateApp && this.options.notNeedUpdateApp();
    }
  };

  getVersionError = (err) => {
    console.log("RNUpdateAPK::getVersionError - getVersionError", err);
    this.options.onError && this.options.onError(err);
  };

  downloadApkError = (err) => {
    console.log("RNUpdateAPK::downloadApkError - downloadApkError", err);
    this.options.onError && this.options.onError(err);
  };

  checkUpdate = () => {
    if (Platform.OS === "android") {
      this.getApkVersion();
    } else {
      this.getAppStoreVersion();
    }
  };
}

/**
 *
 * @param {string} options.iosAppId
 * @param {string} options.apkVersionUrl
 * @param {string} options.fileProviderAuthority
 * @param {function} options.downloadApkProgress
 * @param {function} options.downloadApkEnd
 * @param {function} options.downloadApkStart
 * @param {object?} options.apkVersionOptions
 * @param {function?} options.pgyerVersionHandler
 * @returns
 */
export function getUpdateInstance(options) {
  const mergedOptions = {};
  mergedOptions.notNeedUpdateApp = () => {
    options.notNeedUpdateApp?.();
  };
  mergedOptions.needUpdateApp = (performUpdate, whatsNew) => {
    options.needUpdateApp?.();

    showConfirm("版本升级", whatsNew ? whatsNew : "有新的版本", [
      {
        text: "暂不升级",
        onPress: () => {
          options.cancelUpdate?.();
        },
      },
      // Note, apps can be large. You may want to check if the network is metered (cellular data) to be nice.
      // Note that the user will likely get a popup saying the device is set to block installs from uknown sources.
      // ...you will need to guide them through that, maybe by explaining it here, before you call performUpdate(true);
      {
        text: "立即升级",
        onPress: () => {
          console.log("RNUpdateAPK::needUpdateApp confirmed");
          performUpdate(true);
        },
      },
    ]);
  };
  if (options.forceUpdateApp) {
    mergedOptions.forceUpdateApp = options.forceUpdateApp;
  }
  mergedOptions.downloadApkStart = () => {
    showProgress();

    options.downloadApkStart?.();
  };
  mergedOptions.downloadApkProgress = (progress, contentLength, bytesWritten) => {
    console.log(`downloadApkProgress callback called - ${progress}%...`);
    // This is your opportunity to provide feedback to users on download progress
    // If you hae a state variable it is trivial to update the UI

    ProgressDialogModule.updateProgress({
      progress: progress,
      totalSize: contentLength,
      downloadSize: bytesWritten,
    });

    options.downloadApkProgress?.({
      progress: progress,
      totalSize: contentLength,
      downloadSize: bytesWritten,
    });
  };
  mergedOptions.downloadApkEnd = () => {
    ProgressDialogModule.closeProgress();
    options.downloadApkEnd?.();
  };
  mergedOptions.onError = (error) => {
    options.onError?.(error);
  };

  if (!instance) {
    if (options.fileProviderAuthority) {
      mergedOptions.fileProviderAuthority = options.fileProviderAuthority;
    }
    if (options.iosAppId) {
      mergedOptions.iosAppId = options.iosAppId;
    }
    if (options.apkVersionUrl) {
      mergedOptions.apkVersionUrl = options.apkVersionUrl;
    }
    if (options.pgyerVersionHandler) {
      mergedOptions.pgyerVersionHandler = options.pgyerVersionHandler;
    }
    if (options.apkOptions) {
      mergedOptions.apkOptions = options.apkOptions;
    } else {
      mergedOptions.apkOptions = {
        headers: {},
      };
    }
    if (options.apkVersionOptions) {
      mergedOptions.apkVersionOptions = options.apkVersionOptions;
    } else {
      mergedOptions.apkVersionOptions = {
        method: "GET",
        headers: {},
      };
    }

    instance = new UpdateAPK(mergedOptions);
  } else {
    instance.setOptions(mergedOptions);
  }

  return instance;
}

export function getInstalledVersionName() {
  return RNUpdateAPK.versionName;
}
export function getInstalledVersionCode() {
  return RNUpdateAPK.versionCode;
}
export function getInstalledPackageName() {
  return RNUpdateAPK.packageName;
}
export function getInstalledFirstInstallTime() {
  return RNUpdateAPK.firstInstallTime;
}
export function getInstalledLastUpdateTime() {
  return RNUpdateAPK.lastUpdateTime;
}
export function getInstalledPackageInstaller() {
  return RNUpdateAPK.packageInstaller;
}
export function getInstalledSigningInfo() {
  return RNUpdateAPK.signatures;
}
export async function getApps() {
  if (Platform.OS === "android") {
    return RNUpdateAPK.getApps();
  } else {
    return Promise.resolve([]);
  }
}
export async function getNonSystemApps() {
  if (Platform.OS === "android") {
    return RNUpdateAPK.getNonSystemApps();
  } else {
    return Promise.resolve([]);
  }
}

function showConfirm(title, message, buttons, options) {
  if (Platform.OS === "ios") {
    Alert.prompt(title, message, buttons, "default");
  } else if (Platform.OS === "android") {
    if (!ConfirmUpdateModule) {
      return;
    }
    const constants = ConfirmUpdateModule.getConstants();

    const config = {
      title: title || "",
      message: message || "",
    };

    if (options && options.cancelable) {
      config.cancelable = options.cancelable;
    }
    // At most three buttons (neutral, negative, positive). Ignore rest.
    // The text 'OK' should be probably localized. iOS Alert does that in native.
    const defaultPositiveText = "OK";
    const validButtons = buttons ? buttons.slice(0, 2) : [{ text: defaultPositiveText }];
    const buttonPositive = validButtons.pop();
    const buttonNegative = validButtons.pop();

    if (buttonNegative) {
      config.buttonNegative = buttonNegative.text || "";
    }
    if (buttonPositive) {
      config.buttonPositive = buttonPositive.text || defaultPositiveText;
    }

    const onAction = (action, buttonKey) => {
      if (action === constants.buttonClicked) {
        if (buttonKey === constants.buttonNegative) {
          buttonNegative.onPress && buttonNegative.onPress();
        } else if (buttonKey === constants.buttonPositive) {
          buttonPositive.onPress && buttonPositive.onPress();
        }
      } else if (action === constants.dismissed) {
        options && options.onDismiss && options.onDismiss();
      }
    };
    const onError = (errorMessage) => console.warn(errorMessage);

    console.debug("RNUpdateAPK::showConfirm 请求打开更新确认对话框");
    ConfirmUpdateModule.showConfirm(config, onError, onAction);
  }
}

function showProgress() {
  if (Platform.OS === "ios") {
    return;
  } else if (Platform.OS === "android") {
    if (!ProgressDialogModule) {
      return;
    }

    const onError = (errorMessage) => console.warn(errorMessage);
    ProgressDialogModule.showProgress({}, onError);
  }
}
