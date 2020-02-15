package com.mouceu.floatingtouch;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class FloatingViewService extends AccessibilityService {

    private WindowManager windowManager;
    private View floatingView;

    @Override
    public void onCreate() {
        super.onCreate();

    }

    private class MyOnTouchEventListener implements View.OnTouchListener {
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

        public MyOnTouchEventListener(WindowManager.LayoutParams layoutParams) {
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

                        layoutParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        layoutParams.y = initialY + (int) (event.getRawY() - initialTouchY);

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
                    intent.setAction(Intent.ACTION_MAIN);// "android.intent.action.MAIN"
                    intent.addCategory(Intent.CATEGORY_HOME); //"android.intent.category.HOME"
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
                performGlobalAction(GLOBAL_ACTION_RECENTS);
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


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras = intent.getExtras();
        int opacity = 0;
        if (extras != null)
            opacity = extras.getInt(MainActivity.TOUCH_OPACITY);
        opacity = (opacity > 100) ? 100 : ((opacity < 0) ? 0 : opacity);

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_touch, null);
        floatingView.setAlpha((100 - opacity) / 100.0f);

        final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = 0;
        layoutParams.y = 100;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null)
            throw new RuntimeException("Window manager is null");
        windowManager.addView(floatingView, layoutParams);

        final View floatingView = this.floatingView.findViewById(R.id.rl_floating_view_root);
        floatingView.setOnTouchListener(new MyOnTouchEventListener(layoutParams));
//        floatingView.setOnTouchListener(new View.OnTouchListener() {
//            private int initialX;
//            private int initialY;
//            private float initialTouchX;
//            private float initialTouchY;
//
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        initialX = layoutParams.x;
//                        initialY = layoutParams.y;
//
//                        initialTouchX = event.getRawX();
//                        initialTouchY = event.getRawY();
//                        return true;
//                    case MotionEvent.ACTION_UP:
//                        int xDiff = (int) (event.getRawX() - initialTouchX);
//                        int yDiff = (int) (event.getRawY() - initialTouchY);
//
//                        if (isTouchPerformed(xDiff, yDiff)) {
//                            performGlobalAction(GLOBAL_ACTION_BACK);
//                        }
//                        return true;
//
//                    case MotionEvent.ACTION_MOVE:
//
//                        layoutParams.x = initialX + (int) (event.getRawX() - initialTouchX);
//                        layoutParams.y = initialY + (int) (event.getRawY() - initialTouchY);
//
//                        windowManager.updateViewLayout(FloatingViewService.this.floatingView, layoutParams);
//                        return true;
//                }
//                return false;
//            }
//        });

        return super.onStartCommand(intent, flags, startId);
    }

    private boolean isTouchPerformed(int xDiff, int yDiff) {
        return xDiff < 10 && yDiff < 10;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null)
            windowManager.removeView(floatingView);
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void onInterrupt() {

    }
}
