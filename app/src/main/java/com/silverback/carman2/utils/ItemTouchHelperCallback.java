package com.silverback.carman2.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.adapters.SettingServiceItemAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

/**
 * Drag and Drop Helper class
 */


public class ItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private static final LoggingHelper log = LoggingHelperFactory.create(ItemTouchHelperCallback.class);

    // Objects
    private RecyclerItemMoveListener mListener;

    public interface RecyclerItemMoveListener {
        void onItemMove(int from, int to);
        void onItemRemove(int pos);
    }

    // Constructor
    public ItemTouchHelperCallback(SettingServiceItemAdapter listener) {
        mListener = listener;
    }


    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {

        mListener.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        log.i("onSiped: %s %s", viewHolder.getAdapterPosition(), direction);
        mListener.onItemRemove(viewHolder.getAdapterPosition());
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }
}
