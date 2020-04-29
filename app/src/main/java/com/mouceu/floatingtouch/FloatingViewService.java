package com.mouceu.floatingtouch;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import java.util.concurrent.atomic.AtomicBoolean;

public class FloatingViewService extends AccessibilityService {
    private static final String POSITION_X_KEY = "POS_X";
    private static final String POSITION_Y_KEY = "POS_Y";

    private WindowManager windowManager;
    private View floatingView;
    private BroadcastReceiver broadcastReceiver;
    private int coneOfSensitivityAngle = MainActivity.DEFAULT_ANGLE;
    private int touchAreaSize = MainActivity.DEFAULT_TOUCH_AREA_SIZE;
    private SlideAction actionLeft;
    private SlideAction actionUp;
    private SlideAction actionRight;
    private SlideAction actionDown;
    private SlideAction actionTouch;

    @Override
    public void onCreate() {
        super.onCreate();
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_touch, null);
        floatingView.setAlpha(getStoredOpacity());
        actionLeft = Util.getStoredAction(AppSetting.ACTION_LEFT, SlideAction.OPEN_RECENT_APPS, this);
        actionUp = Util.getStoredAction(AppSetting.ACTION_UP, SlideAction.OPEN_HOME_SCREEN, this);
        actionRight = Util.getStoredAction(AppSetting.ACTION_RIGHT, SlideAction.OPEN_PREVIOUS_APP, this);
        actionDown = Util.getStoredAction(AppSetting.ACTION_DOWN, SlideAction.OPEN_NOTIFICATIONS, this);
        actionTouch = Util.getStoredAction(AppSetting.ACTION_TOUCH, SlideAction.GO_BACK, this);
        coneOfSensitivityAngle = Integer.parseInt(Util.getSetting(AppSetting.SENSITIVITY_ANGLE.name(), String.valueOf(MainActivity.DEFAULT_ANGLE), this));
        touchAreaSize = Util.getSetting(AppSetting.TOUCH_AREA.name(), MainActivity.DEFAULT_TOUCH_AREA_SIZE, this);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateSettings(intent);
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(broadcastReceiver, new IntentFilter(MainActivity.SERVICE_PARAMS));

        final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = Util.getSetting(POSITION_X_KEY, 100, this);
        layoutParams.y = Util.getSetting(POSITION_Y_KEY, 100, this);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(floatingView, layoutParams);

