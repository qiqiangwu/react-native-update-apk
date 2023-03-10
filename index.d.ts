declare module "@evm/rn-update-apk" {
  interface IApkVersion {
    versionName: string;
    versionCode: string;
    apkUrl: string;
    forceUpdate: boolean;
    whatsNew: string;
  }
  export interface UpdateAPK {
    apkVersion?: IApkVersion;
    checkUpdate: () => void;
    options: Options;
  }
  export interface Options {
    iosAppId: string;
    apkVersionUrl: string;
    apkVersionOptions?: RequestInit;
    apkOptions?: RequestInit;
    fileProviderAuthority: string;
    pgyerVersionHandler: (remote: any) => any;
    needUpdateApp?: (remoteVersionCode: string) => void;
    forceUpdateApp?: () => void;
    notNeedUpdateApp?: () => void;
    downloadApkStart?: () => void;
    downloadApkProgress?: (args: { progress: number; totalSize: number; downloadSize: number }) => void;
    downloadApkEnd?: () => void;
    onError?: (error: Error | string) => void;
    cancelUpdate?: () => void;
  }
  export function getUpdateInstance(options: Options): UpdateAPK;
  export function getInstalledPackageName(): string;
  export function getInstalledVersionCode(): string;
}
