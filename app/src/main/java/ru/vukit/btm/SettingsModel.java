package ru.vukit.btm;

import androidx.annotation.Keep;

import java.util.Observable;
import java.util.Observer;

import ru.vukit.btm.database.DatabaseMessage;

@Keep
class SettingsModel implements Observer {

    private static SettingsModel INSTANCE = null;

    public static synchronized SettingsModel getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SettingsModel();
        }
        return (INSTANCE);
    }

    private SettingsFragment controller;

    void connectController(SettingsFragment controller) {
        this.controller = controller;
    }

    void disconnectController() {
        this.controller = null;
    }

    @Override
    public void update(Observable o, Object arg) {
        String observable_name = o.getClass().getSimpleName();
        if (observable_name.equals("DatabaseDriver")) {
            DatabaseMessage databaseMessage = (DatabaseMessage) arg;
            if (databaseMessage.getType() == DatabaseMessage.TYPE_NEW_DATA) {
                if (controller != null && databaseMessage.getMessage() != null)
                    new SnackBar().ShowShort(databaseMessage.getMessage());
            }
        }
    }

}
