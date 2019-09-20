package com.silverback.carman2.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.SettingFavoritePagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingFavoriteListFragment extends Fragment implements
        ViewPager.OnPageChangeListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingFavoriteListFragment.class);

    // Objects

    // Constructor
    public SettingFavoriteListFragment() {
        // Required empty public constructor

    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View localView = inflater.inflate(R.layout.fragment_favorite_pager, container, false);

        // TabLayout. addTab(tabLayout.newTab().setText("hello") doesn't work when ViewPager
        // is combined by invoking setUpWithViewPager.
        TabLayout tabLayout = localView.findViewById(R.id.tab_favorite);
        tabLayout.addTab(tabLayout.newTab());
        tabLayout.addTab(tabLayout.newTab());

        // ViewPager
        ViewPager viewPager = localView.findViewById(R.id.viewpager_favorite);
        SettingFavoritePagerAdapter adapter = new SettingFavoritePagerAdapter(getFragmentManager());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(this);
        // setupWithViewPager() prevents tab titles from displaying. To avoid this, override
        // onPageTitle() defined in FragmentPagerAdapter.
        tabLayout.setupWithViewPager(viewPager);

        setHasOptionsMenu(true); // to call onOptionsItemSelected() in Fragment

        return localView;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home && getActivity() != null) {
            item.setEnabled(false);
            getActivity().onBackPressed();
            log.i("onOptionsItemSelected in Frgment");
        }

        return true;
    }


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
    @Override
    public void onPageSelected(int position) {}
    @Override
    public void onPageScrollStateChanged(int state) {}

}
