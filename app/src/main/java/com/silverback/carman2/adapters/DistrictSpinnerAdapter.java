package com.silverback.carman2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.List;

public class DistrictSpinnerAdapter extends BaseAdapter {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(DistrictSpinnerAdapter.class);

    // Object references
    private List<String> distList;
    private LayoutInflater inflater;
    private SpinnerViewHolder viewHolder;

    // Constructor

    public DistrictSpinnerAdapter(Context context){
        distList = new ArrayList<>();
        inflater = LayoutInflater.from(context);
    }


    @Override
    public int getCount() {
        return distList.size();
    }

    @Override
    public String getItem(int position) {
        return distList.get(position) ;
    }

    @Override
    public long getItemId(int position) {
        return position ;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {

        if (convertView == null) {
            viewHolder = new SpinnerViewHolder();
            convertView = inflater.inflate(R.layout.dialog_district_spinner, viewGroup, false);
            viewHolder.distName = convertView.findViewById(R.id.tv_spinner_item);
            convertView.setTag(viewHolder);

        } else viewHolder = (SpinnerViewHolder) convertView.getTag();

        //Some bugs are alive around here due to index out of range!!!!!
        viewHolder.distName.setText(distList.get(position));

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View dropdownView, ViewGroup parent) {

        if(dropdownView == null) {
            viewHolder = new SpinnerViewHolder();
            dropdownView = inflater.inflate(R.layout.dialog_dist_spinner_dropdown, parent, false);
            viewHolder.distName = dropdownView.findViewById(R.id.chktv_dist_name);
            dropdownView.setTag(viewHolder);
        } else viewHolder = (SpinnerViewHolder)dropdownView.getTag();

        viewHolder.distName.setText(distList.get(position));

        return dropdownView;
    }

    private class SpinnerViewHolder {
        TextView distName;
    }

    public void addItem(String item) {
        distList.add(item);
    }

    // Invoked by postExecute() of SigunListTask to clear the currently populated items.
    public void removeAll() {
        distList.clear();
    }

}
