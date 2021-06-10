package com.silverback.carman.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

import com.silverback.carman.R;
import com.silverback.carman.database.FavoriteProviderEntity;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.util.List;

public class FavoriteListAdapter extends BaseAdapter {

    private static final LoggingHelper log = LoggingHelperFactory.create(FavoriteListAdapter.class);

    // Objects
    private List<FavoriteProviderEntity> favoriteList;

    // Constructor
    public FavoriteListAdapter(List<FavoriteProviderEntity> list) {
        favoriteList = list;
    }

    @Override
    public int getCount() {
        return favoriteList.size();
    }

    @Override
    public Object getItem(int position) {
        return favoriteList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final Context context = parent.getContext();
        ListViewHolder viewHolder;
        CheckedTextView tvFavorite;

        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.listview_favorite, parent, false);
            viewHolder = new ListViewHolder();
            viewHolder.tvFavoriteName = convertView.findViewById(R.id.tv_favorite_name);
            viewHolder.tvFavoriteAddrs = convertView.findViewById(R.id.tv_favorite_addrs);
            convertView.setTag(viewHolder);

        } else viewHolder = (ListViewHolder)convertView.getTag();

        viewHolder.tvFavoriteName.setText(favoriteList.get(position).providerName);
        viewHolder.tvFavoriteAddrs.setText(favoriteList.get(position).address);

        return convertView;
    }

    public class ListViewHolder {
        CheckedTextView tvFavoriteName;
        TextView tvFavoriteAddrs;
    }
}
