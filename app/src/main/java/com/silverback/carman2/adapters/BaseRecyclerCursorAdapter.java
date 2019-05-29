package com.silverback.carman2.adapters;

import android.database.Cursor;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public abstract class BaseRecyclerCursorAdapter<V extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<V> {

    // Objects
    private Cursor mCursor;

    // Fields
    private int mRowIdColumn;
    private boolean isDataVaild;


    public abstract void onBindViewHolder(V holder, Cursor cursor);

    public BaseRecyclerCursorAdapter(Cursor cursor) {
        setHasStableIds(true);
        swapCursor(cursor);
    }

    /*
    @NonNull
    @Override
    public V onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }
    */

    @Override
    public void onBindViewHolder(@NonNull V holder, int position) {
        if(!isDataVaild) throw new IllegalStateException("Cannot bind view holder due to invalid cursor");
        if(!mCursor.moveToPosition(position))
            throw new IllegalStateException("Could not move cursor to position " + position);

        onBindViewHolder(holder, mCursor);
    }

    @Override
    public int getItemCount() {
        if(isDataVaild) return mCursor.getCount();
        else return 0;
    }

    private void swapCursor(Cursor newCursor) {
        if(newCursor == mCursor) return;

        if(newCursor != null) {
            mCursor = newCursor;
            isDataVaild = true;
            notifyDataSetChanged();
        } else {
            notifyItemRangeRemoved(0, getItemCount()); //what' this for?
            mCursor = null;
            isDataVaild = false;
            mRowIdColumn = -1;
        }
    }
}
