package ru.vukit.btm;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Keep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;

import ru.vukit.btm.bluetooth.BluetoothData;
import ru.vukit.btm.database.DatabaseMessage;

@Keep
class SearchModel implements Observer {

    private SearchFragment controller;
    int bluetoothModeSelected = -1;
    boolean isDiscoveryRunning = false;
    String lastDateSearchCLC = "";
    String lastDateSearchLE = "";
    int currentPositionCLC = 0;
    int currentPositionLE = 0;

    final List<HashMap<String, String>> bluetoothDevicesCLC = new ArrayList<>();
    final List<HashMap<String, String>> bluetoothDevicesLE = new ArrayList<>();
    int counterSelectedBluetoothDevicesCLC = 0;
    int counterSelectedBluetoothDevicesLE = 0;
    final Handler mainLooperHandler = new Handler(Looper.getMainLooper());

    private static SearchModel INSTANCE = null;

    public static synchronized SearchModel getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SearchModel();
        }
        return (INSTANCE);
    }

    @Override
    public void update(Observable o, Object arg) {
        String observable_name = o.getClass().getSimpleName();
        if (observable_name.equals("BluetoothDriver")) {
            BluetoothData bluetoothData = (BluetoothData) arg;
            HashMap<String, String> dataMap;
            boolean isNewDevice = true;
            switch (bluetoothData.getType()) {
                case BluetoothData.TYPE_NEW_LE_DISCOVERY_DATA:
                    dataMap = bluetoothData.getDataMap();
                    for (HashMap<String, String> findDevice : bluetoothDevicesLE)
                        if (findDevice.containsValue(dataMap.get("address"))) {
                            isNewDevice = false;
                            findDevice.putAll(dataMap);
                            findDevice.put("rssi", String.format(Locale.getDefault(), "%3.0f", Double.valueOf(Objects.requireNonNull(dataMap.get("rssi")))));
                            break;
                        }
                    if (isNewDevice) {
                        dataMap.put("rssi", String.format(Locale.getDefault(), "%3.0f", Double.valueOf(Objects.requireNonNull(dataMap.get("rssi")))));
                        dataMap.put("isSelected", "false");
                        bluetoothDevicesLE.add(0, dataMap);
                    }
                    break;
                case BluetoothData.TYPE_NEW_CLC_DISCOVERY_DATA:
                    dataMap = bluetoothData.getDataMap();
                    for (HashMap<String, String> findDevice : bluetoothDevicesCLC)
                        if (findDevice.containsValue(dataMap.get("address"))) {
                            isNewDevice = false;
                            findDevice.putAll(dataMap);
                            findDevice.put("rssi", String.format(Locale.getDefault(), "%3.0f", Double.valueOf(Objects.requireNonNull(dataMap.get("rssi")))));
                            break;
                        }
                    if (isNewDevice) {
                        dataMap.put("rssi", String.format(Locale.getDefault(), "%3.0f", Double.valueOf(Objects.requireNonNull(dataMap.get("rssi")))));
                        dataMap.put("isSelected", "false");
                        bluetoothDevicesCLC.add(0, dataMap);
                    }
                    break;
                case BluetoothData.TYPE_NEW_CLC_DISCOVERY_SDP_UUIDS:
                    dataMap = bluetoothData.getDataMap();
                    for (HashMap<String, String> findDevice : bluetoothDevicesCLC)
                        if (findDevice.containsValue(dataMap.get("address"))) {
                            findDevice.putAll(dataMap);
                            break;
                        }
                    break;
                case BluetoothData.TYPE_DISCOVERY_FINISHED:
                case BluetoothData.TYPE_BLUETOOTH_DISABLE:
                    isDiscoveryRunning = false;
                    if (controller != null) controller.updateControlDiscovery();
                    break;
            }
            if (controller != null) {
                mainLooperHandler.post(() -> controller.updateView());
            }
        } else if (observable_name.equals("DatabaseDriver")) {
            DatabaseMessage databaseMessage = (DatabaseMessage) arg;
            if (controller != null && databaseMessage.getMessage() != null)
                new SnackBar().ShowShort(databaseMessage.getMessage());
        }
    }

    void connectController(SearchFragment controller) {
        this.controller = controller;
    }

    void disconnectController() {
        this.controller = null;
    }

}
