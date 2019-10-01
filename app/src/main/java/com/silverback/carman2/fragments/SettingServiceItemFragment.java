package com.silverback.carman2.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.silverback.carman2.R;
import com.silverback.carman2.SettingPreferenceActivity;
import com.silverback.carman2.adapters.SettingServiceItemAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.utils.ItemTouchHelperCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingServiceItemFragment extends Fragment implements SettingServiceItemAdapter.OnAdapterCallback {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingServiceItemFragment.class);

    // Constants for the mode param in modifyJSONArray()
    private static final int MOVEUP = 3;
    private static final int MOVEDOWN = 4;


    // Objects
    private SharedPreferences mSettings;
    private SettingServiceItemAdapter mAdapter;
    private SettingSvcDialogFragment dlgFragment;
    private JSONArray jsonSvcItemArray;
    private RecyclerView recyclerView;

    // Fields
    private boolean bEditMode = false;
    private int removedPos;

    public SettingServiceItemFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Indicate the fragment has the option menu, invoking onCreateOptionsMenu()
        setHasOptionsMenu(true);
        dlgFragment = new SettingSvcDialogFragment();

        // List.add() does not work if List is create by Arrays.asList().
        mSettings =((SettingPreferenceActivity)getActivity()).getSettings();
        String json = mSettings.getString(Constants.SERVICE_ITEMS, null);

        try {
            jsonSvcItemArray = new JSONArray(json);
            mAdapter = new SettingServiceItemAdapter(jsonSvcItemArray, this);
        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }

        // ViewModel to share data b/w SettingServiceItemFragment and SettingSvcDialogFragment.
        // SettingServiceDlgFragmnt adds a service item with its mileage and time to check, data of
        // which are passed here using FragmentSharedModel as the type of List<String>
        FragmentSharedModel sharedModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
        sharedModel.getJsonServiceItemObject().observe(this, data -> {
            jsonSvcItemArray.put(data);
            mSettings.edit().putString(Constants.SERVICE_ITEMS, jsonSvcItemArray.toString()).apply();
            mAdapter.notifyItemInserted(jsonSvcItemArray.length());
        });
        // Fetch LiveData<Boolean> that indicates whch button to select in AlertDialogFragment when
        // a service item is removed.
        sharedModel.getAlert().observe(this, data -> {
            if(data) {
                log.i("Item Removed:", data);
                jsonSvcItemArray.remove(removedPos);
                mSettings.edit().putString(Constants.SERVICE_ITEMS, jsonSvcItemArray.toString()).apply();
                mAdapter.notifyItemRemoved(removedPos);
            }else {
                // TEST CODING : seems not working
                log.i("Cancel");
                mAdapter.notifyItemInserted(removedPos);
                recyclerView.scrollToPosition(removedPos);

            }
        });
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.fragment_setting_chklist, container, false);
        recyclerView = localView.findViewById(R.id.recycler_chklist);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        ItemTouchHelperCallback callback = new ItemTouchHelperCallback(getContext(), mAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

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
        }

        return false;
    }

    // The following 2 callback methods are invoked by SettingServiceItemAdapter.OnAdapterListener
    // to notify dragging or removing an item of RecyclerView.
    @Override
    public void dragItem(int from, int to) {

    }

    @Override
    public void removeItem(int position) {
        String title = "ALERT";
        String msg = "Remove the Service Item";
        removedPos = position;
        AlertDialogFragment alert = AlertDialogFragment.newInstance(title, msg, 3);
        if(getFragmentManager() != null) alert.show(getFragmentManager(), null);
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
