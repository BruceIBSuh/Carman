package com.silverback.carman2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.EditText;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;

public class NotificationActivity extends AppCompatActivity {

    private static final LoggingHelper log = LoggingHelperFactory.create(NotificationActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        EditText etMileage = findViewById(R.id.et_noti_mileage);
        EditText etPayment = findViewById(R.id.et_noti_payment);

        SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        String currentMileage = mSettings.getString(Constants.ODOMETER, null);
        log.i("Current Mileage", currentMileage);

        etMileage.setHint(currentMileage);
        etPayment.setHint("50,000");


    }
}
