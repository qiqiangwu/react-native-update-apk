package com.evm.ued.rnupdateapk;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.widget.ScrollView;

public class ConfirmMessageScrollView extends ScrollView {
    private final static String TAG = "ConfirmMessage";
    private final Context mContext;

    public ConfirmMessageScrollView(Context context) {
        this(context, null);
    }

    public ConfirmMessageScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConfirmMessageScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        try {
            Display display = ((Activity) mContext).getWindowManager().getDefaultDisplay();
            DisplayMetrics d = new DisplayMetrics();
            display.getMetrics(d);
            // 设置控件最大高度不能超过屏幕高度的一半
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(d.heightPixels / 3, MeasureSpec.AT_MOST);
            Log.d(TAG, "onMeasure height: " + heightMeasureSpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 重新计算控件的宽高
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
