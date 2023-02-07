package ru.vukit.btm;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Handler;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;

import ru.vukit.btm.bluetooth.BluetoothDriver;

public class Worker {

    private final Activity activity;
    private final BluetoothDriver btDriver;
    private final Work callback;
    private final Resources resources = StartApplication.getInstance().getResources();

    public interface Work {
        void work();
    }

    public Worker(Activity activity, BluetoothDriver btDriver, Work callback) {
        this.activity = activity;
        this.btDriver = btDriver;
        this.callback = callback;
    }

    @SuppressLint("InflateParams")
    public void start() {
        if (!MainActivityModel.getInstance().permissionBluetooth) {
            new SnackBar().ShowLong(resources.getString(R.string.no_bluetooth_permissions));
            return;
        }

        if (btDriver.isEnabled()) {
            callback.work();
            return;
        }

        btDriver.Enable();

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.bluetooth_enable_popup, null));
        builder.setCancelable(false);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (btDriver.isEnabled()) {
                    handler.removeCallbacks(this);
                    alertDialog.dismiss();
                    callback.work();
                    return;
                }
                handler.postDelayed(this, 100);
            }
        });
    }
}
