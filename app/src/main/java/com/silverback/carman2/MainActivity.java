package com.silverback.carman2;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.silverback.carman2.fragments.BillboardFragment;
import com.silverback.carman2.fragments.FinishAppDialogFragment;
import com.silverback.carman2.fragments.GeneralFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.threads.StationInfoTask;

import java.io.File;

public class MainActivity extends BaseActivity implements
        FinishAppDialogFragment.NoticeDialogListener {

    // Logging
    private final LoggingHelper log = LoggingHelperFactory.create(MainActivity.class);

    // Objects
    private StationInfoTask mapInfoTask;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);//Sets the toolbar used as ActionBar

        // Get the default value of fuel, searching radius, and listing order from BaseActivity
        // and set it to be bundled to pass it to GeneralFragment
        Bundle bundle = new Bundle();
        bundle.putStringArray("defaults", getDefaultParams());
        Fragment generalFragment = new GeneralFragment();
        //Fragment boardFragment = new BillboardFragment();
        // Attaches GeneralFragment as a default display at first or returning from the fragments
        // picked up by Toolbar menus.
        generalFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_main, generalFragment, "general").addToBackStack(null).commit();

        // Permission Check
        checkPermissions();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onResume(){
        super.onResume();

        String title = mSettings.getString(Constants.VEHICLE_NAME, null);
        if(title != null) getSupportActionBar().setTitle(title);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mapInfoTask != null) mapInfoTask = null;
    }

    /*
     * The following 2 overriding methods are invoked by Toolbar working as Appbar or ActionBar
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_options_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {

            case R.id.action_garage:
                startActivity(new Intent(MainActivity.this, ExpenseActivity.class));
                return true;

            case R.id.action_board:
                startActivity(new Intent(MainActivity.this, BillboardActivity.class));
                return true;

            case R.id.action_login:
                return true;

            case R.id.action_setting:
                startActivity(new Intent(MainActivity.this, SettingPreferenceActivity.class));
                return true;

            default:
                log.i("finish activity");
                finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed(){
        // Pop up the dialog to confirm to leave the app.
        FinishAppDialogFragment alertDialog = new FinishAppDialogFragment();
        alertDialog.show(getSupportFragmentManager(), "FinishAppDialogFragment");
    }


    // App closing process, in which cache-clearing code be required.
    // FinishAppDialogFragment.NoticeDialogListener invokes
    // to handle how the dialog buttons act according to positive and negative.
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        //boolean isDeleted = false;
        File cacheDir = getCacheDir();
        if(cacheDir != null && checkUpdateOilPrice()) {
            for (File file : cacheDir.listFiles()) file.delete();
        }

        finishAffinity();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {}

}
