package ru.vukit.btm.bluetooth;

import androidx.annotation.Keep;

import java.util.HashMap;

@Keep
public
class BluetoothData {

    public static final int TYPE_BLUETOOTH_DISABLE = 1;
    public static final int TYPE_DISCOVERY_FINISHED = 2;
    public static final int TYPE_NEW_CLC_DISCOVERY_DATA = 3;
    public static final int TYPE_NEW_LE_DISCOVERY_DATA = 4;
    public static final int TYPE_BLUETOOTH_PAIRED = 5;
    public static final int TYPE_BLUETOOTH_UNPAIRED = 6;
    public static final int TYPE_BLUETOOTH_ERROR_PAIRED = 7;
    public static final int TYPE_BLUETOOTH_ERROR_UNPAIRED = 8;
    public static final int TYPE_NEW_CLC_DISCOVERY_SDP_UUIDS = 9;
    public static final int TYPE_TERMINAL_LOG_MESSAGE = 10;
    public static final int TYPE_TERMINAL_DATA = 11;

    private final int type;
    private String message;
    private HashMap<String, String> dataMap;
    private byte[] terminalData;

    BluetoothData(int type) {
        this.type = type;
        this.dataMap = null;
    }

    BluetoothData(int type, HashMap<String, String> dataMap) {
        this.type = type;
        this.dataMap = dataMap;
    }

    @SuppressWarnings("SameParameterValue")
    BluetoothData(int type, String message) {
        this.type = type;
        this.message = message;
    }

    @SuppressWarnings("SameParameterValue")
    BluetoothData(int type, byte[] terminalData) {
        this.type = type;
        this.terminalData = terminalData;
    }

    public HashMap<String, String> getDataMap() {
        return dataMap;
    }

    public int getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public byte[] getTerminalData() {
        return terminalData;
    }
}
