package com.silverback.carman.adapters;

import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class MainContentAdapter extends RecyclerView.Adapter<MainContentAdapter.ViewHolder> {
    //private final LoggingHelper log = LoggingHelperFactory.create(MainContentAdapter.class);

    // Objects
    //public MainContentNotificationBinding binding; //DataBiding in JetPack

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View.
        }
    }

    public static class MainItemDecoration extends RecyclerView.ItemDecoration {
        private final int margin;
        private final int columns;
        public MainItemDecoration(int margin, int columns) {
            this.margin = margin;
            this.columns = columns;
        }

        @Override
        public void getItemOffsets(
                @NonNull Rect outRect, @NonNull View view,
                @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            //super.getItemOffsets(outRect, view, parent, state);
            int position = parent.getChildLayoutPosition(view);
            outRect.bottom = margin;
            if(position < columns) {
                outRect.top = margin;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        switch(viewType) {
            case 0:
                ViewGroup notiView = (ViewGroup)LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.main_content_notification, viewGroup, false);

                //binding = DataBindingUtil.bind(notiView);

                /*
                MainContentNotificationBinding binding = DataBindingUtil.inflate(
                        LayoutInflater.from(viewGroup.getContext()),
                        R.layout.main_content_notification, parent, false);
                */

                /*
                new AsyncLayoutInflater(viewGroup.getContext()).inflate(
                        R.layout.main_content_notification, null, (view, resId, parent) -> {
                            binding = DataBindingUtil.bind(view);
                        });
                */

                return new ViewHolder(notiView);

            case 1:
                ViewGroup gasExpenseView = (ViewGroup)LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.main_content_expense_gas, viewGroup, false);
                return new ViewHolder(gasExpenseView);
            case 2:
                ViewGroup svcExpenseView = (ViewGroup)LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.main_content_expense_svc, viewGroup, false);
                return new ViewHolder(svcExpenseView);

            case 3:
                ViewGroup bannerView = (ViewGroup)LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.main_content_banner_1, viewGroup, false);
                return new ViewHolder(bannerView);

            default: return new ViewHolder(null);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        switch(position) {

            case 0:
                //binding.setPost(post); //set + name value in the layout(Pascal naming style)
                RecyclerView recyclerView = holder.itemView.findViewById(R.id.recyclerview);
                FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                firestore.collection("admin_post").orderBy("timestamp", Query.Direction.DESCENDING).limit(5)
                        .get()
                        .addOnSuccessListener(querySnapshots -> {
                            //DispRecentPost post = new DispRecentPost(querySnapshots);
                            //binding.setPost(post);
                            //binding.recyclerview.setAdapter(adapter);
                            RecentPostAdapter recentPostAdapter = new RecentPostAdapter(querySnapshots);
                            recyclerView.setAdapter(recentPostAdapter);
                        });

                break;

            case 1:
                break;
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}