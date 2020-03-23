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

    @Override
    public void onCreate() {
        super.onCreate();
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_touch, null);
        floatingView.setAlpha(getStoredOpacity());

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                floatingView.setAlpha(getOpacity(intent.getExtras()));
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

    private float getOpacity(Bundle extras) {
        int opacity = 0;
        if (extras != null)
            opacity = extras.getInt(MainActivity.OPACITY_SETTING_NAME);
        opacity = (opacity > 100) ? 100 : ((opacity < 0) ? 0 : opacity);
        return (100 - opacity) / 100.0f;
    }

    private float getStoredOpacity() {
        int opacity = Util.getSetting(MainActivity.OPACITY_SETTING_NAME, 0, this);
        opacity = (opacity > 100) ? 100 : ((opacity < 0) ? 0 : opacity);
        return (100 - opacity) / 100.0f;
    }

    private class FloatingViewOnTouchListener implements View.OnTouchListener {
        private final WindowManager.LayoutParams layoutParams;
        private final Handler handler = new Handler();
        private final Runnable longPressedChecked = new Runnable() {
            @Override
            public void run() {
                longPressed.set(true);
                Log.d("TAG", "LongPressed");
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
                        if (swipeState.any()) {
                            Log.d("TAG", "swipe detected");
                            onSwipe();
                        } else
                            performGlobalAction(GLOBAL_ACTION_BACK);
                    } else {
                        longPressed.set(false);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (longPressed.get()) {

                        int x = initialX + (int) (event.getRawX() - initialTouchX);
                        layoutParams.x = x;
                        int y = initialY + (int) (event.getRawY() - initialTouchY);
                        layoutParams.y = y;

                        Context ctx = getApplicationContext();
                        Util.saveSetting(POSITION_X_KEY, x, ctx);
                        Util.saveSetting(POSITION_Y_KEY, y, ctx);
                        windowManager.updateViewLayout(floatingView, layoutParams);
                    } else {
                        handler.removeCallbacks(longPressedChecked);
                        return detectSwipe(event);
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

            public boolean any() {
                return swipeUp || swipeDown || swipeLeft || swipeRight;
            }

            public void reset() {
                swipeUp = false;
                swipeDown = false;
                swipeLeft = false;
                swipeRight = false;
            }
        }


        private void onSwipe() {
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
            if (Math.abs(event.getRawX() - initialTouchX) < 15 && (initialTouchY - event.getRawY() > 25)) {
                swipeState.swipeUp();
                Log.d("TAG", "swipe up");
                return true;
            }
            if (Math.abs(event.getRawX() - initialTouchX) < 15 && (event.getRawY() - initialTouchY > 25)) {
                swipeState.swipeDown();
                Log.d("TAG", "swipe down");
                return true;
            }
            if (Math.abs(event.getRawY() - initialTouchY) < 15 && (event.getRawX() - initialTouchX > 25)) {
                swipeState.swipeRight();
                Log.d("TAG", "swipe right");
                return true;
            }
            if (Math.abs(event.getRawY() - initialTouchY) < 15 && (initialTouchX - event.getRawX() > 25)) {
                swipeState.swipeLeft();
                Log.d("TAG", "swipe left");
                return true;
            }
            return false;
        }
    }
}
