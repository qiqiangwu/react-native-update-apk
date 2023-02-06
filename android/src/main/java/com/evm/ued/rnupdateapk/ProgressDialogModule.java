package com.evm.ued.rnupdateapk;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.SoftAssertions;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.dialog.DialogModule;

import java.util.Map;

@ReactModule(name = ProgressDialogModule.NAME)
public class ProgressDialogModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
    public static final String NAME = "ProgressDialogModule";

    /* package */ static String KEY_PROGRESS = "progress";
    /* package */ static String KEY_DOWNLOAD_SIZE = "downloadSize";
    /* package */ static String KEY_TOTAL_SIZE = "totalSize";
    /* package */ static final String KEY_TITLE = "title";
    /* package */ static final String KEY_TIP = "tip";

    private boolean mIsInForeground;

    public ProgressDialogModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    class FragmentManagerHelper {
        private final @NonNull
        FragmentManager mFragmentManager;
        private @Nullable
        Object mFragmentToShow;


        public FragmentManagerHelper(@NonNull FragmentManager fragmentManager) {
            mFragmentManager = fragmentManager;
        }

        public void showPendingConfirm() {
            UiThreadUtil.assertOnUiThread();
            SoftAssertions.assertCondition(mIsInForeground, "showPendingAlert() called in background");
            if (mFragmentToShow == null) {
                return;
            }

            dismissExisting();
            ConfirmUpdateDialog fragment = (ConfirmUpdateDialog) mFragmentToShow;
            fragment.show(mFragmentManager, ConfirmUpdateDialog.FRAGMENT_TAG);

            mFragmentToShow = null;
        }

        public void dismissExisting() {
            if (!mIsInForeground) {
                return;
            }

            ProgressDialog oldFragment = (ProgressDialog) mFragmentManager.findFragmentByTag(ProgressDialog.FRAGMENT_TAG);
            if (oldFragment != null && oldFragment.isResumed()) {
                oldFragment.dismiss();
            }
        }

        public void showNewConfirm(Bundle args) {
            Log.d(NAME, "showNewConfirm() mIsInForeground:" + mIsInForeground);
            UiThreadUtil.assertOnUiThread();

            dismissExisting();

            ProgressDialog fragment = new ProgressDialog(args);

            if (mIsInForeground && !mFragmentManager.isStateSaved()) {
                fragment.show(mFragmentManager, ProgressDialog.FRAGMENT_TAG);
            } else {
                mFragmentToShow = fragment;
            }
        }

        public void onProgress(Bundle args) {
            ProgressDialog fragment = (ProgressDialog) mFragmentManager.findFragmentByTag(ProgressDialog.FRAGMENT_TAG);
            if (fragment != null && fragment.isResumed()) {
                ProgressDialog.onProgress(fragment, args);
            }

        }
    }

    @ReactMethod
    public void showProgress(ReadableMap options, Callback errorCallback) {
        Log.d(NAME, "showProgress() options: " + options.toString());

        ProgressDialogModule.FragmentManagerHelper fragmentManagerHelper = getFragmentManagerHelper();
        if (fragmentManagerHelper == null) {
            errorCallback.invoke("Tried to show an alert while not attached to an Activity");
            return;
        }

        final Bundle args = new Bundle();
        if (options.hasKey(KEY_TITLE)) {
            args.putString(ConfirmUpdateDialog.ARG_TITLE, options.getString(KEY_TITLE));
        }
        if (options.hasKey(KEY_TIP)) {
            args.putString(ConfirmUpdateDialog.ARG_MESSAGE, options.getString(KEY_TIP));
        }

        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragmentManagerHelper.showNewConfirm(args);
            }
        });
    }

    @ReactMethod
    public void updateProgress(ReadableMap options) {
        ProgressDialogModule.FragmentManagerHelper fragmentManagerHelper = getFragmentManagerHelper();
        if (fragmentManagerHelper == null) {
            return;
        }

        final Bundle args = new Bundle();
        if (options.hasKey(KEY_PROGRESS)) {
            args.putInt(ProgressDialog.ARG_PROGRESS, options.getInt(KEY_PROGRESS));
        }
        if (options.hasKey(KEY_TOTAL_SIZE)) {
            args.putDouble(ProgressDialog.ARG_TOTAL_SIZE, options.getDouble(KEY_TOTAL_SIZE));
        }
        if (options.hasKey(KEY_DOWNLOAD_SIZE)) {
            args.putDouble(ProgressDialog.ARG_DOWNLOAD_SIZE, options.getDouble(KEY_DOWNLOAD_SIZE));
        }

        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragmentManagerHelper.onProgress(args);
            }
        });
    }

    @ReactMethod
    public void closeProgress() {
        ProgressDialogModule.FragmentManagerHelper fragmentManagerHelper = getFragmentManagerHelper();
        if (fragmentManagerHelper == null) {
            return;
        }

        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragmentManagerHelper.dismissExisting();
            }
        });
    }

    @Override
    public void initialize() {
        getReactApplicationContext().addLifecycleEventListener(this);
    }

    @Override
    public void onHostResume() {
        mIsInForeground = true;

        Log.d(NAME, "onHostResume()");

        // Check if a dialog has been created while the host was paused, so that we can show it now.
        ProgressDialogModule.FragmentManagerHelper fragmentManagerHelper = getFragmentManagerHelper();
        if (fragmentManagerHelper != null) {
            fragmentManagerHelper.showPendingConfirm();
        } else {
            FLog.w(DialogModule.class, "onHostResume called but no FragmentManager found");
        }
    }

    @Override
    public void onHostPause() {
        Log.d(NAME, "onHostPause()");

        mIsInForeground = false;
    }

    @Override
    public void onHostDestroy() {

    }

    /**
     * Creates a new helper to work with FragmentManager. Returns null if we're not attached to an
     * Activity.
     *
     * <p>DO NOT HOLD LONG-LIVED REFERENCES TO THE OBJECT RETURNED BY THIS METHOD, AS THIS WILL CAUSE
     * MEMORY LEAKS.
     */
    private @Nullable
    ProgressDialogModule.FragmentManagerHelper getFragmentManagerHelper() {
        Activity activity = getCurrentActivity();
        if (activity == null || !(activity instanceof FragmentActivity)) {
            Log.d(NAME, "getFragmentManagerHelper: " + "current activity is null or not instance of FragmentActivity");
            return null;
        }
        return new ProgressDialogModule.FragmentManagerHelper(((FragmentActivity) activity).getSupportFragmentManager());
    }
}
