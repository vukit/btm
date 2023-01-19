package ru.vukit.btm;

import android.content.res.Resources;

import androidx.annotation.Keep;

import java.util.Observable;
import java.util.Observer;

import ru.vukit.btm.database.DatabaseDriver;
import ru.vukit.btm.database.DatabaseMessage;

@Keep
class DashBoardModel implements Observer {

    private DashBoardFragment controller;
    private final Resources resources = StartApplication.getInstance().getResources();
    int currentPosition = 0;
    String query = "SELECT " + DatabaseDriver.DatabaseContract.Devices._ID + " FROM " + DatabaseDriver.DatabaseContract.Devices.TABLE_NAME + " ORDER BY " + DatabaseDriver.DatabaseContract.Devices._ID + " DESC;";
    String search_string = "";
    Double search_latitude = MathModule.notLatitude;
    Double search_longitude = MathModule.notLongitude;
    String search_radio_button = resources.getString(R.string.radio_button_none);
    Double search_latitude_min = 0.0, search_longitude_min = 0.0, search_latitude_max = 0.0, search_longitude_max = 0.0;

    private static DashBoardModel INSTANCE = null;

    public static synchronized DashBoardModel getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DashBoardModel();
        }
        return (INSTANCE);
    }

    void connectController(DashBoardFragment controller) {
        this.controller = controller;
    }

    void disconnectController() {
        this.controller = null;
    }

    boolean isSetSearchFilter() {
        return !search_string.isEmpty() || !search_radio_button.equals(resources.getString(R.string.radio_button_none));
    }

    @Override
    public void update(Observable o, Object arg) {
        String observable_name = o.getClass().getSimpleName();
        if (observable_name.equals("DatabaseDriver")) {
            DatabaseMessage databaseMessage = (DatabaseMessage) arg;
            if (databaseMessage.getType() == DatabaseMessage.TYPE_NEW_DATA) {
                if (controller != null) controller.updateView();
            }
            if (controller != null && databaseMessage.getMessage() != null)
                new SnackBar().ShowShort(databaseMessage.getMessage());
        }
    }

}
