package com.mouceu.floatingtouch;

import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    public static final String TOUCH_OPACITY = "com.mouceu.floatingtouch.MainActivity_SERVICE_DATA";
    private static final int DRAW_OVER_OTHER_APP_PERMISSION_CODE = 2084;

    private int selectedOpacity = 0;
    private SeekBar opacityPicker;
    private TextView opacityPickerValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            startActivityForResult(intent, DRAW_OVER_OTHER_APP_PERMISSION_CODE);
        } else {
            initView();
        }

        initOpacityPicker();
        initRemoveButton();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DRAW_OVER_OTHER_APP_PERMISSION_CODE) {
            if (resultCode == RESULT_OK) {
                initView();
            } else {
                Toast.makeText(
                        this,
                        "Draw over apps permission required. Exiting",
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void initView() {
        findViewById(R.id.btn_enable).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putInt(TOUCH_OPACITY, selectedOpacity);
                Intent intent = new Intent(MainActivity.this, FloatingViewService.class);
                intent.putExtras(bundle);
                startService(intent);
                finish();
            }
        });
    }

    private void initOpacityPicker() {
        opacityPicker = findViewById(R.id.opacity_picker);
        opacityPickerValue = findViewById(R.id.opacity_picker_value);

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
            }
        });

    }

    private void initRemoveButton() {
        findViewById(R.id.button_disable).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(MainActivity.this, FloatingViewService.class));
                finish();
            }
        });
    }
}
