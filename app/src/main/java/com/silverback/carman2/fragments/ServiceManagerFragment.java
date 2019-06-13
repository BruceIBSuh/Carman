package com.silverback.carman2.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.ServiceListAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.views.ServiceRecyclerView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import org.json.JSONArray;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class ServiceManagerFragment extends Fragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceManagerFragment.class);

    // Objects
    private Calendar calendar;
    private FragmentSharedModel viewModel;
    private ServiceListAdapter mAdapter;
    private ServiceRecyclerView serviceRecyclerView;

    public ServiceManagerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ViewModel instance
        if(getActivity() != null) {
            viewModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
        }

        calendar = Calendar.getInstance(Locale.getDefault());
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        String[] serviceItems = getResources().getStringArray(R.array.service_item_list);
        JSONArray jsonArray = new JSONArray(Arrays.asList(serviceItems));
        String json = jsonArray.toString();

        View localView = inflater.inflate(R.layout.fragment_service_manager, container, false);
        View boxview = localView.findViewById(R.id.view);
        log.i("BoxView height: %s %s", boxview.getHeight(), boxview.getMeasuredHeight());

        TextView tvDate = localView.findViewById(R.id.tv_service_date);
        Button btnDate = localView.findViewById(R.id.btn_date);
        ImageButton favorite = localView.findViewById(R.id.imgbtn_favorite);


        String date = BaseActivity.formatMilliseconds(getString(R.string.date_format_1), System.currentTimeMillis());
        tvDate.setText(date);
        favorite.setBackgroundResource(R.drawable.btn_favorite);

        serviceRecyclerView = localView.findViewById(R.id.recycler_service);
        //String jsonItems = getArguments().getString("serviceItems");
        mAdapter = new ServiceListAdapter(json);
        serviceRecyclerView.setAdapter(mAdapter);

        // Inflate the layout for this fragment
        return localView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Notify ExpensePagerFragment of the current fragment to load the recent 5 expense data from
        // ServiceTable.
        viewModel.setCurrentFragment(this);
    }

}
