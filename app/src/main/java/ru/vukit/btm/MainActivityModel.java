package ru.vukit.btm;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;

import androidx.annotation.Keep;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

@Keep
public
class MainActivityModel {

    public static final int PERMISSIONS_REQUEST_ALL = 1;

    MainActivity controller;
    String selectedAction;
    String parentFragment = "";
    String childFragment = "";
    boolean isCheckedPermission = false;
    boolean permissionInternet = false;
    boolean permissionBluetooth = false;
    boolean permissionLocation = false;
    public Location lastLocation;

    private static MainActivityModel INSTANCE = null;

    public static synchronized MainActivityModel getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MainActivityModel();
        }
        return (INSTANCE);
    }

    void connectController(MainActivity controller) {
        this.controller = controller;
    }

    void disconnectController() {
        this.controller = null;
    }

    public List<String> getBluetoothPermissionsRequests() {
        List<String> permissionsRequests = new ArrayList<>();
        if ((ContextCompat.checkSelfPermission(controller, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED)) {
            permissionsRequests.add(android.Manifest.permission.BLUETOOTH);
        }
        if ((ContextCompat.checkSelfPermission(controller, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED)) {
            permissionsRequests.add(android.Manifest.permission.BLUETOOTH_ADMIN);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if ((ContextCompat.checkSelfPermission(controller, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)) {
                permissionsRequests.add(android.Manifest.permission.BLUETOOTH_CONNECT);
            }
            if ((ContextCompat.checkSelfPermission(controller, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
                permissionsRequests.add(android.Manifest.permission.BLUETOOTH_SCAN);
            }
        }
        return permissionsRequests;
    }

    public List<String> getLocationPermissionsRequests() {
        List<String> permissionsRequests = new ArrayList<>();
        if ((ContextCompat.checkSelfPermission(controller, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            permissionsRequests.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if ((ContextCompat.checkSelfPermission(controller, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            permissionsRequests.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        return permissionsRequests;
    }

}
