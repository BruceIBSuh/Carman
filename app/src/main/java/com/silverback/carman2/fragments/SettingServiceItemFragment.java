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

import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingServiceItemFragment extends Fragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingServiceItemFragment.class);

    // Objects
    private SharedPreferences mSettings;
    private SettingServiceItemAdapter mAdapter;
    private SettingServiceDlgFragment dlgFragment;
    private JSONArray jsonSvcItemArray;

    // Fields
    private boolean bEditMode = false;

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
        String jsonServiceItem = mSettings.getString(Constants.SERVICE_ITEMS, null);

        try {
            jsonSvcItemArray = new JSONArray(jsonServiceItem);
            mAdapter = new SettingServiceItemAdapter(jsonSvcItemArray);
        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }


        // ViewModel to share data b/w SettingServiceItemFragment and SettingServiceDlgFragment.
        // SettingServiceDlgFragmnt adds a service item with its mileage and time to check, data of
        // which are passed here using FragmentSharedModel as the type of List<String>
        FragmentSharedModel sharedModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
        sharedModel.getJsonServiceItemObject().observe(this, data -> {
            jsonSvcItemArray.put(data);
            mAdapter.notifyDataSetChanged();
        });
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.fragment_setting_chklist, container, false);
        RecyclerView recyclerView = localView.findViewById(R.id.recycler_chklist);

        //recyclerView.setHasFixedSize(true);
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch(item.getItemId()) {
            case android.R.id.home:
                mSettings.edit().putString(Constants.SERVICE_ITEMS, jsonSvcItemArray.toString()).apply();
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

}
