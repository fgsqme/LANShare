package com.fgsqw.lanshare.toast;

import android.widget.Toast;

import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.utils.ViewUpdate;

public class T {


    public static void s(Object s) {
        App application = App.getInstance();
        if (application != null) {
            ViewUpdate.threadUi(() -> Toast.makeText(application, s == null ? "" : s.toString(), Toast.LENGTH_LONG).show());
        }
    }


    public static void ss(Object s) {
        App application = App.getInstance();
        if (application != null) {
            ViewUpdate.threadUi(() -> Toast.makeText(application, s == null ? "" : s.toString(), Toast.LENGTH_SHORT).show());
        }
    }
}
