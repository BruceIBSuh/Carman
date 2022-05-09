package com.silverback.carman.utils;

import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.core.content.ContextCompat;
import com.google.firebase.firestore.DocumentSnapshot;
import com.silverback.carman.R;
import com.silverback.carman.adapters.BoardCommentAdapter;
import com.silverback.carman.databinding.PopupCommentOverflowBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.lang.ref.WeakReference;

public class PopupDropdownUtil extends PopupWindow {
    private final LoggingHelper log = LoggingHelperFactory.create(PopupDropdownUtil.class);
    private DocumentSnapshot doc;
    private WeakReference<View> weakViewRef;
    private WeakReference<View> weakAnchorRef;
    //private View contentView;

    private static class ViewHolder {
        private static final PopupDropdownUtil sInstance = new PopupDropdownUtil();
    }

    public static PopupDropdownUtil getInstance() {
        return ViewHolder.sInstance;
    }

    public void setInitParams(View contentView, View anchorView, DocumentSnapshot doc){
        this.weakViewRef = new WeakReference<>(contentView);
        this.weakAnchorRef = new WeakReference<>(anchorView);
        this.doc = doc;
    }

    public PopupWindow createPopupWindow() {
        PopupWindow dropdown = new PopupWindow(weakViewRef.get(),
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        final DisplayMetrics metrics = weakViewRef.get().getContext().getResources().getDisplayMetrics();
        int offsetX = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, metrics);
        int offsetY = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, metrics);

        Drawable background = ContextCompat.getDrawable(
                weakViewRef.get().getContext(), android.R.drawable.editbox_background);
        dropdown.setBackgroundDrawable(background);
        dropdown.showAsDropDown(weakAnchorRef.get(), -offsetX, -offsetY);
        dropdown.setOverlapAnchor(true);
        dropdown.setOutsideTouchable(true);
        dropdown.update();

        return dropdown;
    }

    // The comment and reply overflow event handler
    /*
    private void showListPopupWindow(BoardCommentAdapter.ViewHolder holder, DocumentSnapshot doc) {
        //ListPopupWindow
        int[] size = measurePopupContentSize(arrayCommentAdapter);
        log.i("content size: %s, %s", size[0], size[1]);
        popupWindow = new ListPopupWindow(context);
        popupWindow.setAnchorView(holder.getOverflowView());
        popupWindow.setHeight(220);
        popupWindow.setContentWidth(180);
        popupWindow.setHorizontalOffset(-160);
        Drawable background = ContextCompat.getDrawable(context, android.R.drawable.editbox_background);
        popupWindow.setBackgroundDrawable(background);
        popupWindow.setModal(true);
        popupWindow.setOnItemClickListener((parent, view, i, l) -> {
            log.i("click");
        });
        popupWindow.setAdapter(arrayCommentAdapter);
        popupWindow.show();


        // PopupWindow
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.popup_comment_overflow, holder.commentBinding.getRoot(), false);
        PopupWindow dropdown = new PopupWindow(view,
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        Drawable background = ContextCompat.getDrawable(context, android.R.drawable.editbox_background);
        dropdown.setBackgroundDrawable(background);
        dropdown.showAsDropDown(holder.getOverflowView(), -120, -20);
        dropdown.setOverlapAnchor(true);
        dropdown.setOutsideTouchable(true);
        dropdown.update();

        // TextView event Listener
        PopupCommentOverflowBinding popupBinding = PopupCommentOverflowBinding.bind(view);
        if(viewerId.equals(doc.getString("user_id"))) {
            log.i("remove document");
            popupBinding.tvPopup1.setVisibility(View.VISIBLE);
            popupBinding.tvPopup1.setOnClickListener(v -> {
                log.i("remove listener");
                commentListener.deleteComment(doc.getId());
                dropdown.dismiss();
            });
        }

        popupBinding.tvPopup2.setOnClickListener(v -> {
            log.i("menu2");
            dropdown.dismiss();
        });
        popupBinding.tvPopup3.setOnClickListener(v -> {
            log.i("menu3");
            dropdown.dismiss();
        });

        // PopupMenu
        final String commentId = comment.getId();
        final String ownerId = comment.getString("user_id");

        PopupMenu popupMenu = new PopupMenu(styleWrapper, holder.getOverflowView());
        popupMenu.getMenuInflater().inflate(R.menu.popup_board_comment, popupMenu.getMenu());

        boolean visible = ownerId != null && ownerId.equals(viewerId);
        popupMenu.getMenu().findItem(R.id.board_comment_delete).setVisible(visible);

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            if(menuItem.getItemId() == R.id.board_comment_delete) {
                commentListener.deleteComment(commentId, position);

            } else if(menuItem.getItemId() == R.id.board_comment_report) {
                log.i("popup report");

            } else if(menuItem.getItemId() == R.id.board_comment_share) {
                log.i("popup share");
            }

            return false;
        });
        popupMenu.show();

    }

     */

}
