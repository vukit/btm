package ru.vukit.btm;

import android.location.Location;

import androidx.annotation.Keep;

@Keep
public
class MainActivityModel {

    MainActivity controller;
    String selectedAction;
    String parentFragment = "";
    String childFragment = "";
    boolean isCheckedPermission = false;
    boolean permissionBluetooth;
    boolean permissionBluetoothAdmin;
    boolean permissionBluetoothConnect;
    boolean permissionInternet;
    boolean permissionAccessFineLocation;
    boolean permissionAccessCoarseLocation;
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

}
