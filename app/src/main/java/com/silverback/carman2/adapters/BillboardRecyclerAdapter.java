package com.silverback.carman2.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.List;

public class BillboardRecyclerAdapter extends RecyclerView.Adapter<BillboardRecyclerAdapter.BillboardHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BillboardRecyclerAdapter.class);

    // Objects
    private FirebaseFirestore firestore;
    private List<QueryDocumentSnapshot> dataList;
    private CardView cardView;

    // Constructor
    public BillboardRecyclerAdapter(List<QueryDocumentSnapshot> dataList) {
        super();
        this.dataList = dataList;
        log.i("DataList: %s", dataList.size());
    }


    @NonNull
    @Override
    public BillboardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_billbaord, parent, false);


        return new BillboardHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull BillboardHolder holder, int position) {
        QueryDocumentSnapshot document = dataList.get(position);
        log.i("Snapshot title: %s", document.getString("title"));
        holder.tvPostTitle.setText(document.getString("title"));
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }


    class BillboardHolder extends RecyclerView.ViewHolder {

        TextView tvPostTitle;

        BillboardHolder(CardView view){
            super(view);
            tvPostTitle = view.findViewById(R.id.tv_post_title);
        }
    }

}
