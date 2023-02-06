declare module 'rn-update-apk' {
  export interface UpdateAPK {
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
    needUpdateApp?: (
      performUpdate: (isUpdate: boolean) => void,
      whatsNew: string,
    ) => void;
    forceUpdateApp?: () => void;
    notNeedUpdateApp?: () => void;
    downloadApkStart?: () => void;
    downloadApkProgress?: (args: {
      progress: number;
      totalSize: number;
      downloadSize: number;
    }) => void;
    downloadApkEnd?: () => void;
    onError?: (error: Error | string) => void;
    cancelUpdate?: () => void;
  }
  export function getUpdateInstance(options: Options): UpdateAPK;
}
