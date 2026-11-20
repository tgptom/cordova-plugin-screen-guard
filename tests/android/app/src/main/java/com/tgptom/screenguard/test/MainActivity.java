package com.tgptom.screenguard.test;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

/** Stand-in for the Cordova app's MainActivity: a solid magenta screen. */
public class MainActivity extends Activity {
    public static final int COLOR = Color.MAGENTA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View v = new View(this);
        v.setBackgroundColor(COLOR);
        setContentView(v);
    }
}
