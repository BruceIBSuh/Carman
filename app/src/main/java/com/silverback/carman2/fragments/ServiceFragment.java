package com.silverback.carman2.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.ServiceListAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.views.ServiceRecyclerView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 */
public class ServiceFragment extends Fragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceFragment.class);

    // Objects
    private ServiceListAdapter mAdapter;
    private ServiceRecyclerView serviceRecyclerView;

    public ServiceFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        String[] serviceItems = getResources().getStringArray(R.array.service_item_list);
        JSONArray jsonArray = new JSONArray(Arrays.asList(serviceItems));
        String json = jsonArray.toString();

        View localView = inflater.inflate(R.layout.fragment_service, container, false);
        View boxview = localView.findViewById(R.id.view);
        log.i("BoxView height: %s %s", boxview.getHeight(), boxview.getMeasuredHeight());

        serviceRecyclerView = localView.findViewById(R.id.recycler_service);
        //String jsonItems = getArguments().getString("serviceItems");
        mAdapter = new ServiceListAdapter(json);
        serviceRecyclerView.setAdapter(mAdapter);

        // Inflate the layout for this fragment
        return localView;
    }

}
