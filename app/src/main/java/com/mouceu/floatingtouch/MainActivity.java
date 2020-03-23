package com.mouceu.floatingtouch;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final int DRAW_OVER_OTHER_APP_PERMISSION_CODE = 2084;
    private static final int ENABLE_ACCESSIBILITY_CODE = 2085;
    static final String SERVICE_PARAMS = "com.mouceu.floatingtouch.SERVICE_PARAMS";
    static final String OPACITY_SETTING_NAME = "OPACITY";

    private int selectedOpacity = 0;
    private TextView opacityPickerValue;
    private LocalBroadcastManager broadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        broadcastManager = LocalBroadcastManager.getInstance(this);
        setContentView(R.layout.activity_main);

        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            // TODO: message/instruction
            startActivityForResult(intent, DRAW_OVER_OTHER_APP_PERMISSION_CODE);
        }
        if (!Util.isAccessibilityServiceEnabled(this, FloatingViewService.class)) {
            // TODO: message/instruction
            Intent accessibilityIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivityForResult(accessibilityIntent, ENABLE_ACCESSIBILITY_CODE);
        }

        initOpacityPicker();
        initManageAccessibilityButton();
        initManageOverlayButton();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DRAW_OVER_OTHER_APP_PERMISSION_CODE) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(
                        this,
                        R.string.permissions_required,
                        Toast.LENGTH_SHORT
                ).show();
            }
        } else if (requestCode == ENABLE_ACCESSIBILITY_CODE) {
            if (!Util.isAccessibilityServiceEnabled(this, FloatingViewService.class)) {
                Toast.makeText(
                        this,
                        "Enable FloatingView service in accessibility settings",
                        Toast.LENGTH_SHORT
                ).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void initOpacityPicker() {
        SeekBar opacityPicker = findViewById(R.id.opacity_picker);
        opacityPickerValue = findViewById(R.id.opacity_picker_value);
        selectedOpacity = Util.getSetting(OPACITY_SETTING_NAME, 0, MainActivity.this);
        opacityPickerValue.setText(String.valueOf(selectedOpacity));
        opacityPicker.setProgress(selectedOpacity);

        opacityPicker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedOpacity = seekBar.getProgress();
                opacityPickerValue.setText(String.valueOf(selectedOpacity));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Util.saveSetting(OPACITY_SETTING_NAME, selectedOpacity, MainActivity.this);
                Bundle bundle = new Bundle();
                bundle.putInt(OPACITY_SETTING_NAME, selectedOpacity);
                Intent intent = new Intent(SERVICE_PARAMS);
                intent.putExtras(bundle);
                broadcastManager.sendBroadcast(intent);
            }
        });
    }

    private void initManageAccessibilityButton() {
        findViewById(R.id.button_manage_accessibility).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent accessibilityIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(accessibilityIntent);
            }
        });
    }
    private void initManageOverlayButton() {
        findViewById(R.id.button_manage_overlay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())
                );
                startActivityForResult(intent, DRAW_OVER_OTHER_APP_PERMISSION_CODE);
            }
        });
    }
}
