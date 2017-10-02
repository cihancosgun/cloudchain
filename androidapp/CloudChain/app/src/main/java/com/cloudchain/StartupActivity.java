package com.cloudchain;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;

import com.cloudchain.db.DBManager;

public class StartupActivity extends AppCompatActivity {

    static StartupActivity startupActivity;

    public static StartupActivity getStartUpActivity() {
        return startupActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        startupActivity = this;
        DBManager dbManager = new DBManager(this);
        if (dbManager.getLoggedUser() != null) {
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
        } else {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
        }
    }
}
