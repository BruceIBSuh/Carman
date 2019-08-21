package com.silverback.carman2.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.SettingPreferenceActivity;
import com.silverback.carman2.adapters.SettingServiceItemAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.FragmentSharedModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingServiceItemFragment extends Fragment implements
        SettingServiceItemAdapter.OnServiceItemClickListener{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingServiceItemFragment.class);

    // Constants for the mode param in modifyJSONArray()
    private static final int MOVEUP = 3;
    private static final int MOVEDOWN = 4;


    // Objects
    private SharedPreferences mSettings;
    private SettingServiceItemAdapter mAdapter;
    private SettingServiceDlgFragment dlgFragment;
    private JSONArray jsonSvcItemArray;

    // Fields
    private boolean bEditMode = false;
    private int itemPos;

    public SettingServiceItemFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Indicate the fragment has the option menu, invoking onCreateOptionsMenu()
        setHasOptionsMenu(true);
        dlgFragment = new SettingServiceDlgFragment();

        // List.add() does not work if List is create by Arrays.asList().
        mSettings =((SettingPreferenceActivity)getActivity()).getSettings();
        String json = mSettings.getString(Constants.SERVICE_ITEMS, null);

        try {
            jsonSvcItemArray = new JSONArray(json);
            mAdapter = new SettingServiceItemAdapter(this, jsonSvcItemArray);
        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }


        // ViewModel to share data b/w SettingServiceItemFragment and SettingServiceDlgFragment.
        // SettingServiceDlgFragmnt adds a service item with its mileage and time to check, data of
        // which are passed here using FragmentSharedModel as the type of List<String>
        FragmentSharedModel sharedModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
        sharedModel.getJsonServiceItemObject().observe(this, data -> {
            jsonSvcItemArray.put(data);
            mAdapter.notifyItemInserted(jsonSvcItemArray.length() - 1);
            mSettings.edit().putString(Constants.SERVICE_ITEMS, jsonSvcItemArray.toString()).apply();
        });

        // Fetch LiveData<Boolean> that indicates whch button to select in AlertDialogFragment when
        // a service item is removed.
        sharedModel.getAlert().observe(this, data -> {
            if(data) {
                jsonSvcItemArray.remove(itemPos);
                mAdapter.notifyDataSetChanged();
                mSettings.edit().putString(Constants.SERVICE_ITEMS, jsonSvcItemArray.toString()).apply();
            }
        });
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.fragment_setting_chklist, container, false);
        RecyclerView recyclerView = localView.findViewById(R.id.recycler_chklist);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(mAdapter);

        // Inflate the layout for this fragment
        return localView;
    }

    // Create the Toolbar option menu when the fragment is instantiated.
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_options_setting, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch(item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;

            case R.id.menu_add:
                if(getFragmentManager() != null)
                    dlgFragment.show(getFragmentManager(), null);

                return true;

            case R.id.menu_edit:
                bEditMode = !bEditMode;
                mAdapter.setEditMode(bEditMode);
                mAdapter.notifyDataSetChanged();
                return true;
        }

        return false;
    }

    // Callback invoked by SettingServiceItemAdapter.OnServiceItemClickListener
    @Override
    public void editServiceItem(int resId, int pos) {

        log.i("Edit Service Item: %s", pos);
        switch(resId) {
            case R.id.btn_setting_del:
                //jsonSvcItemArray.remove(pos);
                itemPos = pos;
                AlertDialogFragment alertFragment = AlertDialogFragment.newInstance("Alert", "Delete");
                if(getFragmentManager() != null) alertFragment.show(getFragmentManager(), null);
                break;

            case R.id.btn_setting_up:
                if(pos > 0) {
                    swapJSONObject(pos, MOVEUP);
                    mAdapter.notifyDataSetChanged();
                    mSettings.edit().putString(Constants.SERVICE_ITEMS, jsonSvcItemArray.toString()).apply();
                }

                break;

            case R.id.btn_setting_down:
                if(pos < (jsonSvcItemArray.length() -1)) {
                    swapJSONObject(pos, MOVEDOWN);
                    mAdapter.notifyDataSetChanged();
                    mSettings.edit().putString(Constants.SERVICE_ITEMS, jsonSvcItemArray.toString()).apply();
                }
                break;
        }

    }

    // Method for switching the location of an service item using Up and Down button
    private void swapJSONObject(int index, int mode) {
        JSONObject obj = jsonSvcItemArray.optJSONObject(index);
        try {
            switch(mode) {
                case MOVEUP:
                    JSONObject tempUp = jsonSvcItemArray.optJSONObject(index - 1);
                    jsonSvcItemArray.put(index - 1, obj);
                    jsonSvcItemArray.put(index, tempUp);
                    break;

                case MOVEDOWN:
                    JSONObject tempDown = jsonSvcItemArray.optJSONObject(index + 1);
                    jsonSvcItemArray.put(index + 1, obj);
                    jsonSvcItemArray.put(index, tempDown);
                    break;
            }

        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }
    }
}