package com.silverback.carman2.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.DataProviderContract;
import com.silverback.carman2.viewholders.FavoriteItemHolder;

public class FavoriteCursorAdapter extends RecyclerView.Adapter<FavoriteItemHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(FavoriteCursorAdapter.class);

    // Objects
    private Context context;
    private Cursor cursor;

    // Constructor
    public FavoriteCursorAdapter() {}

    /*
    public FavoriteCursorAdapter(Cursor cursor) {
        log.i("FavoriteCursorAdapter constructor");
        this.cursor = cursor;
    }
    */

    @NonNull
    @Override
    public FavoriteItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        CardView cardView = (CardView) LayoutInflater.from(context)
                .inflate(R.layout.view_card_favorite, parent, false);

        return new FavoriteItemHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteItemHolder holder, int position) {
        log.i("onBindViewHolder");
        int columnIndex = cursor.getColumnIndex(DataProviderContract.FAVORITE_PROVIDER_NAME);
        if(cursor.moveToLast()) log.i("Cursor data: %s", cursor.getString(columnIndex));
    }


    @Override
    public int getItemCount() {
        return cursor.getCount();
    }
}
