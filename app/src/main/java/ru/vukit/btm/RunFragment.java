package ru.vukit.btm;

import androidx.annotation.Keep;

@Keep
interface RunFragment {

    void Child(String parent, String child);

    @SuppressWarnings("unused")
    void Parent(String parent, String child);
}
