package ru.vukit.btm;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Handler;

import androidx.annotation.Keep;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;

import ru.vukit.btm.bluetooth.BluetoothAppearance;
import ru.vukit.btm.bluetooth.BluetoothData;
import ru.vukit.btm.bluetooth.BluetoothDriver;
import ru.vukit.btm.bluetooth.BluetoothGattUUIDs;
import ru.vukit.btm.database.DatabaseDriver;
import ru.vukit.btm.database.DatabaseMessage;

@Keep
@SuppressLint("MissingPermission")
class DeviceDetailsModel implements Observer {

    static final int VIEW_HOME = 0;
    static final int VIEW_SERVICES = 1;
    static final int VIEW_TERMINAL = 2;

    private final DatabaseDriver dbDriver = DatabaseDriver.getInstance();
    private final BluetoothDriver btDriver = BluetoothDriver.getInstance();
    private final Handler handler = new Handler();

    private DeviceDetailsFragment controller;

    private final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(StartApplication.getInstance());
    private final Resources resources = StartApplication.getInstance().getResources();

    int viewMode = VIEW_HOME;
    long deviceId;
    String device_name;
    String device_address;
    String device_class;
    String device_type;
    String device_manufacturer;
    Double device_latitude;
    Double device_longitude;
    String device_scan_record;
    String device_service_uuids;
    int gattConnectionState = 0;
    String gattConnectionLog;
    int selectedServiceNumber = -1;
    int selectedCharacteristicNumber = -1;
    int selectedDescriptorNumber = -1;
    private int gattDiscoverServicesAttempts;
    private boolean isActionPairDevice;
    private boolean readCharacteristicsData;
    private int gattReadTimeout;
    private boolean gattAutoConnect;
    private Runnable runnablePairDevice;
    private Runnable runnableObject;
    private final ArrayList<Runnable> runnableArrayList = new ArrayList<>();

    String terminalServiceUUID = "00001101-0000-1000-8000-00805f9b34fb";
    String terminalLogSPP = "";
    String terminalLogDialUp = "";
    String terminalLogMyService = "";
    int terminalConnectState = 0;

    String device_details_home_radio_button = resources.getString(R.string.device_details_home_radio_button_map);
    String device_details_services_view = "";

    private List<BluetoothGattService> gattServices = null;
    private final HashMap<String, HashMap<String, String>> gattCharacteristicsData = new HashMap<>();
    private final ArrayList<BluetoothGattDescriptor> bluetoothGattDescriptors = new ArrayList<>();

    final ArrayList<HashMap<String, String>> ServicesList = new ArrayList<>();
    final ArrayList<ArrayList<HashMap<String, String>>> CharacteristicsList = new ArrayList<>();
    final HashMap<String, ArrayList<HashMap<String, String>>> DescriptorsList = new HashMap<>();

    private static DeviceDetailsModel INSTANCE = null;

