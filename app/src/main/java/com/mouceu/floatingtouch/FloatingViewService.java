package com.mouceu.floatingtouch;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

    @Override
    public void onCreate() {
        super.onCreate();
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_touch, null);
        floatingView.setAlpha(getStoredOpacity());

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                if (extras == null) return;
                if (extras.containsKey(MainActivity.OPACITY_SETTING_NAME)) {
                    floatingView.setAlpha(getOpacity(extras.getInt(MainActivity.OPACITY_SETTING_NAME)));
                }
                if (extras.containsKey(MainActivity.ANGLE_SETTING_NAME)) {
                    int newAngle = extras.getInt(MainActivity.ANGLE_SETTING_NAME);
                    if (newAngle > 0)
                        coneOfSensitivityAngle = newAngle;
                }
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

    private float getOpacity(int opacity) {
        opacity = (opacity > 100) ? 100 : ((opacity < 0) ? 0 : opacity);
        return (100 - opacity) / 100.0f;
    }

    private float getStoredOpacity() {
        int opacity = Util.getSetting(MainActivity.OPACITY_SETTING_NAME, 0, this);
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
        private final SwipeState swipeState = new SwipeState();
        private AtomicBoolean longPressed = new AtomicBoolean(false);


        private int initialX;
        private int initialY;
        private float initialTouchX;
        private float initialTouchY;

        public FloatingViewOnTouchListener(WindowManager.LayoutParams layoutParams) {
            this.layoutParams = layoutParams;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
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
                        detectSwipe(event);
                        if (swipeState.any()) {
                            Log.d(TAG, "swipe detected");
                            onSwipe();
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
                    int rad = 25;
                    if (Math.pow(event.getRawX() - initialTouchX, 2)
                            + Math.pow(event.getRawY() - initialTouchY, 2) <= rad * rad) {
                        Log.d(TAG, "Min threshold is not reached");
                        break;
                    }
                    if (longPressed.get()) {
                        Log.d(TAG, "Action move long pressed");

                        int x = initialX + (int) (event.getRawX() - initialTouchX);
                        int y = initialY + (int) (event.getRawY() - initialTouchY);
                        layoutParams.x = x;
                        layoutParams.y = y;

                        windowManager.updateViewLayout(floatingView, layoutParams);
                    } else {
                        Log.d(TAG, "Action move normal");
                        handler.removeCallbacks(longPressedChecked);
                    }
                    break;
                default:
                    return false;
            }

            return true;
        }

        private class SwipeState {
            private boolean swipeUp = false;
            private boolean swipeDown = false;
            private boolean swipeLeft = false;
            private boolean swipeRight = false;
            private boolean click = false;

            public void swipeUp() {
                this.swipeUp = true;
            }

            public void swipeDown() {
                this.swipeDown = true;
            }

            public void swipeLeft() {
                this.swipeLeft = true;
            }

            public void swipeRight() {
                this.swipeRight = true;
            }

            public void click() {
                this.click = true;
            }

            public boolean any() {
                return swipeUp || swipeDown || swipeLeft || swipeRight || click;
            }

            public void reset() {
                swipeUp = false;
                swipeDown = false;
                swipeLeft = false;
                swipeRight = false;
                click = false;
            }
        }


        private void onSwipe() {
            if (swipeState.click) {
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
            if (swipeState.swipeUp) {
                if ("OnePlus".equalsIgnoreCase(Build.MANUFACTURER)) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    performGlobalAction(GLOBAL_ACTION_HOME);
                }
            }
            if (swipeState.swipeDown)
                performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
            if (swipeState.swipeRight) {
                performGlobalAction(GLOBAL_ACTION_RECENTS);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        performGlobalAction(GLOBAL_ACTION_RECENTS);
                    }
                }, 50);

            }
            if (swipeState.swipeLeft)
                performGlobalAction(GLOBAL_ACTION_RECENTS);
            swipeState.reset();
        }

        private boolean detectSwipe(MotionEvent event) {
            int rad = 25;
            float x = event.getRawX();
            float y = event.getRawY();
            if (Math.pow(x - initialTouchX, 2) + Math.pow(y - initialTouchY, 2) <= rad * rad) {
                Log.d(TAG, "Min threshold is not reached");
                swipeState.click();
                return true;
            }

            double slope = getSlope(coneOfSensitivityAngle);
            if (y - initialTouchY >= slope * (x - initialTouchX)
                    && y - initialTouchY >= slope * (-x + initialTouchX)) {
                swipeState.swipeDown();
                Log.d(TAG, "Swipe down");
                return true;
            }
            if (y - initialTouchY > (x - initialTouchX) / slope
                    && y - initialTouchY < (-x + initialTouchX) / slope) {
                swipeState.swipeLeft();
                Log.d(TAG, "Swipe left");
                return true;
            }
            if (y - initialTouchY <= slope * (x - initialTouchX)
                    && y - initialTouchY <= slope * (-x + initialTouchX)) {
                swipeState.swipeUp();
                Log.d(TAG, "Swipe up");
                return true;
            }
            if (y - initialTouchY < (x - initialTouchX) / slope
                    && y - initialTouchY > (-x + initialTouchX) / slope) {
                swipeState.swipeRight();
                Log.d(TAG, "Swipe right");
                return true;
            }
            return false;
        }

        private double getSlope(int angleDegree) {
            if (angleDegree >= 90 || angleDegree <= 0)
                return 1;

            double a = Math.tan(angleDegree * Math.PI / 180);
            return  (1 + Math.sqrt(1 + a * a)) / a;
        }
    }
}
