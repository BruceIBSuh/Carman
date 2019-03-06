package com.silverback.carman2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class IntroActivity extends AppCompatActivity { //implements View.OnClickListener {

    // Objects
    private LoggingHelper log;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Instantiate LogginHelper to show logs only at the debug mode.
        this.log = LoggingHelperFactory.create(IntroActivity.class);

        // Hide the action bar
        //getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        //getSupportActionBar().hide();

        setContentView(R.layout.activity_intro);

        Button btn = findViewById(R.id.btn_next);
        btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                startActivity(new Intent(IntroActivity.this, MainActivity.class));
            }
        });

    }


}
