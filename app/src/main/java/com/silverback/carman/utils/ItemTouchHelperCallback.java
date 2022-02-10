package com.silverback.carman.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman.R;
import com.silverback.carman.adapters.SettingFavAdapter;
import com.silverback.carman.adapters.SettingServiceItemAdapter;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

/**
 * Drag and Drop Helper class
 */
public class ItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private static final LoggingHelper log = LoggingHelperFactory.create(ItemTouchHelperCallback.class);
    // Objects
    private RecyclerItemMoveListener mListener;
    private Drawable iconTrash;
    private ColorDrawable background;

    public interface RecyclerItemMoveListener {
        void onDragItem(int from, int to);
        void onDeleteItem(int pos);
    }

    // Constructor
    public ItemTouchHelperCallback(Context context, RecyclerView.Adapter listener) {
        // Set the Background color and the icon in the item background.
        background = new ColorDrawable(Color.parseColor("#606060"));
        iconTrash = ContextCompat.getDrawable(context, R.drawable.ic_trash);

       if(listener instanceof SettingServiceItemAdapter) mListener = (SettingServiceItemAdapter)listener;
       else if(listener instanceof SettingFavAdapter) mListener = (SettingFavAdapter)listener;
    }


    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.LEFT;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {

        iconTrash.setVisible(false, true);
        mListener.onDragItem(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        log.i("onSiped: %s %s", viewHolder.getAdapterPosition(), direction);
        mListener.onDeleteItem(viewHolder.getAdapterPosition());

    }

    @Override
    public void onChildDraw(
            @NonNull Canvas canvas, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
            float dx, float dy, int actionState, boolean isCurrentlyActive) {

        super.onChildDraw(canvas, recyclerView, viewHolder, dx, dy, actionState, isCurrentlyActive);
        View itemView = viewHolder.itemView;
        int backgroundCornerOffset = 20;

        int iconMargin = (itemView.getHeight() - iconTrash.getIntrinsicHeight()) / 2;
        int iconTop = itemView.getTop() + (itemView.getHeight() - iconTrash.getIntrinsicHeight()) / 2;
        int iconBottom = iconTop + iconTrash.getIntrinsicHeight();

        // Swiping to the Right
        if(dx < 0) {
            log.i("swipe dx: %s", dx);
            background.setBounds(itemView.getRight() + ((int)dx), itemView.getTop(),
                    itemView.getRight() + backgroundCornerOffset, itemView.getBottom());

            int centerWidth = (itemView.getRight() - itemView.getLeft()) / 2;
            int centerHeight = (itemView.getBottom() - itemView.getTop()) / 2;
            iconTrash.setBounds(centerWidth - iconTrash.getIntrinsicWidth() / 2, iconTop,
                    centerWidth + iconTrash.getIntrinsicWidth() / 2, iconBottom);

        // Swiping to the Left
        } else if (dx > 0) {

        } else {
            background.setBounds(0, 0, 0, 0);
        }

        background.draw(canvas);
        iconTrash.draw(canvas);

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
