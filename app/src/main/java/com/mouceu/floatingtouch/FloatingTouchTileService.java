package com.mouceu.floatingtouch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class FloatingTouchTileService extends TileService {
    static final String TOUCH_STATE = "com.mouceu.floatingtouch.TOUCH_STATE";
    static final String TILE_STATE = "com.mouceu.floatingtouch.TILE_STATE";
    private static final String TAG = "FloatingTile";

    private LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Tile tile = getQsTile();
            if (tile == null) {
                return;
            }
            int state = intent.getIntExtra(FloatingViewService.STATE, FloatingViewService.STATE_UNKNOWN);
            Log.d(TAG, "Get response. State: " + state);
            if (state == FloatingViewService.STATE_VISIBLE) {
                Log.d(TAG, "Make tile active");
                tile.setState(Tile.STATE_ACTIVE);
            } else if (state == FloatingViewService.STATE_HIDDEN) {
                Log.d(TAG, "Make tile inactive");
                tile.setState(Tile.STATE_INACTIVE);
            } else {
                Log.d(TAG, "Make tile unavailable");
                tile.setState(Tile.STATE_UNAVAILABLE);
            }
            tile.updateTile();
        }
    };

    @Override
    public void onStartListening() {
        super.onStartListening();
        Log.d(TAG, "The tile starts listening");
        broadcastManager.registerReceiver(broadcastReceiver, new IntentFilter(TILE_STATE));

        if (!Util.isAccessibilityServiceEnabled(this, FloatingViewService.class)) {
            final Tile tile = getQsTile();
            if (tile != null) {
                tile.setState(Tile.STATE_UNAVAILABLE);
                tile.updateTile();
            }
        } else {
            Intent intent = new Intent(TOUCH_STATE);
            intent.putExtra(FloatingViewService.STATE, FloatingViewService.STATE_UNKNOWN);
            broadcastManager.sendBroadcast(intent);
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        Log.d(TAG, "The tile was clicked");

        final Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        boolean shouldBeActive = tile.getState() != Tile.STATE_ACTIVE;
        Log.d(TAG, "The view is requested to be " + (shouldBeActive ? "visible" : "hidden"));

        Intent intent = new Intent(TOUCH_STATE);
        intent.putExtra(FloatingViewService.STATE,
                shouldBeActive ? FloatingViewService.STATE_VISIBLE : FloatingViewService.STATE_HIDDEN);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        Log.d(TAG, "The tile stops listening");
        broadcastManager.unregisterReceiver(broadcastReceiver);
    }
}
