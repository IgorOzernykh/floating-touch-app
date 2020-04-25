package com.mouceu.floatingtouch;

import android.content.Context;

enum KnownAction {
    OPEN_RECENT_APPS(R.string.action_open_recent_apps),
    OPEN_NOTIFICATIONS(R.string.action_open_notifications),
    OPEN_HOME_SCREEN(R.string.action_open_home_screen),
    OPEN_PREVIOUS_APP(R.string.action_open_previous_app);

    private final int resourceId;

    KnownAction(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public String resolve(Context context) {
        return context.getString(resourceId);
    }
}