    public static synchronized DeviceDetailsModel getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DeviceDetailsModel();
        }
        return (INSTANCE);
    }

    void connectController(DeviceDetailsFragment controller) {
        this.controller = controller;
    }

    void disconnectController() {
        this.controller = null;
    }

    void initDetails() {
        if (sharedPreferences.getString(SettingsFragment.KEY_DEVICE_DETAILS_STARTUP_SCREEN, "").equals(resources.getString(R.string.device_details_startup_screen_home))) {
            viewMode = VIEW_HOME;
        }
        if (sharedPreferences.getString(SettingsFragment.KEY_DEVICE_DETAILS_STARTUP_SCREEN, "").equals(resources.getString(R.string.device_details_startup_screen_gatt_services))) {
            viewMode = VIEW_SERVICES;
        }
        if (sharedPreferences.getString(SettingsFragment.KEY_DEVICE_DETAILS_STARTUP_SCREEN, "").equals(resources.getString(R.string.device_details_startup_screen_terminal))) {
            viewMode = VIEW_TERMINAL;
        }
        gattConnectionLog = "";
        device_details_services_view = "";
        ServicesList.clear();
        CharacteristicsList.clear();
        selectedServiceNumber = -1;
        selectedCharacteristicNumber = -1;
        selectedDescriptorNumber = -1;
        gattCharacteristicsData.clear();
        for (Runnable runnable : runnableArrayList)
            if (runnable != null) handler.removeCallbacks(runnable);
        runnableArrayList.clear();
        btDriver.stopGattClient();
        gattConnectionState = 0;
        terminalLogSPP = "";
        terminalLogDialUp = "";
        terminalLogMyService = "";
        terminalConnectState = 0;
    }

    void getDevice(long deviceId) {
        this.deviceId = deviceId;
        Cursor cursor;
        if (this.deviceId != -1) {
            cursor = dbDriver.rawQuery("SELECT * FROM " + DatabaseDriver.DatabaseContract.Devices.TABLE_NAME + " WHERE " + DatabaseDriver.DatabaseContract.Devices._ID + " = " + this.deviceId + ";");
            cursor.moveToFirst();
        } else {
            cursor = dbDriver.rawQuery("SELECT * FROM " + DatabaseDriver.DatabaseContract.Devices.TABLE_NAME + " WHERE " + DatabaseDriver.DatabaseContract.Devices.ADDRESS + " = '" + device_address + "';");
            cursor.moveToFirst();
            this.deviceId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices._ID));
        }
        device_name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices.NAME));
        device_address = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices.ADDRESS));
        device_class = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices.CLASS));
        device_type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices.TYPE));
        device_latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices.LATITUDE));
        device_longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices.LONGITUDE));
        device_scan_record = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices.SCAN_RECORD));
        device_service_uuids = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices.SERVICE_UUIDs));
        device_manufacturer = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices.MANUFACTURER));
        cursor.close();
        if (device_manufacturer.isEmpty()) getNameByMac(device_address);
    }

    void getDevice(HashMap<String, String> device) {
        deviceId = -1;
        device_name = device.get("name");
        device_address = device.get("address");
        device_service_uuids = device.getOrDefault("serviceUUIDs", "");
        device_class = device.getOrDefault("class", "");
        device_type = device.getOrDefault("type", "");
        if (device.containsKey("latitude")) {
            if (Objects.equals(device.get("latitude"), resources.getString(R.string.undefined)))
                device_latitude = MathModule.notLatitude;
            else
                device_latitude = Double.parseDouble(Objects.requireNonNull(device.get("latitude")));
        }
        if (device.containsKey("longitude")) {
            if (Objects.equals(device.get("longitude"), resources.getString(R.string.undefined)))
                device_longitude = MathModule.notLongitude;
            else
                device_longitude = Double.parseDouble(Objects.requireNonNull(device.get("longitude")));
        }
        if (device.containsKey("scanRecord"))
            device_scan_record = Objects.requireNonNull(device.get("scanRecord")).replaceAll(" ", "").replaceAll("\n", "");
        else device_scan_record = "";
        if (dbDriver.isDeviceExist(device_address)) {
            Cursor cursor;
            cursor = dbDriver.rawQuery("SELECT * FROM " + DatabaseDriver.DatabaseContract.Devices.TABLE_NAME + " WHERE " + DatabaseDriver.DatabaseContract.Devices.ADDRESS + " = '" + device_address + "';");
            cursor.moveToFirst();
            deviceId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices._ID));
            device_name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices.NAME));
            cursor.close();
            if (device.containsKey("latitude") && !Objects.equals(device.get("latitude"), resources.getString(R.string.undefined)) &&
                    device.containsKey("longitude") && !Objects.equals(device.get("longitude"), resources.getString(R.string.undefined))) {
                dbDriver.updateDeviceLocation(deviceId, device_longitude, device_latitude);
            }
        }
        device_manufacturer = "";
        getNameByMac(device_address);
    }

    private void updateDeviceManufacturer(final String name, final String mac_address) {
        if (controller != null) {
            controller.requireActivity().runOnUiThread(() -> {
                if (mac_address.equals(device_address)) {
                    device_manufacturer = name;
                    if (dbDriver.isDeviceExist(mac_address)) {
                        if (deviceId == -1) {
                            Cursor cursor;
                            cursor = dbDriver.rawQuery("SELECT * FROM " + DatabaseDriver.DatabaseContract.Devices.TABLE_NAME + " WHERE " + DatabaseDriver.DatabaseContract.Devices.ADDRESS + " = '" + mac_address + "';");
                            cursor.moveToFirst();
                            deviceId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices._ID));
                            cursor.close();
                        }
                        dbDriver.updateDeviceManufacturer(deviceId, device_manufacturer);
                    }
                    if (controller != null) {
                        switch (viewMode) {
                            case VIEW_HOME:
                                controller.updateViewHome();
                                break;
                            case VIEW_SERVICES:
                                controller.updateViewServices();
                                break;
                            case VIEW_TERMINAL:
                                controller.updateViewTerminal();
                                break;
                        }
                    }
                }
            });
        }
    }

    private void getNameByMac(final String mac_address) {
        String[] mac_address_octets = mac_address.toUpperCase().split(":");
        String server_url = "https://api.macvendors.com/" + mac_address_octets[0] + ":" + mac_address_octets[1] + ":" + mac_address_octets[2];
        final StringRequest stringRequest = new StringRequest(Request.Method.GET, server_url,
                (response) -> {
                    if (response.equals("{\"errors\":{\"detail\":\"Page not found\"}}"))
                        updateDeviceManufacturer(resources.getString(R.string.unknown), mac_address);
                    else
                        updateDeviceManufacturer(response, mac_address);
                },
                (error) -> {
                    if (error != null && error.networkResponse != null) {
                        if (error.networkResponse.statusCode == 404) {
                            updateDeviceManufacturer(resources.getString(R.string.unknown), mac_address);
                        }
                    }
                }

        );
        RequestQueue queue = Volley.newRequestQueue(StartApplication.getInstance().getApplicationContext());
        queue.add(stringRequest);
    }

    @Override
    public void update(Observable o, Object arg) {
        String observable_name = o.getClass().getSimpleName();
        if (observable_name.equals("BluetoothDriver")) {
            if (controller != null) {
                BluetoothData bluetoothData = (BluetoothData) arg;
                switch (bluetoothData.getType()) {
                    case BluetoothData.TYPE_BLUETOOTH_PAIRED:
                        isActionPairDevice = true;
                        if (viewMode == VIEW_HOME) {
                            controller.updateViewHome();
                            new SnackBar().ShowShort(resources.getString(R.string.bluetooth_successful_pairing));
                        }
                        break;
                    case BluetoothData.TYPE_BLUETOOTH_ERROR_PAIRED:
                        isActionPairDevice = true;
                        if (viewMode == VIEW_HOME) {
                            controller.updateViewHome();
                            new SnackBar().ShowLong(resources.getString(R.string.bluetooth_error_paired_device));
                        }
                        break;
                    case BluetoothData.TYPE_BLUETOOTH_UNPAIRED:
                        isActionPairDevice = true;
                        if (viewMode == VIEW_HOME) {
                            controller.updateViewHome();
                            new SnackBar().ShowShort(resources.getString(R.string.bluetooth_successful_unpairing));
                        }
                        break;
                    case BluetoothData.TYPE_BLUETOOTH_ERROR_UNPAIRED:
                        isActionPairDevice = true;
                        if (viewMode == VIEW_HOME) {
                            controller.updateViewHome();
                            new SnackBar().ShowLong(resources.getString(R.string.bluetooth_error_unpaired_device));
                        }
                        break;
                    case BluetoothData.TYPE_NEW_CLC_DISCOVERY_SDP_UUIDS:
                        HashMap<String, String> dataMap = bluetoothData.getDataMap();
                        if (dbDriver.isDeviceExist(dataMap.get("address"))) {
                            dbDriver.updateDeviceSDPServices(dataMap.get("address"), dataMap.get("serviceUUIDs"));
                        }
                        if (device_address.equals(dataMap.get("address"))) {
                            device_service_uuids = dataMap.get("serviceUUIDs");
                        }
                        break;
                    case BluetoothData.TYPE_TERMINAL_LOG_MESSAGE:
                        if (btDriver.isRfCommValid()) terminalConnectState = 2;
                        else terminalConnectState = 0;
                        switch (terminalServiceUUID) {
                            case "00001101-0000-1000-8000-00805f9b34fb":
                                terminalLogSPP = "log> " + bluetoothData.getMessage() + "\n" + terminalLogSPP;
                                break;
                            case "00001103-0000-1000-8000-00805f9b34fb":
                                terminalLogDialUp = "log> " + bluetoothData.getMessage() + "\n" + terminalLogDialUp;
                                break;
                            default:
                                terminalLogMyService = "log> " + bluetoothData.getMessage() + "\n" + terminalLogMyService;
                                break;
                        }
                        if (viewMode == VIEW_TERMINAL) {
                            controller.updateViewTerminal();
                        }
                        break;
                    case BluetoothData.TYPE_TERMINAL_DATA:
                        String dataMessage;
                        if (btDriver.isRfCommValid()) terminalConnectState = 2;
                        else terminalConnectState = 0;
                        if (sharedPreferences.getString(SettingsFragment.KEY_TERMINAL_INPUT_FORMAT, "Text").equals("Text")) {
                            dataMessage = (new String(bluetoothData.getTerminalData())).replaceAll("\r", "<CR>").replaceAll("\n", "<LF>");
                        } else {
                            byte[] dataBytes = bluetoothData.getTerminalData();
                            StringBuilder dataBytesString = new StringBuilder();
                            for (byte b : dataBytes) {
                                dataBytesString.append(String.format("%02x", b));
                                dataBytesString.append(" ");
                            }
                            dataMessage = dataBytesString.toString().trim().toUpperCase();
                        }
                        if (terminalServiceUUID.equals("00001101-0000-1000-8000-00805f9b34fb") && !dataMessage.isEmpty()) {
                            terminalLogSPP = "in > " + dataMessage + "\n" + terminalLogSPP;
                        } else if (terminalServiceUUID.equals("00001103-0000-1000-8000-00805f9b34fb") && !dataMessage.isEmpty()) {
                            terminalLogDialUp = "in > " + dataMessage + "\n" + terminalLogDialUp;
                        } else {
                            terminalLogMyService = "in > " + dataMessage + "\n" + terminalLogMyService;
                        }
                        if (viewMode == VIEW_TERMINAL) {
                            controller.updateViewTerminal();
                        }
                        break;
                    default:
                        break;
                }
            }
        } else if (observable_name.equals("DatabaseDriver")) {
            DatabaseMessage databaseMessage = (DatabaseMessage) arg;
            if (databaseMessage.getType() == DatabaseMessage.TYPE_NEW_DATA) {
                if (controller != null) {
                    getDevice(deviceId);
                    switch (viewMode) {
                        case DeviceDetailsModel.VIEW_HOME:
                            controller.updateViewHome();
                            break;
                        case DeviceDetailsModel.VIEW_SERVICES:
                            controller.updateViewServices();
                            break;
                        case DeviceDetailsModel.VIEW_TERMINAL:
                            controller.updateViewTerminal();
                            break;
                    }
                    if (databaseMessage.getMessage() != null)
                        new SnackBar().ShowShort(databaseMessage.getMessage());
                }
            }
        }
    }

    void timeoutPairDevice() {
        if (runnablePairDevice != null) {
            handler.removeCallbacks(runnablePairDevice);
        }
        isActionPairDevice = false;
        runnablePairDevice = () -> {
            if (!isActionPairDevice) {
                if (controller != null && viewMode == VIEW_HOME)
                    new SnackBar().ShowLong(resources.getString(R.string.device_is_not_responding));
            }
        };
        handler.postDelayed(runnablePairDevice, 20000);
        if (controller != null && viewMode == VIEW_HOME) {
            Snackbar.make(controller.requireActivity().findViewById(R.id.cl), resources.getString(R.string.waiting_for_response_device), Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.stop, (v) -> {
                        isActionPairDevice = true;
                        if (runnablePairDevice != null) {
                            handler.removeCallbacks(runnablePairDevice);
                            runnablePairDevice = null;
                        }
                    }).show();
        }
    }

    void stopGetGattServices(boolean logging) {
        for (Runnable runnable : runnableArrayList) {
            if (runnable != null) handler.removeCallbacks(runnable);
        }
        btDriver.stopGattClient();
        gattConnectionState = 0;
        if (logging) {
            gattConnectionLog += resources.getString(R.string.gcl_stop_gatt_connection) + "\n";
            if (controller != null && viewMode == DeviceDetailsModel.VIEW_SERVICES) {
                controller.updateViewServices();
            }
        }
    }

    void getGattServices() {
        gattConnectionLog = resources.getString(R.string.gcl_start_gatt_connection) + "\n";
        for (Runnable runnable : runnableArrayList) {
            if (runnable != null) handler.removeCallbacks(runnable);
        }
        runnableArrayList.clear();
        btDriver.stopGattClient();
        gattConnectionState = 1;
        if (gattServices != null) gattServices.clear();
        gattServices = null;
        ServicesList.clear();
        CharacteristicsList.clear();
        DescriptorsList.clear();
        bluetoothGattDescriptors.clear();
        gattCharacteristicsData.clear();
        gattDiscoverServicesAttempts = 0;
        readCharacteristicsData = sharedPreferences.getBoolean(SettingsFragment.KEY_DEVICES_DETAILS_READING_GATT_CHARACTERISTICS, true);
        gattAutoConnect = sharedPreferences.getBoolean(SettingsFragment.KEY_GATT_AUTO_CONNECT, false);
        gattReadTimeout = Integer.parseInt(sharedPreferences.getString(SettingsFragment.KEY_GATT_READING_TIMEOUT, "200"));
        selectedServiceNumber = -1;
        selectedCharacteristicNumber = -1;
        selectedDescriptorNumber = -1;
        runnableObject = () -> {
            btDriver.stopGattClient();
            gattConnectionLog += resources.getString(R.string.gcl_gatt_server_is_not_responding) + "\n";
            gattConnectionLog += resources.getString(R.string.gcl_stop_gatt_connection) + "\n";
            gattConnectionState = 0;
            if (controller != null && viewMode == DeviceDetailsModel.VIEW_SERVICES) {
                controller.updateViewServices();
            }
        };
        handler.postDelayed(runnableObject, 10000);
        runnableArrayList.add(runnableObject);
        handler.post(() -> btDriver.startGattClient(device_address, bluetoothCattCallback, gattAutoConnect));
        if (controller != null && viewMode == DeviceDetailsModel.VIEW_SERVICES) {
            controller.updateViewServices();
        }
    }

    private final BluetoothGattCallback bluetoothCattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                for (Runnable runnable : runnableArrayList)
                    if (runnable != null) handler.removeCallbacks(runnable);
                gattConnectionLog += resources.getString(R.string.gcl_connected_to_gatt_server) + "\n";
                gattConnectionLog += resources.getString(R.string.gcl_discovering_gatt_services) + "\n";
                if (controller != null) {
                    controller.requireActivity().runOnUiThread(() -> {
                        if (viewMode == DeviceDetailsModel.VIEW_SERVICES) {
                            controller.updateViewServices();
                        }
                    });
                }
                gattDiscoverServices(gatt);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                for (Runnable runnable : runnableArrayList)
                    if (runnable != null) handler.removeCallbacks(runnable);
                gattConnectionLog += resources.getString(R.string.gcl_disconnected_from_gatt_server) + "\n";
                gattConnectionLog += resources.getString(R.string.gcl_stop_gatt_connection) + "\n";
                makeServicesList();
                btDriver.stopGattClient();
                gattConnectionState = 0;
                if (controller != null) {
                    controller.requireActivity().runOnUiThread(() -> {
                        if (viewMode == DeviceDetailsModel.VIEW_SERVICES) {
                            controller.updateViewServices();
                        }
                    });
                }
            } else {
                for (Runnable runnable : runnableArrayList)
                    if (runnable != null) handler.removeCallbacks(runnable);
                gattConnectionLog += resources.getString(R.string.gcl_error_connecting_to_gatt_server_csc) + newState + ").\n";
                gattConnectionLog += resources.getString(R.string.gcl_stop_gatt_connection) + "\n";
                btDriver.stopGattClient();
                gattConnectionState = 0;
                if (controller != null) {
                    controller.requireActivity().runOnUiThread(() -> {
                        if (viewMode == DeviceDetailsModel.VIEW_SERVICES) {
                            controller.updateViewServices();
                        }
                    });
                }
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (Runnable runnable : runnableArrayList)
                    if (runnable != null) handler.removeCallbacks(runnable);
                gattServices = gatt.getServices();
                gattConnectionLog += resources.getString(R.string.gcl_discovered_services) + gattServices.size() + ".\n";
                if (controller != null) {
                    controller.requireActivity().runOnUiThread(() -> {
                        if (viewMode == DeviceDetailsModel.VIEW_SERVICES) {
                            controller.updateViewServices();
                        }
                    });
                }
                if (readCharacteristicsData) getGattCharacteristics(gatt);
                else {
                    gattConnectionState = 2;
                    makeServicesList();
                }
            } else if (status == 129 || status == 133) {
                if (++gattDiscoverServicesAttempts >= 5) gatt.disconnect();
                else gatt.discoverServices();
            } else {
                for (Runnable runnable : runnableArrayList)
                    if (runnable != null) handler.removeCallbacks(runnable);
                gattConnectionLog += resources.getString(R.string.gcl_error_get_gatt_services_sd) + status + ").\n";
                gattConnectionLog += resources.getString(R.string.gcl_stop_gatt_connection) + "\n";
                btDriver.stopGattClient();
                gattConnectionState = 0;
                if (controller != null) {
                    controller.requireActivity().runOnUiThread(() -> {
                        if (viewMode == DeviceDetailsModel.VIEW_SERVICES) {
                            controller.updateViewServices();
                        }
                    });
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                HashMap<String, String> data = decodeCharacteristic(characteristic);
                Objects.requireNonNull(gattCharacteristicsData.get(characteristic.getUuid().toString())).putAll(data);
                makeServicesList();
                new SnackBar().ShowShort(resources.getString(R.string.gatt_sending_completed_successfully));
            } else
                new SnackBar().ShowShort(resources.getString(R.string.gatt_unsuccessful_sending));
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                new SnackBar().ShowShort(resources.getString(R.string.gatt_sending_completed_successfully));
            else
                new SnackBar().ShowShort(resources.getString(R.string.gatt_unsuccessful_sending));
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                new SnackBar().ShowLong(resources.getString(R.string.device_rssi) + " " + rssi + " " + resources.getString(R.string.dBm));
            else
                new SnackBar().ShowLong(resources.getString(R.string.gatt_unsuccessful_sending));
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                new SnackBar().ShowLong(resources.getString(R.string.device_new_mtu_value) + " " + mtu);
            else
                new SnackBar().ShowLong(resources.getString(R.string.gatt_unsuccessful_sending));
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            HashMap<String, String> data = decodeCharacteristic(characteristic);
            Objects.requireNonNull(gattCharacteristicsData.get(characteristic.getUuid().toString())).putAll(data);
            makeServicesList();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                decodeDescriptor(descriptor);
                makeServicesList();
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                decodeDescriptor(descriptor);
                makeServicesList();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                HashMap<String, String> data = decodeCharacteristic(characteristic);
                List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
                bluetoothGattDescriptors.addAll(descriptors);
                if (gattCharacteristicsData.containsKey(characteristic.getUuid().toString()))
                    Objects.requireNonNull(gattCharacteristicsData.get(characteristic.getUuid().toString())).putAll(data);
                else
                    gattCharacteristicsData.put(characteristic.getUuid().toString(), data);
            }
        }

    };

    private HashMap<String, String> decodeCharacteristic(BluetoothGattCharacteristic characteristic) {
        String uuid = characteristic.getUuid().toString();
        HashMap<String, String> data = new HashMap<>();
        data.put("PROPERTIES", getPropertiesDescription(characteristic.getProperties()));
        boolean isFindCharacteristics = false;
        if (BluetoothGattUUIDs.characteristicsUUIDs.containsKey(uuid)) {
            if ("Appearance".equals(BluetoothGattUUIDs.characteristicsUUIDs.get(uuid))) {
                int valueAppearance = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                String stringAppearance = BluetoothAppearance.getAppearance(valueAppearance);
                if (stringAppearance.equals(resources.getString(R.string.unknown))) {
                    data.put("VALUE", "0x" + String.format("%04X", valueAppearance));
                } else {
                    data.put("VALUE", "0x" + String.format("%04X", valueAppearance) + " (" + stringAppearance + ")");
                }
                isFindCharacteristics = true;
            }
            /* Decode others characteristics */
        }
        if (!isFindCharacteristics) {
            byte[] valueBytes = characteristic.getValue();
            StringBuilder valueHex = null;
            if (valueBytes != null && valueBytes.length > 0) {
                valueHex = new StringBuilder(valueBytes.length);
                for (byte byteChar : valueBytes) valueHex.append(String.format("%02X", byteChar));
            }
            if (valueHex != null) {
                String valueString = new String(valueBytes);
                if (valueString.replaceAll("[\\p{C}]", "").length() != valueString.length())
                    data.put("VALUE", "0x" + valueHex);
                else data.put("VALUE", "0x" + valueHex + " (" + valueString + ")");
            } else data.put("VALUE", "");
        }
        return data;
    }

    private void decodeDescriptor(BluetoothGattDescriptor descriptor) {
        String characteristicUUID = descriptor.getCharacteristic().getUuid().toString();
        if (DescriptorsList.get(characteristicUUID) != null) {
            boolean isFindDescriptor = false;
            ArrayList<HashMap<String, String>> arrayListDescriptors = DescriptorsList.get(characteristicUUID);
            if (arrayListDescriptors != null) {
                for (HashMap<String, String> hashMapDescriptor : arrayListDescriptors) {
                    if (Objects.equals(hashMapDescriptor.get("UUID"), descriptor.getUuid().toString())) {
                        isFindDescriptor = true;
                        byte[] valueBytes = descriptor.getValue();
                        StringBuilder valueHex = null;
                        if (valueBytes != null && valueBytes.length > 0) {
                            valueHex = new StringBuilder(valueBytes.length);
                            for (byte byteChar : valueBytes)
                                valueHex.append(String.format("%02X", byteChar));
                        }
                        if (valueHex != null) {
                            String valueString = new String(valueBytes);
                            if (valueString.replaceAll("[\\p{C}]", "").length() != valueString.length())
                                hashMapDescriptor.put("VALUE", "0x" + valueHex);
                            else
                                hashMapDescriptor.put("VALUE", "0x" + valueHex + " (" + valueString + ")");
                        } else hashMapDescriptor.put("VALUE", "");
                        break;
                    }
                }
                if (!isFindDescriptor) {
                    if (descriptor.getUuid() != null) {
                        String uuidDescriptor = descriptor.getUuid().toString();
                        String nameDescriptor;
                        nameDescriptor = BluetoothGattUUIDs.descriptorsUUIDs.getOrDefault(uuidDescriptor, "");
                        HashMap<String, String> hashMapDescriptor = new HashMap<>();
                        hashMapDescriptor.put("UUID", uuidDescriptor);
                        hashMapDescriptor.put("NAME", nameDescriptor);
                        byte[] valueBytes = descriptor.getValue();
                        StringBuilder valueHex = null;
                        if (valueBytes != null && valueBytes.length > 0) {
                            valueHex = new StringBuilder(valueBytes.length);
                            for (byte byteChar : valueBytes)
                                valueHex.append(String.format("%02X", byteChar));
                        }
                        if (valueHex != null) {
                            String valueString = new String(valueBytes);
                            if (valueString.replaceAll("[\\p{C}]", "").length() != valueString.length())
                                hashMapDescriptor.put("VALUE", "0x" + valueHex);
                            else
                                hashMapDescriptor.put("VALUE", "0x" + valueHex + " (" + valueString + ")");
                        } else hashMapDescriptor.put("VALUE", "");
                        arrayListDescriptors.add(hashMapDescriptor);
                    }
                }
            }
        } else {
            if (descriptor.getUuid() != null) {
                String uuidDescriptor = descriptor.getUuid().toString();
                String nameDescriptor;
                nameDescriptor = BluetoothGattUUIDs.descriptorsUUIDs.getOrDefault(uuidDescriptor, "");
                HashMap<String, String> hashMapDescriptor = new HashMap<>();
                hashMapDescriptor.put("UUID", uuidDescriptor);
                hashMapDescriptor.put("NAME", nameDescriptor);
                byte[] valueBytes = descriptor.getValue();
                StringBuilder valueHex = null;
                if (valueBytes != null && valueBytes.length > 0) {
                    valueHex = new StringBuilder(valueBytes.length);
                    for (byte byteChar : valueBytes)
                        valueHex.append(String.format("%02X", byteChar));
                }
                if (valueHex != null) {
                    String valueString = new String(valueBytes);
                    if (valueString.replaceAll("[\\p{C}]", "").length() != valueString.length())
                        hashMapDescriptor.put("VALUE", "0x" + valueHex);
                    else
                        hashMapDescriptor.put("VALUE", "0x" + valueHex + " (" + valueString + ")");
                } else hashMapDescriptor.put("VALUE", "");
                ArrayList<HashMap<String, String>> arrayListDescriptors = new ArrayList<>();
                arrayListDescriptors.add(hashMapDescriptor);
                DescriptorsList.put(characteristicUUID, arrayListDescriptors);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void gattDiscoverServices(final BluetoothGatt gatt) {
        for (Runnable runnable : runnableArrayList)
            if (runnable != null) handler.removeCallbacks(runnable);
        runnableObject = () -> {
            btDriver.stopGattClient();
            gattConnectionState = 0;
            gattConnectionLog += resources.getString(R.string.gcl_gatt_server_is_not_responding) + "\n";
            gattConnectionLog += resources.getString(R.string.gcl_disconnected_from_gatt_server) + "\n";
            gattConnectionLog += resources.getString(R.string.gcl_stop_gatt_connection) + "\n";
            if (controller != null && viewMode == DeviceDetailsModel.VIEW_SERVICES) {
                controller.updateViewServices();
            }
        };
        handler.postDelayed(runnableObject, 2000);
        runnableArrayList.add(runnableObject);
        gatt.discoverServices();
    }

    @SuppressLint("MissingPermission")
    private void getGattCharacteristics(final BluetoothGatt gatt) {
        gattConnectionLog += resources.getString(R.string.gcl_reading_gatt_characteristics) + "\n";
        if (controller != null) {
            controller.requireActivity().runOnUiThread(() -> {
                if (viewMode == DeviceDetailsModel.VIEW_SERVICES) {
                    controller.updateViewServices();
                }
            });
        }
        for (Runnable runnable : runnableArrayList)
            if (runnable != null) handler.removeCallbacks(runnable);
        int allCharacteristics = 0;
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                ++allCharacteristics;
                runnableObject = () -> gatt.readCharacteristic(gattCharacteristic);
                handler.postDelayed(runnableObject, (long) gattReadTimeout * allCharacteristics);
                runnableArrayList.add(runnableObject);
            }
        }
        runnableObject = () -> getGattDescriptors(gatt);
        handler.postDelayed(runnableObject, (long) gattReadTimeout * (allCharacteristics + 1));
        runnableArrayList.add(runnableObject);
        gattConnectionLog += resources.getString(R.string.gcl_discovered_characteristics) + allCharacteristics + ".\n";
        if ((gattReadTimeout / 1000.0) * (allCharacteristics + 1) >= 1)
            gattConnectionLog += resources.getString(R.string.gcl_waiting_for) + (gattReadTimeout / 1000.0) * (allCharacteristics + 1) + " " + resources.getString(R.string.sec) + "\n";
        else
            gattConnectionLog += resources.getString(R.string.gcl_waiting_for) + gattReadTimeout * (allCharacteristics + 1) + " " + resources.getString(R.string.ms) + "\n";
        if (controller != null) {
            controller.requireActivity().runOnUiThread(() -> {
                if (viewMode == DeviceDetailsModel.VIEW_SERVICES) {
                    controller.updateViewServices();
                }
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void getGattDescriptors(final BluetoothGatt gatt) {
        int allDescriptors = bluetoothGattDescriptors.size();
        if (allDescriptors > 0) {
            gattConnectionLog += resources.getString(R.string.gcl_reading_gatt_descriptors) + "\n";
            gattConnectionLog += resources.getString(R.string.gcl_discovered_descriptors) + allDescriptors + ".\n";
            if ((gattReadTimeout / 1000.0) * (allDescriptors + 1) >= 1)
                gattConnectionLog += resources.getString(R.string.gcl_waiting_for) + (gattReadTimeout / 1000.0) * (allDescriptors + 1) + " " + resources.getString(R.string.sec) + "\n";
            else
                gattConnectionLog += resources.getString(R.string.gcl_waiting_for) + gattReadTimeout * (allDescriptors + 1) + " " + resources.getString(R.string.ms) + "\n";
            if (controller != null) {
                controller.requireActivity().runOnUiThread(() -> {
                    if (viewMode == DeviceDetailsModel.VIEW_SERVICES) {
                        controller.updateViewServices();
                    }
                });
            }
            int currentDescriptor = 0;
            for (final BluetoothGattDescriptor descriptor : bluetoothGattDescriptors) {
                ++currentDescriptor;
                runnableObject = () -> gatt.readDescriptor(descriptor);
                handler.postDelayed(runnableObject, (long) gattReadTimeout * currentDescriptor);
                runnableArrayList.add(runnableObject);
            }
            bluetoothGattDescriptors.clear();
            runnableObject = () -> {
                gattConnectionState = 2;
                makeServicesList();
            };
            handler.postDelayed(runnableObject, (long) gattReadTimeout * (currentDescriptor + 1));
            runnableArrayList.add(runnableObject);
        } else {
            gattConnectionState = 2;
            makeServicesList();
        }
    }

    @SuppressLint("MissingPermission")
    void readRemoteRssi() {
        final BluetoothGatt gatt = btDriver.getGattClient();
        if (gatt == null) {
            new SnackBar().ShowLong(resources.getString(R.string.device_details_services_no_gatt_connection));
            return;
        }
        if (!gatt.readRemoteRssi())
            new SnackBar().ShowShort(resources.getString(R.string.gatt_unsuccessful_sending));
    }

    @SuppressLint("MissingPermission")
    void requestNewMtu(int mtu) {
        final BluetoothGatt gatt = btDriver.getGattClient();
        if (gatt == null) {
            new SnackBar().ShowLong(resources.getString(R.string.device_details_services_no_gatt_connection));
            return;
        }
        if (!gatt.requestMtu(mtu))
            new SnackBar().ShowLong(resources.getString(R.string.gatt_unsuccessful_sending));

    }

    @SuppressLint("MissingPermission")
    void requestConnectionPriority(int connectionPriority) {
        final BluetoothGatt gatt = btDriver.getGattClient();
        if (gatt == null) {
            new SnackBar().ShowLong(resources.getString(R.string.device_details_services_no_gatt_connection));
            return;
        }
        switch (connectionPriority) {
            case 0:
                if (gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED))
                    new SnackBar().ShowShort(resources.getString(R.string.gatt_sending_completed_successfully));
                else
                    new SnackBar().ShowLong(resources.getString(R.string.gatt_unsuccessful_sending));
                break;
            case 1:
                if (gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH))
                    new SnackBar().ShowShort(resources.getString(R.string.gatt_sending_completed_successfully));
                else
                    new SnackBar().ShowLong(resources.getString(R.string.gatt_unsuccessful_sending));
                break;
            case 2:
                if (gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER))
                    new SnackBar().ShowShort(resources.getString(R.string.gatt_sending_completed_successfully));
                else
                    new SnackBar().ShowLong(resources.getString(R.string.gatt_unsuccessful_sending));
                break;
        }
    }

    @SuppressLint("MissingPermission")
    void getGattCharacteristic(String serviceUUID, String characteristicUUID) {
        final BluetoothGatt gatt = btDriver.getGattClient();
        if (gatt == null) {
            new SnackBar().ShowLong(resources.getString(R.string.device_details_services_no_gatt_connection));
            return;
        }
        gattConnectionLog += resources.getString(R.string.gcl_reading_gatt_characteristic) + "\n";
        gattConnectionLog += characteristicUUID + "\n";
        if (controller != null) {
            controller.requireActivity().runOnUiThread(() -> {
                if (viewMode == DeviceDetailsModel.VIEW_SERVICES) {
                    controller.updateViewServices();
                }
            });
        }
        gatt.readCharacteristic(gatt.getService(UUID.fromString(serviceUUID)).getCharacteristic(UUID.fromString(characteristicUUID)));
        runnableObject = () -> getGattDescriptors(gatt);
        handler.postDelayed(runnableObject, gattReadTimeout);
        runnableArrayList.add(runnableObject);
        gattConnectionState = 1;
        if (gattReadTimeout / 1000.0 >= 1)
            gattConnectionLog += resources.getString(R.string.gcl_waiting_for) + (gattReadTimeout / 1000.0) + " " + resources.getString(R.string.sec) + "\n";
        else
            gattConnectionLog += resources.getString(R.string.gcl_waiting_for) + gattReadTimeout + " " + resources.getString(R.string.ms) + "\n";
        if (controller != null) {
            controller.requireActivity().runOnUiThread(() -> {
                if (viewMode == DeviceDetailsModel.VIEW_SERVICES) {
                    controller.updateViewServices();
                }
            });
        }
    }

    @SuppressLint("MissingPermission")
    void setGattCharacteristic(String serviceUUID, String characteristicUUID, byte[] value) {
        final BluetoothGatt gatt = btDriver.getGattClient();
        if (gatt == null) {
            new SnackBar().ShowLong(resources.getString(R.string.device_details_services_no_gatt_connection));
            return;
        }
        BluetoothGattCharacteristic characteristic = gatt.getService(UUID.fromString(serviceUUID)).getCharacteristic(UUID.fromString(characteristicUUID));
        characteristic.setValue(value);
        if (gatt.writeCharacteristic(characteristic))
            gatt.executeReliableWrite();
        else
            new SnackBar().ShowShort(resources.getString(R.string.gatt_unsuccessful_sending));
    }

    @SuppressLint("MissingPermission")
    void setCharacteristicNotification(String serviceUUID, String characteristicUUID, boolean isEnable) {
        final BluetoothGatt gatt = btDriver.getGattClient();
        if (gatt == null) {
            new SnackBar().ShowLong(resources.getString(R.string.device_details_services_no_gatt_connection));
            return;
        }
        BluetoothGattCharacteristic characteristic = gatt.getService(UUID.fromString(serviceUUID)).getCharacteristic(UUID.fromString(characteristicUUID));
        if (gatt.setCharacteristicNotification(characteristic, isEnable)) {
            if (gattCharacteristicsData.containsKey(characteristicUUID)) {
                Objects.requireNonNull(gattCharacteristicsData.get(characteristicUUID)).put("NOTIFY", isEnable ? "enable" : "disable");
            } else {
                HashMap<String, String> data = new HashMap<>();
                data.put("NOTIFY", isEnable ? "enable" : "disable");
                gattCharacteristicsData.put(characteristicUUID, data);
            }
            makeServicesList();
            new SnackBar().ShowShort(resources.getString(R.string.gatt_sending_completed_successfully));
        } else
            new SnackBar().ShowShort(resources.getString(R.string.gatt_unsuccessful_sending));
    }

    @SuppressLint("MissingPermission")
    void setGattDescriptor(String serviceUUID, String characteristicUUID, String descriptorUUID, byte[] value) {
        final BluetoothGatt gatt = btDriver.getGattClient();
        if (gatt == null) {
            new SnackBar().ShowLong(resources.getString(R.string.device_details_services_no_gatt_connection));
            return;
        }
        BluetoothGattDescriptor descriptor = gatt.getService(UUID.fromString(serviceUUID)).getCharacteristic(UUID.fromString(characteristicUUID)).getDescriptor(UUID.fromString(descriptorUUID));
        descriptor.setValue(value);
        if (gatt.writeDescriptor(descriptor)) {
            new SnackBar().ShowShort(resources.getString(R.string.gatt_sending_completed_successfully));
        } else
            new SnackBar().ShowShort(resources.getString(R.string.gatt_unsuccessful_sending));
    }

    private void makeServicesList() {
        if (controller != null) {
            controller.requireActivity().runOnUiThread(() -> {
                if (gattServices != null) {
                    HashMap<String, String> serviceHashMap;
                    HashMap<String, String> characteristicsHashMap;
                    String uuid;
                    for (BluetoothGattService gattService : gattServices) {
                        uuid = gattService.getUuid().toString();
                        boolean isFindService = false;
                        int serviceCounter = 0;
                        for (HashMap<String, String> service : ServicesList) {
                            if (Objects.equals(service.get("UUID"), uuid)) {
                                isFindService = true;
                                break;
                            }
                            ++serviceCounter;
                        }
                        if (!isFindService) {
                            serviceHashMap = new HashMap<>();
                            serviceHashMap.put("UUID", uuid);
                            ServicesList.add(serviceHashMap);
                        }
                        List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                        ArrayList<HashMap<String, String>> gattGroupCharacteristics = new ArrayList<>();
                        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                            characteristicsHashMap = new HashMap<>();
                            uuid = gattCharacteristic.getUuid().toString();
                            characteristicsHashMap.put("UUID", uuid);
                            try {
                                Objects.requireNonNull(gattCharacteristicsData.get(uuid));
                                characteristicsHashMap.put("PROPERTIES", getPropertiesDescription(gattCharacteristic.getProperties()));
                                if (Objects.requireNonNull(gattCharacteristicsData.get(uuid)).get("PROPERTIES") != null) {
                                    characteristicsHashMap.put("PROPERTIES", Objects.requireNonNull(gattCharacteristicsData.get(uuid)).get("PROPERTIES"));
                                }
                                characteristicsHashMap.put("VALUE", Objects.requireNonNull(gattCharacteristicsData.get(uuid)).get("VALUE"));
                                characteristicsHashMap.put("NOTIFY", Objects.requireNonNull(gattCharacteristicsData.get(uuid)).getOrDefault("NOTIFY", "disable"));
                                characteristicsHashMap.put("DESCRIPTORS", Objects.requireNonNull(gattCharacteristicsData.get(uuid)).get("DESCRIPTORS"));
                                gattGroupCharacteristics.add(characteristicsHashMap);
                            } catch (NullPointerException ignored) {

                            }
                        }
                        if (serviceCounter == CharacteristicsList.size())
                            CharacteristicsList.add(gattGroupCharacteristics);
                        else
                            CharacteristicsList.add(serviceCounter, gattGroupCharacteristics);
                    }
                }
                if (MainActivityModel.getInstance().lastLocation != null) {
                    device_longitude = MainActivityModel.getInstance().lastLocation.getLongitude();
                    device_latitude = MainActivityModel.getInstance().lastLocation.getLatitude();
                    if (dbDriver.isDeviceExist(device_address)) {
                        if (deviceId == -1) {
                            Cursor cursor;
                            cursor = dbDriver.rawQuery("SELECT * FROM " + DatabaseDriver.DatabaseContract.Devices.TABLE_NAME + " WHERE " + DatabaseDriver.DatabaseContract.Devices.ADDRESS + " = '" + device_address + "';");
                            cursor.moveToFirst();
                            deviceId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseDriver.DatabaseContract.Devices._ID));
                            cursor.close();
                        }
                        dbDriver.updateDeviceLocation(deviceId, device_longitude, device_latitude);
                    }
                }
                if (viewMode == DeviceDetailsModel.VIEW_SERVICES) {
                    if (ServicesList.size() > 0) {
                        if (device_details_services_view.equals(resources.getString(R.string.device_details_services_show_gatt_log)))
                            device_details_services_view = resources.getString(R.string.device_details_services_radio_button_gatt_services);
                    }
                    controller.updateViewServices();
                }
            });
        }
    }

    private String getPropertiesDescription(int properties) {
        String description;
        String propertyDescription = "";
        if ((properties & BluetoothGattCharacteristic.PROPERTY_BROADCAST) == BluetoothGattCharacteristic.PROPERTY_BROADCAST)
            propertyDescription += "Broadcast, ";
        if ((properties & BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) == BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS)
            propertyDescription += "Extended props, ";
        if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) == BluetoothGattCharacteristic.PROPERTY_INDICATE)
            propertyDescription += "Indicate, ";
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == BluetoothGattCharacteristic.PROPERTY_NOTIFY)
            propertyDescription += "Notify, ";
        if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) == BluetoothGattCharacteristic.PROPERTY_READ)
            propertyDescription += "Read, ";
        if ((properties & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) == BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE)
            propertyDescription += "Signed write, ";
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) == BluetoothGattCharacteristic.PROPERTY_WRITE)
            propertyDescription += "Write, ";
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
            propertyDescription += "Write no response, ";
        if (propertyDescription.isEmpty()) description = "0x" + String.format("%02X", properties);
        else {
            propertyDescription += ";";
            description = "0x" + String.format("%02X", properties) + " (" + propertyDescription.replace(", ;", "") + ")";
        }
        return description;
    }
}
