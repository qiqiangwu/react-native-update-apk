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

@ReactModule(name = ConfirmUpdateModule.NAME)
public class ConfirmUpdateModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
    public static final String NAME = "ConfirmUpdateModule";

    /* package */ static final String ACTION_BUTTON_CLICKED = "buttonClicked";
    /* package */ static final String ACTION_DISMISSED = "dismissed";
    /* package */ static final String KEY_TITLE = "title";
    /* package */ static final String KEY_MESSAGE = "message";
    /* package */ static final String KEY_BUTTON_POSITIVE = "buttonPositive";
    /* package */ static final String KEY_BUTTON_NEGATIVE = "buttonNegative";
    /* package */ static final String KEY_CANCELABLE = "cancelable";

    /* package */ static final Map<String, Object> CONSTANTS = MapBuilder.<String, Object>of(
            ACTION_BUTTON_CLICKED, ACTION_BUTTON_CLICKED,
            ACTION_DISMISSED, ACTION_DISMISSED,
            KEY_BUTTON_POSITIVE, DialogInterface.BUTTON_POSITIVE,
            KEY_BUTTON_NEGATIVE, DialogInterface.BUTTON_NEGATIVE);

    private boolean mIsInForeground;

    public ConfirmUpdateModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    @Nullable
    @Override
    public Map<String, Object> getConstants() {
        return CONSTANTS;
    }

    class ConfirmUpdateDialogListener implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
        private final Callback mCallback;
        private boolean mCallbackConsumed = false;

        public ConfirmUpdateDialogListener(Callback callback) {
            mCallback = callback;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (!mCallbackConsumed) {
                if (getReactApplicationContext().hasCurrentActivity()) {
                    mCallback.invoke(ACTION_BUTTON_CLICKED, i);
                    mCallbackConsumed = true;
                }
            }
        }

        @Override
        public void onDismiss(DialogInterface dialogInterface) {
            if (!mCallbackConsumed) {
                if (getReactApplicationContext().hasCurrentActivity()) {
                    mCallback.invoke(ACTION_DISMISSED);
                    mCallbackConsumed = true;
                }
            }
        }
    }

    class FragmentManagerHelper {
        private final @NonNull FragmentManager mFragmentManager;
        private @Nullable Object mFragmentToShow;

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

            ConfirmUpdateDialog oldFragment = (ConfirmUpdateDialog) mFragmentManager
                    .findFragmentByTag(ConfirmUpdateDialog.FRAGMENT_TAG);
            if (oldFragment != null && oldFragment.isResumed()) {
                oldFragment.dismiss();
            }
        }

        public void showNewConfirm(Bundle args, Callback actionCallback) {
            Log.d(NAME, "showNewConfirm() mIsInForeground:" + mIsInForeground);
            UiThreadUtil.assertOnUiThread();

            dismissExisting();

            ConfirmUpdateDialogListener actionListener = actionCallback != null
                    ? new ConfirmUpdateDialogListener(actionCallback)
                    : null;
            ConfirmUpdateDialog fragment = new ConfirmUpdateDialog(actionListener, args);

            if (mIsInForeground && !mFragmentManager.isStateSaved()) {
                fragment.setCancelable(false);
                fragment.show(mFragmentManager, ConfirmUpdateDialog.FRAGMENT_TAG);
            } else {
                mFragmentToShow = fragment;
            }
        }
    }

    @ReactMethod
    public void showConfirm(ReadableMap options, Callback errorCallback, final Callback actionCallback) {
        Log.d(NAME, "showConfirm() options: " + options.toString());

        FragmentManagerHelper fragmentManagerHelper = getFragmentManagerHelper();
        if (fragmentManagerHelper == null) {
            errorCallback.invoke("Tried to show an alert while not attached to an Activity");
            return;
        }

        final Bundle args = new Bundle();
        if (options.hasKey(KEY_TITLE)) {
            args.putString(ConfirmUpdateDialog.ARG_TITLE, options.getString(KEY_TITLE));
        }
        if (options.hasKey(KEY_MESSAGE)) {
            args.putString(ConfirmUpdateDialog.ARG_MESSAGE, options.getString(KEY_MESSAGE));
        }

        if (options.hasKey(KEY_BUTTON_POSITIVE)) {
            args.putString(ConfirmUpdateDialog.ARG_BUTTON_POSITIVE, options.getString((KEY_BUTTON_POSITIVE)));
        }
        if (options.hasKey(KEY_BUTTON_NEGATIVE)) {
            args.putString(ConfirmUpdateDialog.ARG_BUTTON_NEGATIVE, options.getString((KEY_BUTTON_NEGATIVE)));
        }

        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragmentManagerHelper.showNewConfirm(args, actionCallback);
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

        // Check if a dialog has been created while the host was paused, so that we can
        // show it now.
        ConfirmUpdateModule.FragmentManagerHelper fragmentManagerHelper = getFragmentManagerHelper();
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
     * Creates a new helper to work with FragmentManager. Returns null if we're not
     * attached to an
     * Activity.
     *
     * <p>
     * DO NOT HOLD LONG-LIVED REFERENCES TO THE OBJECT RETURNED BY THIS METHOD, AS
     * THIS WILL CAUSE
     * MEMORY LEAKS.
     */
    private @Nullable ConfirmUpdateModule.FragmentManagerHelper getFragmentManagerHelper() {
        Activity activity = getCurrentActivity();
        if (activity == null || !(activity instanceof FragmentActivity)) {
            Log.d(NAME, "getFragmentManagerHelper: " + "current activity is null or not instance of FragmentActivity");
            return null;
        }
        return new ConfirmUpdateModule.FragmentManagerHelper(((FragmentActivity) activity).getSupportFragmentManager());
    }
}
