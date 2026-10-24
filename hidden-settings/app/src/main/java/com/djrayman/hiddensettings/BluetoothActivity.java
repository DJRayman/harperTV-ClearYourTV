package com.djrayman.hiddensettings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/**
 * The Bluetooth mode chooser the Jixin firmware lost: its own chooser (com.jixin.btspeaker/.MainActivity) jumps
 * straight into speaker (sink) mode in onCreate and finishes, so the settings tile never reaches the device list.
 * This activity answers that tile's action (jixin.intent.action.BLUETOOTH_MODE_SELECTER, picked once with
 * "Always") and android.settings.BLUETOOTH_SETTINGS, which nothing on the projector handles.
 */
public class BluetoothActivity extends Activity {
    private static final String BT = "com.jixin.btspeaker";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        String[] labels = {
                getString(R.string.bt_source),
                getString(R.string.bt_sink),
                getString(R.string.bt_remote),
        };
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(R.string.bt_title)
                .setItems(labels, (dialog, which) -> {
                    if (which == 0) open(BT + ".SourceModeActivity");
                    else if (which == 1) open(BT + ".MainActivity");   // it forwards to speaker mode at once
                    else open(BT + ".BluetoothPairNormalActivity");
                    finish();
                })
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void open(String activity) {
        try {
            startActivity(new Intent().setComponent(new ComponentName(BT, activity)));
        } catch (RuntimeException e) {
            Toast.makeText(this, getString(R.string.failed, activity), Toast.LENGTH_LONG).show();
        }
    }
}
