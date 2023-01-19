package ru.vukit.btm.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Parcelable;

import androidx.annotation.Keep;
import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Observable;
import java.util.Set;
import java.util.UUID;

import ru.vukit.btm.MainActivityModel;
import ru.vukit.btm.R;
import ru.vukit.btm.SettingsFragment;
import ru.vukit.btm.StartApplication;

@Keep
@SuppressLint({"MissingPermission"})
@SuppressWarnings({"rawtypes", "JavaReflectionMemberAccess", "deprecation"})
public
class BluetoothDriver extends Observable {

    private static final int BLUETOOTH_ALL_MODE = -1;
    public static final int BLUETOOTH_CLC_MODE = 0;
    public static final int BLUETOOTH_LE_MODE = 1;
    private static final int rssiIsNotSet = 1;

    private final Resources resources = StartApplication.getInstance().getResources();
    private final BluetoothManager bluetoothManager = (BluetoothManager) StartApplication.getInstance().getSystemService(Context.BLUETOOTH_SERVICE);
    private final BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

    private Intent brBluetoothActionStateChanged;
    private Intent brBluetoothActionFound;
    private Intent brBluetoothActionUUID;
    private Intent brBluetoothActionBondStateChanged;

    private boolean runDiscovery;

    private BluetoothGatt bluetoothGattClient;

    private final Handler handlerCLCScanner = new Handler();
    private Runnable runnableCLCScanner;
    private final Handler handlerLeScanner = new Handler();
    private Runnable runnableLeScanner;
    private final Handler handlerRfCommConnect = new Handler();
    private final Handler handlerRfCommRead = new Handler();

    private int minRssi = rssiIsNotSet;
    private final int maxRssi = rssiIsNotSet;
    private String searchStringFilter = "";
    private String searchCondition = resources.getString(R.string.radio_button_none);

    private static BluetoothDriver INSTANCE = null;

