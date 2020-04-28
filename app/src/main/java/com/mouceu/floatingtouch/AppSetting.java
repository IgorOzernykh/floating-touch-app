package com.mouceu.floatingtouch;

import android.content.Context;

enum AppSetting {
    ACTION_LEFT(R.string.setting_slide_left),
    ACTION_UP(R.string.setting_slide_up),
    ACTION_RIGHT(R.string.setting_slide_right),
    ACTION_DOWN(R.string.setting_slide_down),
    ACTION_TOUCH(R.string.setting_touch),
    SENSITIVITY_ANGLE(R.string.angle_spinner_title),
    OPACITY(R.string.opacity_picker_title)
    ;

    private final int resourceId;

    AppSetting(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public String resolve(Context context) {
        return context.getString(resourceId);
    }
}
