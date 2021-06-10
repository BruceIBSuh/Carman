package com.silverback.carman.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.silverback.carman.BaseActivity;
import com.silverback.carman.R;
import com.silverback.carman.adapters.SettingServiceItemAdapter;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.utils.ItemTouchHelperCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingSvcItemFragment extends Fragment implements
        SettingServiceItemAdapter.OnServiceItemAdapterCallback {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingSvcItemFragment.class);

    // Constants for the mode param in modifyJSONArray()
    private static final int MOVEUP = 3;
    private static final int MOVEDOWN = 4;


    // Objects
    private CarmanDatabase mDB;
    private FragmentSharedModel fragmentModel;
    private SharedPreferences mSettings;
    private SettingServiceItemAdapter mAdapter;
    private SettingSvcItemDlgFragment dlgFragment;
    private JSONArray jsonSvcItemArray;
    private RecyclerView recyclerView;

    // Fields
    private boolean bEditMode = false;
    private int removedPos;
    private boolean isHistory;

    public SettingSvcItemFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Indicate the fragment has the option menu, invoking onCreateOptionsMenu()
        setHasOptionsMenu(true);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        mDB = CarmanDatabase.getDatabaseInstance(getContext());
        mSettings =((BaseActivity)getActivity()).getSharedPreferernces();
        dlgFragment = new SettingSvcItemDlgFragment();

        String json = mSettings.getString(Constants.SERVICE_ITEMS, null);
        int average = Integer.parseInt(mSettings.getString(Constants.AVERAGE, "10000"));
        log.i("average mileage: %s", average);
        try {
            jsonSvcItemArray = new JSONArray(json);
            mAdapter = new SettingServiceItemAdapter(jsonSvcItemArray, average, this);
        } catch(JSONException e) { e.printStackTrace();}

        // ViewModel to share data b/w SettingServiceItemFragment and SettingSvcItemDlgFragment.
        // SettingServiceDlgFragmnt adds a service item with its mileage and time to check, data of
        // which are passed here using FragmentSharedModel as the type of List<String>
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
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

        return localView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if(fragmentModel != null) fragmentModel = null;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        fragmentModel.getJsonServiceItemObj().observe(requireActivity(), jsonObject -> {
            // This is kind of an expedient code to invoke getItemCount() in which a new item is added
            // to ArrayList(svcItemList). A right coding should be notifyItemChanged(position, payloads)
            // which calls the partial binding to update the dataset but it seems not work here.
            // It looks like the new item has not been added to the list before notifyItemChanged
            // is called such that payloads shouldn't be passed.
            boolean isItemExist = false;

            // Check if a new item is valid by comparing its name w/ existing item names.
            for(int i = 0; i < jsonSvcItemArray.length(); i++) {
                // Compare Strings which is required to refactor with RegExp.
                try {
                    String itemName = jsonSvcItemArray.optJSONObject(i).getString("name");
                    isItemExist = itemName.equalsIgnoreCase(jsonObject.getString("name"));
                    if(isItemExist) break;
                } catch (JSONException e) {e.printStackTrace();}
            }

            if(!isItemExist) {
                jsonSvcItemArray.put(jsonObject);
                recyclerView.scrollToPosition(jsonSvcItemArray.length() - 1);
                mAdapter.notifyItemChanged(mAdapter.getItemCount());

            } else {
                Snackbar snackbar = Snackbar.make(recyclerView, R.string.pref_snackbar_msg, Snackbar.LENGTH_SHORT);
                TextView tvMessage = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                tvMessage.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                snackbar.show();
            }
        });
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

            case R.id.menu_add_service_item:
                if(getActivity() != null) dlgFragment.show(getActivity().getSupportFragmentManager(), null);
                return true;
        }

        return false;
    }

    @Override
    public void dragServiceItem(int from, int to) {
        JSONObject fromObject = jsonSvcItemArray.optJSONObject(from);
        JSONObject toObject = jsonSvcItemArray.optJSONObject(to);
        try {
            jsonSvcItemArray.put(from, toObject);
            jsonSvcItemArray.put(to, fromObject);
            mAdapter.notifyItemMoved(from, to);

            // Partial binding to change the index number ahead of each item when dragging the item.
            // notifyItemRangeChanged(int start, int count, Object payload)
            mAdapter.notifyItemRangeChanged(Math.min(from, to), Math.abs(from - to) + 1, true);

        } catch(JSONException e) { e.printStackTrace(); }
    }

    @Override
    public void delServiceItem(int position) {

        // Check if any maintenance history exists with this item.
        String itemName = jsonSvcItemArray.optJSONObject(position).optString("name");
        int itemId = mDB.servicedItemModel().queryServicedItemByName(itemName);
        isHistory = itemId > 0;

        // Split the Snackbar message according to whehter the history exists or not.
        String warning = (isHistory)?getString(R.string.pref_snackbar_delete_history)
                :getString(R.string.pref_snackbar_delete_no_history);

        // Set Snackbar action to remove the item from the list and the adapter should be rebound
        // when clicking the confirm.
        Snackbar snackbar = Snackbar.make(recyclerView, warning, Snackbar.LENGTH_LONG);
        snackbar.setAction("REMOVE", v -> {
            jsonSvcItemArray.remove(position);
            mAdapter.notifyItemRemoved(position);
            mAdapter.notifyItemRangeChanged(position, jsonSvcItemArray.length() - position, true);

            snackbar.dismiss();

        }).addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackkbar, int event) {
                if(event == Snackbar.Callback.DISMISS_EVENT_TIMEOUT) {
                    mAdapter.notifyItemChanged(position);
                }
            }
        });

        snackbar.show();


    }

    @Override
    public void changeServicePeriod(int position, SparseArray<String> sparseArray) {
        String mileage = sparseArray.valueAt(0);
        String month = sparseArray.valueAt(1);

        log.i("callback values: %s, %s", mileage, month);
        try {
            jsonSvcItemArray.optJSONObject(position).put("mileage", mileage);
            jsonSvcItemArray.optJSONObject(position).put("month", month);

        } catch(JSONException e) {e.printStackTrace();}
    }

    /*
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
    */


    // Method for switching the location of an service item using Up and Down button
    /*
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

     */
}
