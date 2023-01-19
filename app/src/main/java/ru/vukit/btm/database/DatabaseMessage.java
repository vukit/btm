package ru.vukit.btm.database;

import androidx.annotation.Keep;

@Keep
public
class DatabaseMessage {

    public static final int TYPE_BEGIN_TRANSACTION = 1;
    public static final int TYPE_NEW_DATA = 2;

    private final int type;
    private final String message;

    DatabaseMessage(int type, String message) {
        this.type = type;
        this.message = message;
    }

    public int getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

}
