package com.silverback.carman.adapters;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.adapter.FragmentViewHolder;

import com.silverback.carman.fragments.MainContentPagerFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.util.List;

public class MainExpPagerAdapter extends FragmentStateAdapter {
    private static final LoggingHelper log = LoggingHelperFactory.create(MainExpPagerAdapter.class);
    private static final int NUM_PAGES = 2;

    private MainContentPagerFragment targetFragment;

    public MainExpPagerAdapter(FragmentActivity fa) {
        super(fa);
    }

    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return targetFragment = MainContentPagerFragment.newInstance(position);
    }

    @Override
    public void onBindViewHolder(
            @NonNull FragmentViewHolder holder, int position, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) super.onBindViewHolder(holder, position, payloads);
        else {
            if(position == 0) {
                log.i("MainExpPagerAdapter payloads: %s, %s", payloads.get(0), targetFragment);
                targetFragment.reload((int)payloads.get(0));
                notifyItemChanged(0);
                //notifyDataSetChanged();
                /*
                holder.itemView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View view) {

                    }

                    @Override
                    public void onViewDetachedFromWindow(View view) {

                    }
                });

                 */

            }


        }
    }
}
