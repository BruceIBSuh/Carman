package com.silverback.carman2;

import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class BoardWriteActivity extends BaseActivity {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardWriteActivity.class);

    // Constants
    private static final int MENU_ITEM_ID = 1001;

    // UIs
    private ConstraintLayout statusLayout;
    private View statusView, titleView;
    private TextView tvAutoMaker, tvAutoModel, tvAutoYear;
    private TextView tvBoardTitle, tvClubStatus;
    private EditText etBoardTitle;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billboard_write);

        Toolbar toolbar = findViewById(R.id.toolbar_board_write);
        statusLayout = findViewById(R.id.vg_status);
        //statusView = findViewById(R.id.tv_status_automaker);
        //titleView = findViewById(R.id.view_title);
        //tvAutoMaker = findViewById(R.id.tv_status_automaker);
        //tvAutoModel = findViewById(R.id.tv_status_model);
        //tvAutoYear = findViewById(R.id.tv_status_year);

        //EditText etTitle = findViewById(R.id.et_board_title);
        //etTitle.setFocusable(true);

        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }


        animateStatusTitleViews(getActionbarHeight());

        //InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.showSoftInput(etTitle, InputMethodManager.SHOW_FORCED);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_ITEM_ID, Menu.NONE, R.string.exp_menuitem_title_save);
        MenuItem item = menu.findItem(MENU_ITEM_ID);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item.setIcon(R.drawable.ic_toolbar_save);

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            log.i("onOptionsItemSelected in SettingPreferenceActivity");
            Intent resultIntent = new Intent();
            resultIntent.putExtra("result_msg", "OK");
            setResult(1001, resultIntent);
            finish();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void animateStatusTitleViews(float actionbarHeight) {
        ObjectAnimator animStatusView = ObjectAnimator.ofFloat(statusLayout, "Y", actionbarHeight);
        animStatusView.setDuration(1000);
        animStatusView.start();
    }
}
