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
    private static final String TAG = "FloatingView";
    private static final int DEFAULT_X = 100;
    private static final int DEFAULT_Y = 100;
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private View floatingView;
    private View floatingButton;
    private BroadcastReceiver broadcastReceiver;
    private int coneOfSensitivityAngle = MainActivity.DEFAULT_ANGLE;
    private double slope = 1;
    private int touchAreaSize = MainActivity.DEFAULT_TOUCH_AREA_SIZE;
    private int floatingTouchSize = MainActivity.DEFAULT_FLOATING_TOUCH_SIZE;
    private SlideAction actionLeft;
    private SlideAction actionUp;
    private SlideAction actionRight;
    private SlideAction actionDown;
    private SlideAction actionTouch;

    @Override
    public void onCreate() {
        super.onCreate();
        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = Util.getSetting(AppSetting.POSITION_X.name(), DEFAULT_X, this);
        layoutParams.y = Util.getSetting(AppSetting.POSITION_Y.name(), DEFAULT_Y, this);
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_touch, null);
        floatingView.setAlpha(getStoredOpacity());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(floatingView, layoutParams);
        floatingButton = floatingView.findViewById(R.id.iv_floating_button);
        floatingButton.setOnTouchListener(new FloatingViewOnTouchListener());

        restoreSettings();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateSettings(intent);
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(broadcastReceiver, new IntentFilter(MainActivity.SERVICE_PARAMS));
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

    private void restoreSettings() {
        final String format = "Stored setting: %s = %s";
        actionLeft = Util.getStoredAction(
                AppSetting.ACTION_LEFT, SlideAction.OPEN_RECENT_APPS, this);
        Log.d(TAG, String.format(format, AppSetting.ACTION_LEFT, actionLeft));

        actionUp = Util.getStoredAction(
                AppSetting.ACTION_UP, SlideAction.OPEN_HOME_SCREEN, this);
        Log.d(TAG, String.format(format, AppSetting.ACTION_UP, actionUp));

        actionRight = Util.getStoredAction(
                AppSetting.ACTION_RIGHT, SlideAction.OPEN_PREVIOUS_APP, this);
        Log.d(TAG, String.format(format, AppSetting.ACTION_RIGHT, actionRight));

        actionDown = Util.getStoredAction(
                AppSetting.ACTION_DOWN, SlideAction.OPEN_NOTIFICATIONS, this);
        Log.d(TAG, String.format(format, AppSetting.ACTION_DOWN, actionDown));

        actionTouch = Util.getStoredAction(
                AppSetting.ACTION_TOUCH, SlideAction.GO_BACK, this);
        Log.d(TAG, String.format(format, AppSetting.ACTION_TOUCH, actionTouch));

        coneOfSensitivityAngle = Util.getSetting(
                AppSetting.SENSITIVITY_ANGLE.name(), MainActivity.DEFAULT_ANGLE, this);
        Log.d(TAG, String.format(format, AppSetting.SENSITIVITY_ANGLE, coneOfSensitivityAngle));

        slope = getSlope(coneOfSensitivityAngle);

        touchAreaSize = Util.getSetting(
                AppSetting.TOUCH_AREA.name(), MainActivity.DEFAULT_TOUCH_AREA_SIZE, this);
        Log.d(TAG, String.format(format, AppSetting.TOUCH_AREA, touchAreaSize));

        floatingTouchSize = Util.getSetting(AppSetting.FLOATING_TOUCH_SIZE.name(),
                MainActivity.DEFAULT_FLOATING_TOUCH_SIZE, this);
        Log.d(TAG, String.format(format, AppSetting.FLOATING_TOUCH_SIZE, floatingTouchSize));
        updateFloatingViewSize();
    }


    private void updateFloatingViewSize() {
        float density = getResources().getDisplayMetrics().density;
        floatingButton.getLayoutParams().width = (int) (density * floatingTouchSize);
        floatingButton.getLayoutParams().height = (int) (density * floatingTouchSize);
    }

    private void updateSettings(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) return;
        final String format = "Update setting: %s = %s";
        if (extras.containsKey(AppSetting.OPACITY.name())) {
            int newOpacityValue = extras.getInt(AppSetting.OPACITY.name());
            Log.d(TAG, String.format(format, AppSetting.OPACITY, newOpacityValue));
            floatingView.setAlpha(getOpacity(newOpacityValue));
        }
        if (extras.containsKey(AppSetting.SENSITIVITY_ANGLE.name())) {
            String newAngleStr = extras.getString(AppSetting.SENSITIVITY_ANGLE.name());
            if (newAngleStr != null) {
                int newAngle = Integer.parseInt(newAngleStr);
                if (newAngle > 0) {
                    coneOfSensitivityAngle = newAngle;
                    Log.d(TAG, String.format(format, AppSetting.SENSITIVITY_ANGLE, newAngle));
                    slope = getSlope(newAngle);
                }
            }
        }
        if (extras.containsKey(AppSetting.ACTION_LEFT.name())) {
            actionLeft = SlideAction.valueOf(extras.getString(AppSetting.ACTION_LEFT.name()));
            Log.d(TAG, String.format(format, AppSetting.ACTION_LEFT, actionLeft));
        }
        if (extras.containsKey(AppSetting.ACTION_UP.name())) {
            actionUp = SlideAction.valueOf(extras.getString(AppSetting.ACTION_UP.name()));
            Log.d(TAG, String.format(format, AppSetting.ACTION_UP, actionUp));
        }
        if (extras.containsKey(AppSetting.ACTION_RIGHT.name())) {
            actionRight = SlideAction.valueOf(extras.getString(AppSetting.ACTION_RIGHT.name()));
            Log.d(TAG, String.format(format, AppSetting.ACTION_RIGHT, actionRight));
        }
        if (extras.containsKey(AppSetting.ACTION_DOWN.name())) {
            actionDown = SlideAction.valueOf(extras.getString(AppSetting.ACTION_DOWN.name()));
            Log.d(TAG, String.format(format, AppSetting.ACTION_DOWN, actionDown));
        }
        if (extras.containsKey(AppSetting.ACTION_TOUCH.name())) {
            actionTouch = SlideAction.valueOf(extras.getString(AppSetting.ACTION_TOUCH.name()));
            Log.d(TAG, String.format(format, AppSetting.ACTION_TOUCH, actionTouch));
        }
        if (extras.containsKey(AppSetting.TOUCH_AREA.name())) {
            touchAreaSize = extras.getInt(AppSetting.TOUCH_AREA.name());
            Log.d(TAG, String.format(format, AppSetting.TOUCH_AREA, touchAreaSize));
        }
        if (extras.containsKey(AppSetting.FLOATING_TOUCH_SIZE.name())) {
            floatingTouchSize = extras.getInt(AppSetting.FLOATING_TOUCH_SIZE.name());
            Log.d(TAG, String.format(format, AppSetting.FLOATING_TOUCH_SIZE, floatingTouchSize));
            updateFloatingViewSize();
            windowManager.updateViewLayout(floatingView, layoutParams);
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

    private double getSlope(int angleDegree) {
        if (angleDegree >= 90 || angleDegree <= 0)
            return 1;

        double a = Math.tan(angleDegree * Math.PI / 180);
        return  (1 + Math.sqrt(1 + a * a)) / a;
    }

    private class FloatingViewOnTouchListener implements View.OnTouchListener {
        private static final String TAG = "Gesture";
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

        @Override
        @SuppressLint("ClickableViewAccessibility")
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = layoutParams.x;
                    initialY = layoutParams.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();

                    if (!longPressed.get()) {
                        handler.postDelayed(longPressedChecked, ViewConfiguration.getLongPressTimeout());
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    handler.removeCallbacks(longPressedChecked);
                    if (!longPressed.get()) {
                        Log.d(TAG, "Action up normal");

                        final SlideAction action = detectSwipe(event);
                        if (action != SlideAction.NONE) {
                            Log.d(TAG, "swipe detected");
                            handle(action);
                        }
                    } else {
                        Log.d(TAG, "Action up long pressed");
                        longPressed.set(false);

                        int x = getNewCoordinate(initialX, event.getRawX(), initialTouchX);
                        int y = getNewCoordinate(initialY, event.getRawY(), initialTouchY);
                        Util.saveSetting(AppSetting.POSITION_X.name(), x, FloatingViewService.this);
                        Util.saveSetting(AppSetting.POSITION_Y.name(), y, FloatingViewService.this);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:

                    if (longPressed.get()) {
                        Log.d(TAG, "Action move long pressed");

                        int x = getNewCoordinate(initialX, event.getRawX(), initialTouchX);
                        int y = getNewCoordinate(initialY, event.getRawY(), initialTouchY);
                        layoutParams.x = x;
                        layoutParams.y = y;

                        windowManager.updateViewLayout(floatingView, layoutParams);
                    } else {
                        if (isWithinTouchArea(event.getRawX(), event.getRawY())) {
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
                    }, 100);
                    break;
                case OPEN_RECENT_APPS:
                    performGlobalAction(GLOBAL_ACTION_RECENTS);
                    break;
            }
        }

        @NonNull
        private SlideAction detectSwipe(MotionEvent event) {
            float x = event.getRawX();
            float y = event.getRawY();
            if (isWithinTouchArea(x, y)) {
                Log.d(TAG, "Min threshold is not reached");
                return actionTouch;
            }

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
            return SlideAction.NONE;
        }

        private int getNewCoordinate(int viewInitial, float eventCurrent, float eventInitial) {
            return viewInitial + (int) (eventCurrent - eventInitial);
        }

        private boolean isWithinTouchArea(float x, float y) {
            return Math.pow(x - initialTouchX, 2) + Math.pow(y - initialTouchY, 2)
                    <= touchAreaSize * touchAreaSize;
        }
    }
}