    public static synchronized BluetoothDriver getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BluetoothDriver();
        }
        return (INSTANCE);
    }

    public boolean isLeSupported() {
        return StartApplication.getInstance().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public boolean isEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    public void Enable() {
        mBluetoothAdapter.enable();
    }

    public void Disable() {
        mBluetoothAdapter.disable();
    }

    private int getDiscoveryDuration() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(StartApplication.getInstance());
        return Integer.parseInt(sharedPreferences.getString(SettingsFragment.KEY_DISCOVERY_DURATION, StartApplication.getInstance().getString(R.string.sec30)).split(" ")[0]);
    }

    private BluetoothSocket rfBluetoothSocket = null;
    private InputStream rfBluetoothInStream = null;
    private OutputStream rfBluetoothOutStream = null;
    private int rfReadLoopState = 0;
    private boolean isManualDisconnect = false;

    public void rfCommConnect(String mac_address, final String service_uuid, final boolean insecure) {
        rfReadLoopState = 0;
        isManualDisconnect = false;
        if (rfBluetoothSocket != null) {
            try {
                rfBluetoothSocket.close();
                rfBluetoothSocket = null;
            } catch (IOException e) {
                rfBluetoothSocket = null;
            }
        }
        stopGattClient();
        stopDiscovery(BLUETOOTH_ALL_MODE);
        final BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(mac_address);
        new Thread(() -> {
            try {
                if (insecure) {
                    try {
                        rfBluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(service_uuid));
                    } catch (IllegalArgumentException ex) {
                        handlerRfCommConnect.post(() -> {
                            if (!isManualDisconnect) {
                                setChanged();
                                notifyObservers(new BluetoothData(BluetoothData.TYPE_TERMINAL_LOG_MESSAGE, resources.getString(R.string.terminal_message_incorrect_your_service_uuid)));
                            }
                        });
                    }
                } else {
                    try {
                        rfBluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(service_uuid));
                    } catch (IllegalArgumentException ex) {
                        handlerRfCommConnect.post(() -> {
                            if (!isManualDisconnect) {
                                setChanged();
                                notifyObservers(new BluetoothData(BluetoothData.TYPE_TERMINAL_LOG_MESSAGE, resources.getString(R.string.terminal_message_incorrect_your_service_uuid)));
                            }
                        });
                    }
                }
            } catch (IOException e) {
                rfBluetoothSocket = null;
                handlerRfCommConnect.post(() -> {
                    if (!isManualDisconnect) {
                        setChanged();
                        notifyObservers(new BluetoothData(BluetoothData.TYPE_TERMINAL_LOG_MESSAGE, resources.getString(R.string.terminal_message_fail_connection)));
                    }
                });
            }
            if (rfBluetoothSocket != null) {
                try {
                    rfBluetoothSocket.connect();
                } catch (IOException connectException) {
                    if (!isManualDisconnect) {
                        try {
                            rfBluetoothSocket.close();
                            rfBluetoothSocket = null;
                        } catch (IOException closeException) {
                            rfBluetoothSocket = null;
                        }
                    }
                }
                if (rfBluetoothSocket != null) {
                    try {
                        rfBluetoothInStream = rfBluetoothSocket.getInputStream();
                    } catch (IOException e) {
                        if (!isManualDisconnect) {
                            try {
                                rfBluetoothSocket.close();
                                rfBluetoothSocket = null;
                            } catch (IOException closeException) {
                                rfBluetoothSocket = null;
                            }
                        }
                    }
                }
                if (rfBluetoothSocket != null) {
                    try {
                        rfBluetoothOutStream = rfBluetoothSocket.getOutputStream();
                    } catch (IOException e) {
                        if (!isManualDisconnect) {
                            try {
                                rfBluetoothSocket.close();
                                rfBluetoothSocket = null;
                            } catch (IOException closeException) {
                                rfBluetoothSocket = null;
                            }
                        }
                    }
                }
                handlerRfCommConnect.post(() -> {
                    if (rfBluetoothSocket != null) {
                        setChanged();
                        notifyObservers(new BluetoothData(BluetoothData.TYPE_TERMINAL_LOG_MESSAGE, resources.getString(R.string.terminal_message_connected)));
                        rfCommRead();
                    } else {
                        if (!isManualDisconnect) {
                            setChanged();
                            notifyObservers(new BluetoothData(BluetoothData.TYPE_TERMINAL_LOG_MESSAGE, resources.getString(R.string.terminal_message_fail_connection)));
                        }
                    }
                });

            }
        }).start();
    }

    public void rfCommDisconnect(boolean logging) {
        isManualDisconnect = true;
        if (rfBluetoothSocket != null) {
            rfReadLoopState = 0;
            try {
                rfBluetoothSocket.close();
                rfBluetoothSocket = null;
            } catch (IOException e) {
                rfBluetoothSocket = null;
            }
        }
        if (logging) {
            setChanged();
            notifyObservers(new BluetoothData(BluetoothData.TYPE_TERMINAL_LOG_MESSAGE, resources.getString(R.string.terminal_message_no_connection)));
        }
    }

    public boolean isRfCommValid() {
        return rfBluetoothSocket != null;
    }

    private void rfCommRead() {
        new Thread(() -> {
            final byte[] rfBluetoothInBuffer = new byte[1024];
            rfReadLoopState = 1;
            while (rfReadLoopState > 0) {
                try {
                    final int numBytes = rfBluetoothInStream.read(rfBluetoothInBuffer);
                    if (numBytes > 0) {
                        handlerRfCommRead.post(() -> {
                            byte[] answer = new byte[numBytes];
                            System.arraycopy(rfBluetoothInBuffer, 0, answer, 0, numBytes);
                            setChanged();
                            notifyObservers(new BluetoothData(BluetoothData.TYPE_TERMINAL_DATA, answer));
                        });
                    }
                } catch (NullPointerException | IOException e) {
                    if (!isManualDisconnect) {
                        try {
                            rfBluetoothSocket.close();
                            rfBluetoothSocket = null;
                        } catch (NullPointerException | IOException closeException) {
                            rfBluetoothSocket = null;
                        }
                        handlerRfCommRead.post(() -> {
                            setChanged();
                            notifyObservers(new BluetoothData(BluetoothData.TYPE_TERMINAL_LOG_MESSAGE, resources.getString(R.string.terminal_message_fail_read_data)));
                        });
                    }
                    rfReadLoopState = -1;
                }
            }
        }).start();
    }

    public void rfCommWrite(byte[] bytes) {
        if (rfBluetoothOutStream != null) {
            try {
                rfBluetoothOutStream.write(bytes);
            } catch (NullPointerException | IOException e) {
                if (!isManualDisconnect) {
                    try {
                        rfBluetoothSocket.close();
                        rfBluetoothSocket = null;
                    } catch (IOException closeException) {
                        rfBluetoothSocket = null;
                    }
                    setChanged();
                    notifyObservers(new BluetoothData(BluetoothData.TYPE_TERMINAL_LOG_MESSAGE, resources.getString(R.string.terminal_message_fail_send_data)));
                }
            }
        }
    }

    public void setMinRssi(int minRssi) {
        this.minRssi = minRssi;
    }

    public void unsetRangeRssi() {
        this.minRssi = rssiIsNotSet;
    }

    public int getMinRssi() {
        return this.minRssi;
    }

    public boolean isSetRangeRssi() {
        return (this.minRssi != rssiIsNotSet);
    }

    public void setSearchStringFilter(String searchStringFilter) {
        this.searchStringFilter = searchStringFilter;
    }

    public String getSearchStringFilter() {
        return this.searchStringFilter;
    }

    public void unsetSearchStringFilter() {
        this.searchStringFilter = "";
    }

    public boolean isSetSearchFilter() {
        return isSetRangeRssi() || !searchStringFilter.isEmpty();
    }

    public void setSearchCondition(String searchCondition) {
        this.searchCondition = searchCondition;
    }

    public void unsetSearchCondition() {
        this.searchCondition = resources.getString(R.string.radio_button_none);
    }

    public String getSearchCondition() {
        return searchCondition;
    }

    public ArrayList<String> getBondedDevices() {
        ArrayList<String> result = new ArrayList<>();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0)
            for (BluetoothDevice device : pairedDevices) result.add(device.getAddress());
        return result;
    }

    public boolean getBondState(String mac_address) {
        try {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mac_address);
            return device.getBondState() == BluetoothDevice.BOND_BONDED;
        } catch (NullPointerException | IllegalArgumentException ex) {
            return false;
        }
    }

    public void pairDevice(String mac_address) {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mac_address);
        try {
            if (!device.createBond()) {
                setChanged();
                notifyObservers(new BluetoothData(BluetoothData.TYPE_BLUETOOTH_ERROR_PAIRED));
            }
        } catch (Exception e) {
            setChanged();
            notifyObservers(new BluetoothData(BluetoothData.TYPE_BLUETOOTH_ERROR_PAIRED));
        }
    }

    public void unPairDevice(String mac_address) {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mac_address);
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            setChanged();
            notifyObservers(new BluetoothData(BluetoothData.TYPE_BLUETOOTH_ERROR_UNPAIRED));
        }
    }

    public void startDiscovery(int mode) {
        stopGattClient();
        if (rfBluetoothSocket != null) {
            try {
                rfBluetoothSocket.close();
                rfBluetoothSocket = null;
            } catch (IOException e) {
                rfBluetoothSocket = null;
            }
        }
        switch (mode) {
            case BLUETOOTH_CLC_MODE:
                scanCLCDevice(true);
                runDiscovery = true;
                break;
            case BLUETOOTH_LE_MODE:
                scanLeDevice(true);
                runDiscovery = true;
                break;
            default:
                runDiscovery = false;
                break;
        }
    }

    public void stopDiscovery(int mode) {
        switch (mode) {
            case BLUETOOTH_CLC_MODE:
                scanCLCDevice(false);
                break;
            case BLUETOOTH_LE_MODE:
                scanLeDevice(false);
                break;
            default:
                scanCLCDevice(false);
                scanLeDevice(false);
                break;
        }
        runDiscovery = false;
    }

    private void scanCLCDevice(final boolean enable) {
        if (enable) {
            if (runnableCLCScanner != null) {
                handlerCLCScanner.removeCallbacks(runnableCLCScanner);
                mBluetoothAdapter.cancelDiscovery();
            }
            runnableCLCScanner = () -> {
                if (runDiscovery) {
                    mBluetoothAdapter.cancelDiscovery();
                    setChanged();
                    notifyObservers(new BluetoothData(BluetoothData.TYPE_DISCOVERY_FINISHED));
                    runDiscovery = false;
                }
            };
            handlerCLCScanner.postDelayed(runnableCLCScanner, 1000L * getDiscoveryDuration());
            mBluetoothAdapter.startDiscovery();
        } else {
            if (runnableCLCScanner != null) {
                handlerCLCScanner.removeCallbacks(runnableCLCScanner);
                runnableCLCScanner = null;
            }
            if (runDiscovery) {
                setChanged();
                notifyObservers(new BluetoothData(BluetoothData.TYPE_DISCOVERY_FINISHED));
            }
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    private void scanLeDevice(final boolean enable) {
        BluetoothLeScanner mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (enable) {
            if (runnableLeScanner != null) {
                handlerLeScanner.removeCallbacks(runnableLeScanner);
                mBluetoothLeScanner.stopScan(mLeScanCallback);
            }
            runnableLeScanner = () -> {
                if (runDiscovery) {
                    mBluetoothLeScanner.stopScan(mLeScanCallback);
                    setChanged();
                    notifyObservers(new BluetoothData(BluetoothData.TYPE_DISCOVERY_FINISHED));
                    runDiscovery = false;
                }
            };
            handlerLeScanner.postDelayed(runnableLeScanner, 1000L * getDiscoveryDuration());
            mBluetoothLeScanner.startScan(mLeScanCallback);
        } else {
            if (runnableLeScanner != null) {
                handlerLeScanner.removeCallbacks(runnableLeScanner);
                runnableLeScanner = null;
            }
            if (runDiscovery) {
                setChanged();
                notifyObservers(new BluetoothData(BluetoothData.TYPE_DISCOVERY_FINISHED));
            }
            mBluetoothLeScanner.stopScan(mLeScanCallback);
        }
    }

    private final ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice bluetoothDevice = result.getDevice();
            int rssi = result.getRssi();
            android.bluetooth.le.ScanRecord scanRecord = result.getScanRecord();
            HashMap<String, String> dataMap = new HashMap<>();
            if (bluetoothDevice.getName() != null) {
                dataMap.put("name", bluetoothDevice.getName());
            } else {
                dataMap.put("name", StartApplication.getInstance().getResources().getString(R.string.unnamed));
            }
            dataMap.put("address", bluetoothDevice.getAddress());
            dataMap.put("rssi", String.valueOf(rssi));
            dataMap.putAll(getBluetoothDeviceInfo(bluetoothDevice));
            if (Objects.equals(dataMap.get("type"), "UNKNOWN")) dataMap.put("type", "LE device");
            dataMap.putAll(BluetoothScanRecord.decode(scanRecord.getBytes()));
            StringBuilder scanRecordString = new StringBuilder();
            int byteCounter = 0;
            for (byte b : scanRecord.getBytes()) {
                scanRecordString.append(String.format("%02x ", b));
                if (++byteCounter == 10) {
                    byteCounter = 0;
                    scanRecordString.append("\n");
                }
            }
            dataMap.put("scanRecord", "\n" + scanRecordString);
            if (MainActivityModel.getInstance().lastLocation != null) {
                dataMap.put("latitude", String.valueOf(MainActivityModel.getInstance().lastLocation.getLatitude()));
                dataMap.put("longitude", String.valueOf(MainActivityModel.getInstance().lastLocation.getLongitude()));
            } else {
                dataMap.put("latitude", StartApplication.getInstance().getResources().getString(R.string.undefined));
                dataMap.put("longitude", StartApplication.getInstance().getResources().getString(R.string.undefined));
            }
            if (isSetSearchFilter()) {
                if (searchStringFilter.isEmpty()) {
                    if (isSetRangeRssi() && !searchCondition.equals(resources.getString(R.string.radio_button_none))) {
                        if (!(rssi >= minRssi && rssi <= maxRssi)) {
                            dataMap.clear();
                            return;
                        }
                    }
                } else {
                    if (searchCondition.equals(resources.getString(R.string.radio_button_none))) {
                        for (String key : dataMap.keySet()) {
                            if (Objects.requireNonNull(dataMap.get(key)).replaceAll("\n", " ").matches("(?i:.*" + searchStringFilter + ".*)")) {
                                setChanged();
                                notifyObservers(new BluetoothData(BluetoothData.TYPE_NEW_LE_DISCOVERY_DATA, dataMap));
                                return;
                            }
                        }
                        dataMap.clear();
                        return;
                    }
                    if (searchCondition.equals(resources.getString(R.string.radio_button_only))) {
                        if (isSetRangeRssi()) {
                            if (rssi >= minRssi && rssi <= maxRssi) {
                                setChanged();
                                notifyObservers(new BluetoothData(BluetoothData.TYPE_NEW_LE_DISCOVERY_DATA, dataMap));
                            } else {
                                dataMap.clear();
                            }
                        } else {
                            dataMap.clear();
                        }
                        return;
                    }
                    if (searchCondition.equals(resources.getString(R.string.radio_button_and))) {
                        if (isSetRangeRssi()) {
                            if (rssi >= minRssi && rssi <= maxRssi) {
                                for (String key : dataMap.keySet()) {
                                    if (Objects.requireNonNull(dataMap.get(key)).replaceAll("\n", " ").matches("(?i:.*" + searchStringFilter + ".*)")) {
                                        setChanged();
                                        notifyObservers(new BluetoothData(BluetoothData.TYPE_NEW_LE_DISCOVERY_DATA, dataMap));
                                        return;
                                    }
                                }
                            }
                        }
                        dataMap.clear();
                        return;
                    }
                    if (searchCondition.equals(resources.getString(R.string.radio_button_or))) {
                        for (String key : dataMap.keySet()) {
                            if (Objects.requireNonNull(dataMap.get(key)).replaceAll("\n", " ").matches("(?i:.*" + searchStringFilter + ".*)")) {
                                setChanged();
                                notifyObservers(new BluetoothData(BluetoothData.TYPE_NEW_LE_DISCOVERY_DATA, dataMap));
                                return;
                            }
                        }
                        if (isSetRangeRssi()) {
                            if (rssi >= minRssi && rssi <= maxRssi) {
                                setChanged();
                                notifyObservers(new BluetoothData(BluetoothData.TYPE_NEW_LE_DISCOVERY_DATA, dataMap));
                            } else {
                                dataMap.clear();
                            }
                        } else {
                            dataMap.clear();
                        }
                        return;
                    }
                    dataMap.clear();
                    return;
                }
            }
            setChanged();
            notifyObservers(new BluetoothData(BluetoothData.TYPE_NEW_LE_DISCOVERY_DATA, dataMap));
        }
    };

    public void registerBroadcastReceivers() {
        brBluetoothActionStateChanged = StartApplication.getInstance().registerReceiver(bluetoothActionStateChanged, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        brBluetoothActionFound = StartApplication.getInstance().registerReceiver(bluetoothActionFound, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        brBluetoothActionUUID = StartApplication.getInstance().registerReceiver(bluetoothActionUUID, new IntentFilter(BluetoothDevice.ACTION_UUID));
        brBluetoothActionBondStateChanged = StartApplication.getInstance().registerReceiver(bluetoothActionBondStateChanged, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
    }

    public void unregisterBroadcastReceivers() {
        if (brBluetoothActionStateChanged != null) {
            StartApplication.getInstance().unregisterReceiver(bluetoothActionStateChanged);
        }
        if (brBluetoothActionFound != null) {
            StartApplication.getInstance().unregisterReceiver(bluetoothActionFound);
        }
        if (brBluetoothActionUUID != null) {
            StartApplication.getInstance().unregisterReceiver(bluetoothActionUUID);
        }
        if (brBluetoothActionBondStateChanged != null) {
            StartApplication.getInstance().unregisterReceiver(bluetoothActionBondStateChanged);
        }
    }

    private final BroadcastReceiver bluetoothActionBondStateChanged = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    setChanged();
                    notifyObservers(new BluetoothData(BluetoothData.TYPE_BLUETOOTH_PAIRED));
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                    setChanged();
                    notifyObservers(new BluetoothData(BluetoothData.TYPE_BLUETOOTH_UNPAIRED));
                }
            }
        }
    };

    private final BroadcastReceiver bluetoothActionStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int cState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            if (cState == BluetoothAdapter.STATE_OFF) {
                setChanged();
                notifyObservers(new BluetoothData(BluetoothData.TYPE_BLUETOOTH_DISABLE));
            }
        }
    };

    public void stopGattClient() {
        if (bluetoothGattClient == null) {
            return;
        }
        bluetoothGattClient.close();
        bluetoothGattClient = null;
    }

    public void startGattClient(String mac_address, BluetoothGattCallback bluetoothCattCallback, boolean autoConnect) {
        stopDiscovery(BLUETOOTH_ALL_MODE);
        stopGattClient();
        if (rfBluetoothSocket != null) {
            try {
                rfBluetoothSocket.close();
                rfBluetoothSocket = null;
            } catch (IOException e) {
                rfBluetoothSocket = null;
            }
        }
        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(mac_address);
        bluetoothGattClient = bluetoothDevice.connectGatt(null, autoConnect, bluetoothCattCallback);
    }

    public BluetoothGatt getGattClient() {
        return bluetoothGattClient;
    }

    private final BroadcastReceiver bluetoothActionUUID = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            StringBuilder serviceUUIDs = new StringBuilder();
            String resultServiceUUIDs = "";
            if (BluetoothDevice.ACTION_UUID.equals(action)) {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (!BluetoothAdapter.checkBluetoothAddress(bluetoothDevice.getAddress())) return;
                HashMap<String, String> dataMap = new HashMap<>();
                dataMap.put("address", bluetoothDevice.getAddress());
                Parcelable[] UUIDs = bluetoothDevice.getUuids();
                if (UUIDs != null) {
                    for (Parcelable UUID : UUIDs) {
                        serviceUUIDs.append(UUID.toString());
                        serviceUUIDs.append(";");
                    }
                    serviceUUIDs.append(";");
                    resultServiceUUIDs = serviceUUIDs.toString().replaceAll(";;", "");
                    dataMap.put("serviceUUIDs", resultServiceUUIDs);
                }
                if (!resultServiceUUIDs.isEmpty()) {
                    setChanged();
                    notifyObservers(new BluetoothData(BluetoothData.TYPE_NEW_CLC_DISCOVERY_SDP_UUIDS, dataMap));
                }
            }
        }
    };

    private final BroadcastReceiver bluetoothActionFound = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                double rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                HashMap<String, String> dataMap = new HashMap<>();
                if (bluetoothDevice.getName() != null)
                    dataMap.put("name", bluetoothDevice.getName());
                else
                    dataMap.put("name", StartApplication.getInstance().getResources().getString(R.string.unnamed));
                dataMap.put("address", bluetoothDevice.getAddress());
                dataMap.put("rssi", String.valueOf(rssi));
                dataMap.put("serviceUUIDs", "");
                dataMap.putAll(getBluetoothDeviceInfo(bluetoothDevice));
                if (MainActivityModel.getInstance().lastLocation != null) {
                    dataMap.put("latitude", String.valueOf(MainActivityModel.getInstance().lastLocation.getLatitude()));
                    dataMap.put("longitude", String.valueOf(MainActivityModel.getInstance().lastLocation.getLongitude()));
                } else {
                    dataMap.put("latitude", StartApplication.getInstance().getResources().getString(R.string.undefined));
                    dataMap.put("longitude", StartApplication.getInstance().getResources().getString(R.string.undefined));
                }
                bluetoothDevice.fetchUuidsWithSdp();
                if (isSetSearchFilter()) {
                    if (searchStringFilter.isEmpty()) {
                        if (isSetRangeRssi() && !searchCondition.equals(resources.getString(R.string.radio_button_none))) {
                            if (!(rssi >= minRssi && rssi <= maxRssi)) {
                                dataMap.clear();
                                return;
                            }
                        }
                    } else {
                        if (searchCondition.equals(resources.getString(R.string.radio_button_none))) {
                            if (Objects.requireNonNull(dataMap.get("name")).matches("(?i:.*" + searchStringFilter + ".*)")
                                    || Objects.requireNonNull(dataMap.get("address")).matches("(?i:.*" + searchStringFilter + ".*)")
                                    || Objects.requireNonNull(dataMap.get("class")).matches("(?i:.*" + searchStringFilter + ".*)")
                                    || Objects.requireNonNull(dataMap.get("type")).matches("(?i:.*" + searchStringFilter + ".*)")) {
                                setChanged();
                                notifyObservers(new BluetoothData(BluetoothData.TYPE_NEW_CLC_DISCOVERY_DATA, dataMap));
                                return;
                            }
                            dataMap.clear();
                            return;
                        }
                        if (searchCondition.equals(resources.getString(R.string.radio_button_only))) {
                            if (isSetRangeRssi()) {
                                if (rssi >= minRssi && rssi <= maxRssi) {
                                    setChanged();
                                    notifyObservers(new BluetoothData(BluetoothData.TYPE_NEW_CLC_DISCOVERY_DATA, dataMap));
                                } else {
                                    dataMap.clear();
                                }
                            } else {
                                dataMap.clear();
                            }
                            return;
                        }
                        if (searchCondition.equals(resources.getString(R.string.radio_button_and))) {
                            if (Objects.requireNonNull(Objects.requireNonNull(dataMap.get("name"))).matches("(?i:.*" + searchStringFilter + ".*)")
                                    || Objects.requireNonNull(dataMap.get("address")).matches("(?i:.*" + searchStringFilter + ".*)")
                                    || Objects.requireNonNull(Objects.requireNonNull(dataMap.get("class"))).matches("(?i:.*" + searchStringFilter + ".*)")
                                    || Objects.requireNonNull(Objects.requireNonNull(dataMap.get("type"))).matches("(?i:.*" + searchStringFilter + ".*)")
                            ) {
                                if (isSetRangeRssi()) {
                                    if (rssi >= minRssi && rssi <= maxRssi) {
                                        setChanged();
                                        notifyObservers(new BluetoothData(BluetoothData.TYPE_NEW_CLC_DISCOVERY_DATA, dataMap));
                                    } else {
                                        dataMap.clear();
                                    }
                                } else {
                                    dataMap.clear();
                                }
                            } else {
                                dataMap.clear();
                            }
                            return;
                        }
                        if (searchCondition.equals(resources.getString(R.string.radio_button_or))) {
                            if (Objects.requireNonNull(dataMap.get("name")).matches("(?i:.*" + searchStringFilter + ".*)")
                                    || Objects.requireNonNull(dataMap.get("address")).matches("(?i:.*" + searchStringFilter + ".*)")
                                    || Objects.requireNonNull(dataMap.get("class")).matches("(?i:.*" + searchStringFilter + ".*)")
                                    || Objects.requireNonNull(dataMap.get("type")).matches("(?i:.*" + searchStringFilter + ".*)")
                            ) {
                                setChanged();
                                notifyObservers(new BluetoothData(BluetoothData.TYPE_NEW_CLC_DISCOVERY_DATA, dataMap));
                                return;
                            }
                            if (isSetRangeRssi()) {
                                if (rssi >= minRssi && rssi <= maxRssi) {
                                    setChanged();
                                    notifyObservers(new BluetoothData(BluetoothData.TYPE_NEW_CLC_DISCOVERY_DATA, dataMap));
                                } else {
                                    dataMap.clear();
                                }
                            } else {
                                dataMap.clear();
                            }
                            return;
                        }
                        dataMap.clear();
                        return;
                    }
                }
                setChanged();
                notifyObservers(new BluetoothData(BluetoothData.TYPE_NEW_CLC_DISCOVERY_DATA, dataMap));
            }
        }
    };

    private HashMap<String, String> getBluetoothDeviceInfo(BluetoothDevice bluetoothDevice) {
        HashMap<String, String> result = new HashMap<>();
        switch (bluetoothDevice.getBluetoothClass().getMajorDeviceClass()) {
            case BluetoothClass.Device.Major.AUDIO_VIDEO:
                switch (bluetoothDevice.getBluetoothClass().getDeviceClass()) {
                    case BluetoothClass.Device.AUDIO_VIDEO_CAMCORDER:
                        result.put("class", "Camcorder");
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO:
                        result.put("class", "Car audio device");
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE:
                        result.put("class", "Heads free audio/video device");
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES:
                        result.put("class", "Headphones");
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO:
                        result.put("class", "HI-FI audio device");
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER:
                        result.put("class", "Loud speaker");
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE:
                        result.put("class", "Microphone");
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO:
                        result.put("class", "Portable audio device");
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX:
                        result.put("class", "Set top box");
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_VCR:
                        result.put("class", "VCR");
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CAMERA:
                        result.put("class", "Video camera");
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING:
                        result.put("class", "Video conferencing");
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER:
                        result.put("class", "Video display and loud speaker");
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY:
                        result.put("class", "Video gaming toy");
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR:
                        result.put("class", "Video monitor");
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET:
                        result.put("class", "Wearable headset");
                        break;
                    default:
                    case BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED:
                        result.put("class", "Audio/Video device");
                        break;
                }
                break;
            case BluetoothClass.Device.Major.COMPUTER:
                switch (bluetoothDevice.getBluetoothClass().getDeviceClass()) {
                    case BluetoothClass.Device.COMPUTER_DESKTOP:
                        result.put("class", "Desktop");
                        break;
                    case BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA:
                        result.put("class", "Handheld PC PDA");
                        break;
                    case BluetoothClass.Device.COMPUTER_LAPTOP:
                        result.put("class", "Laptop");
                        break;
                    case BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA:
                        result.put("class", "PALM size PC PDA");
                        break;
                    case BluetoothClass.Device.COMPUTER_SERVER:
                        result.put("class", "Server");
                        break;
                    case BluetoothClass.Device.COMPUTER_WEARABLE:
                        result.put("class", "Wearable computer");
                        break;
                    default:
                    case BluetoothClass.Device.COMPUTER_UNCATEGORIZED:
                        result.put("class", "Computer");
                        break;
                }
                break;
            case BluetoothClass.Device.Major.HEALTH:
                switch (bluetoothDevice.getBluetoothClass().getDeviceClass()) {
                    case BluetoothClass.Device.HEALTH_BLOOD_PRESSURE:
                        result.put("class", "Blood pressure meter");
                        break;
                    case BluetoothClass.Device.HEALTH_DATA_DISPLAY:
                        result.put("class", "Health data display");
                        break;
                    case BluetoothClass.Device.HEALTH_GLUCOSE:
                        result.put("class", "Glucose meter");
                        break;
                    case BluetoothClass.Device.HEALTH_PULSE_OXIMETER:
                        result.put("class", "Pulse oximeter");
                        break;
                    case BluetoothClass.Device.HEALTH_PULSE_RATE:
                        result.put("class", "Pulse rate meter");
                        break;
                    case BluetoothClass.Device.HEALTH_THERMOMETER:
                        result.put("class", "Thermometer");
                        break;
                    case BluetoothClass.Device.HEALTH_WEIGHING:
                        result.put("class", "Weighing");
                        break;
                    default:
                    case BluetoothClass.Device.HEALTH_UNCATEGORIZED:
                        result.put("class", "Health device");
                        break;
                }
                break;
            case BluetoothClass.Device.Major.IMAGING:
                result.put("class", "Imaging device");
                break;
            case BluetoothClass.Device.Major.NETWORKING:
                result.put("class", "Networking device");
                break;
            case BluetoothClass.Device.Major.PERIPHERAL:
                result.put("class", "Peripheral device");
                break;
            case BluetoothClass.Device.Major.PHONE:
                switch (bluetoothDevice.getBluetoothClass().getDeviceClass()) {
                    case BluetoothClass.Device.PHONE_CELLULAR:
                        result.put("class", "Cellular phone");
                        break;
                    case BluetoothClass.Device.PHONE_CORDLESS:
                        result.put("class", "Cordless phone");
                        break;
                    case BluetoothClass.Device.PHONE_ISDN:
                        result.put("class", "ISDN phone");
                        break;
                    case BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY:
                        result.put("class", "Modem or gateway");
                        break;
                    case BluetoothClass.Device.PHONE_SMART:
                        result.put("class", "Smart phone");
                        break;
                    default:
                    case BluetoothClass.Device.PHONE_UNCATEGORIZED:
                        result.put("class", "Phone");
                        break;
                }
                break;
            case BluetoothClass.Device.Major.TOY:
                switch (bluetoothDevice.getBluetoothClass().getDeviceClass()) {
                    case BluetoothClass.Device.TOY_CONTROLLER:
                        result.put("class", "Toy controller");
                        break;
                    case BluetoothClass.Device.TOY_DOLL_ACTION_FIGURE:
                        result.put("class", "Doll action figure");
                        break;
                    case BluetoothClass.Device.TOY_GAME:
                        result.put("class", "Game");
                        break;
                    case BluetoothClass.Device.TOY_ROBOT:
                        result.put("class", "Robot");
                        break;
                    case BluetoothClass.Device.TOY_VEHICLE:
                        result.put("class", "Vehicle");
                        break;
                    default:
                    case BluetoothClass.Device.TOY_UNCATEGORIZED:
                        result.put("class", "Toy");
                        break;
                }
                break;
            case BluetoothClass.Device.Major.WEARABLE:
                switch (bluetoothDevice.getBluetoothClass().getDeviceClass()) {
                    case BluetoothClass.Device.WEARABLE_GLASSES:
                        result.put("class", "Glasses");
                        break;
                    case BluetoothClass.Device.WEARABLE_HELMET:
                        result.put("class", "Helmet");
                        break;
                    case BluetoothClass.Device.WEARABLE_JACKET:
                        result.put("class", "Jacket");
                        break;
                    case BluetoothClass.Device.WEARABLE_PAGER:
                        result.put("class", "Pager");
                        break;
                    case BluetoothClass.Device.WEARABLE_WRIST_WATCH:
                        result.put("class", "Wrist watch");
                        break;
                    default:
                    case BluetoothClass.Device.WEARABLE_UNCATEGORIZED:
                        result.put("class", "Wearable thing");
                        break;
                }
                break;
            default:
            case BluetoothClass.Device.Major.UNCATEGORIZED:
                result.put("class", "UNKNOWN");
                break;
        }
        switch (bluetoothDevice.getType()) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                result.put("type", "BR/EDR device");
                break;
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                result.put("type", "BR/EDR/LE device");
                break;
            case BluetoothDevice.DEVICE_TYPE_LE:
                result.put("type", "LE device");
                break;
            case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
            default:
                result.put("type", "UNKNOWN");
                break;
        }
        return result;
    }
}