        final View floatingView = this.floatingView.findViewById(R.id.iv_floating_button);
        floatingView.setOnTouchListener(new FloatingViewOnTouchListener(layoutParams));
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    private void updateSettings(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) return;
        if (extras.containsKey(AppSetting.OPACITY.name())) {
            floatingView.setAlpha(getOpacity(extras.getInt(AppSetting.OPACITY.name())));
        }
        if (extras.containsKey(AppSetting.SENSITIVITY_ANGLE.name())) {
            String newAngleStr = extras.getString(AppSetting.SENSITIVITY_ANGLE.name());
            if (newAngleStr != null) {
                int newAngle = Integer.parseInt(newAngleStr);
                if (newAngle > 0)
                    coneOfSensitivityAngle = newAngle;
            }
        }
        if (extras.containsKey(AppSetting.ACTION_LEFT.name())) {
            actionLeft = SlideAction.valueOf(extras.getString(AppSetting.ACTION_LEFT.name()));
        }
        if (extras.containsKey(AppSetting.ACTION_UP.name())) {
            actionUp = SlideAction.valueOf(extras.getString(AppSetting.ACTION_UP.name()));
        }
        if (extras.containsKey(AppSetting.ACTION_RIGHT.name())) {
            actionRight = SlideAction.valueOf(extras.getString(AppSetting.ACTION_RIGHT.name()));
        }
        if (extras.containsKey(AppSetting.ACTION_DOWN.name())) {
            actionDown = SlideAction.valueOf(extras.getString(AppSetting.ACTION_DOWN.name()));
        }
        if (extras.containsKey(AppSetting.ACTION_TOUCH.name())) {
            actionTouch = SlideAction.valueOf(extras.getString(AppSetting.ACTION_TOUCH.name()));
        }
        if (extras.containsKey(AppSetting.TOUCH_AREA.name())) {
            touchAreaSize = extras.getInt(AppSetting.TOUCH_AREA.name());
        }
    }

    private float getOpacity(int opacity) {
        opacity = (opacity > 100) ? 100 : ((opacity < 0) ? 0 : opacity);
        return (100 - opacity) / 100.0f;
    }

    private float getStoredOpacity() {
        int opacity = Util.getSetting(AppSetting.OPACITY.name(), 0, this);
        return getOpacity(opacity);
    }

    private class FloatingViewOnTouchListener implements View.OnTouchListener {
        private static final String TAG = "Gesture";
        private final WindowManager.LayoutParams layoutParams;
        private final Handler handler = new Handler();
        private final Runnable longPressedChecked = new Runnable() {
            @Override
            public void run() {
                longPressed.set(true);
            }
        };
        private AtomicBoolean longPressed = new AtomicBoolean(false);


        private int initialX;
        private int initialY;
        private float initialTouchX;
        private float initialTouchY;

        public FloatingViewOnTouchListener(WindowManager.LayoutParams layoutParams) {
            this.layoutParams = layoutParams;
        }

        @Override
        @SuppressLint("ClickableViewAccessibility")
        public boolean onTouch(View v, MotionEvent event) {
            SlideAction action = null;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = layoutParams.x;
                    initialY = layoutParams.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();

                    if (!longPressed.get())
                        handler.postDelayed(longPressedChecked, ViewConfiguration.getLongPressTimeout());

                    break;
                case MotionEvent.ACTION_UP:
                    handler.removeCallbacks(longPressedChecked);
                    if (!longPressed.get()) {
                        Log.d(TAG, "Action up normal");
                        if ((action = detectSwipe(event)) != null) {
                            Log.d(TAG, "swipe detected");
                            handle(action);
                        }
                    } else {
                        Log.d(TAG, "Action up long pressed");
                        longPressed.set(false);

                        int x = initialX + (int) (event.getRawX() - initialTouchX);
                        int y = initialY + (int) (event.getRawY() - initialTouchY);
                        Util.saveSetting(POSITION_X_KEY, x, FloatingViewService.this);
                        Util.saveSetting(POSITION_Y_KEY, y, FloatingViewService.this);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:

                    if (longPressed.get()) {
                        Log.d(TAG, "Action move long pressed");

                        int x = initialX + (int) (event.getRawX() - initialTouchX);
                        int y = initialY + (int) (event.getRawY() - initialTouchY);
                        layoutParams.x = x;
                        layoutParams.y = y;

                        windowManager.updateViewLayout(floatingView, layoutParams);
                    } else {
                        int rad = touchAreaSize;
                        if (Math.pow(event.getRawX() - initialTouchX, 2)
                                + Math.pow(event.getRawY() - initialTouchY, 2) <= rad * rad) {
                            Log.d(TAG, "Min threshold is not reached");
                            break;
                        }
                        Log.d(TAG, "Action move normal");
                        handler.removeCallbacks(longPressedChecked);
                    }
                    break;
                default:
                    return false;
            }

            return true;
        }

        private void handle(@NonNull SlideAction action) {
            switch (action) {
                case GO_BACK:
                    performGlobalAction(GLOBAL_ACTION_BACK);
                    break;
                case OPEN_HOME_SCREEN:
                    if ("OnePlus".equalsIgnoreCase(Build.MANUFACTURER)) {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } else {
                        performGlobalAction(GLOBAL_ACTION_HOME);
                    }
                    break;
                case OPEN_NOTIFICATIONS:
                    performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
                    break;
                case OPEN_PREVIOUS_APP:
                    performGlobalAction(GLOBAL_ACTION_RECENTS);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            performGlobalAction(GLOBAL_ACTION_RECENTS);
                        }
                    }, 50);
                    break;
                case OPEN_RECENT_APPS:
                    performGlobalAction(GLOBAL_ACTION_RECENTS);
                    break;
            }
        }

        @Nullable
        private SlideAction detectSwipe(MotionEvent event) {
            int rad = touchAreaSize;
            float x = event.getRawX();
            float y = event.getRawY();
            if (Math.pow(x - initialTouchX, 2) + Math.pow(y - initialTouchY, 2) <= rad * rad) {
                Log.d(TAG, "Min threshold is not reached");
                return actionTouch;
            }

            double slope = getSlope(coneOfSensitivityAngle);
            if (y - initialTouchY >= slope * (x - initialTouchX)
                    && y - initialTouchY >= slope * (-x + initialTouchX)) {
                Log.d(TAG, "Swipe down");
                return actionDown;
            }
            if (y - initialTouchY > (x - initialTouchX) / slope
                    && y - initialTouchY < (-x + initialTouchX) / slope) {
                Log.d(TAG, "Swipe left");
                return actionLeft;
            }
            if (y - initialTouchY <= slope * (x - initialTouchX)
                    && y - initialTouchY <= slope * (-x + initialTouchX)) {
                Log.d(TAG, "Swipe up");
                return actionUp;
            }
            if (y - initialTouchY < (x - initialTouchX) / slope
                    && y - initialTouchY > (-x + initialTouchX) / slope) {
                Log.d(TAG, "Swipe right");
                return actionRight;
            }
            return null;
        }

        private double getSlope(int angleDegree) {
            if (angleDegree >= 90 || angleDegree <= 0)
                return 1;

            double a = Math.tan(angleDegree * Math.PI / 180);
            return  (1 + Math.sqrt(1 + a * a)) / a;
        }
    }
}
