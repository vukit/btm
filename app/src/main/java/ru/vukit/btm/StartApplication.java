package ru.vukit.btm;

import androidx.annotation.Keep;
import androidx.multidex.MultiDexApplication;

import ru.vukit.btm.bluetooth.BluetoothDriver;
import ru.vukit.btm.database.DatabaseDriver;

@Keep
public class StartApplication extends MultiDexApplication {

    private static StartApplication startApplicationSingleton;
    static BluetoothDriver btDriverSingleton;
    static DatabaseDriver dbDriverSingleton;
    static SearchModel searchModelSingleton;
    static DashBoardModel dashBoardModelSingleton;
    static DeviceDetailsModel deviceDetailsModelSingleton;
    static SettingsModel settingsModelSingleton;

    public static StartApplication getInstance() {
        return startApplicationSingleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startApplicationSingleton = this;
        btDriverSingleton = BluetoothDriver.getInstance();
        dbDriverSingleton = DatabaseDriver.getInstance();
        searchModelSingleton = SearchModel.getInstance();
        dashBoardModelSingleton = DashBoardModel.getInstance();
        deviceDetailsModelSingleton = DeviceDetailsModel.getInstance();
        settingsModelSingleton = SettingsModel.getInstance();
        btDriverSingleton.addObserver(searchModelSingleton);
        btDriverSingleton.addObserver(dashBoardModelSingleton);
        btDriverSingleton.addObserver(deviceDetailsModelSingleton);
        dbDriverSingleton.addObserver(dashBoardModelSingleton);
        dbDriverSingleton.addObserver(searchModelSingleton);
        dbDriverSingleton.addObserver(deviceDetailsModelSingleton);
        dbDriverSingleton.addObserver(settingsModelSingleton);
    }

}
