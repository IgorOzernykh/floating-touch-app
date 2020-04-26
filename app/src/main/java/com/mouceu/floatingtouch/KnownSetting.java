package com.mouceu.floatingtouch;

import android.content.Context;

enum KnownSetting {
    ACTION_LEFT(R.string.setting_slide_left),
    ACTION_UP(R.string.setting_slide_up),
    ACTION_RIGHT(R.string.setting_slide_right),
    ACTION_DOWN(R.string.setting_slide_down),
    SENSITIVITY_ANGLE(R.string.angle_spinner_title)
    ;

    private final int resourceId;

    KnownSetting(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public String resolve(Context context) {
        return context.getString(resourceId);
    }
}
