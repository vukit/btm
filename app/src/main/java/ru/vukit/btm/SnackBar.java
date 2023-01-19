package ru.vukit.btm;

import android.app.Activity;

import androidx.annotation.Keep;

import com.google.android.material.snackbar.Snackbar;

@Keep
class SnackBar {

    private final Activity activity;

    SnackBar() {
        activity = MainActivityModel.getInstance().controller;
    }

    void ShowShort(String message) {
        if (activity != null) {
            Snackbar.make(activity.findViewById(R.id.cl), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    void ShowLong(String message) {
        if (activity != null) {
            Snackbar.make(activity.findViewById(R.id.cl), message, Snackbar.LENGTH_LONG).show();
        }
    }

    void ShowIndefinite(String message) {
        if (activity != null) {
            Snackbar.make(activity.findViewById(R.id.cl), message, Snackbar.LENGTH_INDEFINITE).show();
        }
    }

}
