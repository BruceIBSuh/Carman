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
import com.silverback.carman2.models.Opinet;

import java.util.ArrayList;
import java.util.List;

public class DistrictSpinnerAdapter extends BaseAdapter {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(DistrictSpinnerAdapter.class);

    // Object references
    private List<Opinet.DistrictCode> mDistrictCodeList;
    private LayoutInflater inflater;
    private SpinnerViewHolder viewHolder;

    // Constructor

    public DistrictSpinnerAdapter(Context context){
        mDistrictCodeList = new ArrayList<>();
        inflater = LayoutInflater.from(context);
    }


    @Override
    public int getCount() {
        return mDistrictCodeList.size();
    }

    @Override
    public Opinet.DistrictCode getItem(int position) {
        return mDistrictCodeList.get(position) ;
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
        viewHolder.distName.setText(mDistrictCodeList.get(position).getDistrictName());

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

        viewHolder.distName.setText(mDistrictCodeList.get(position).getDistrictName());

        return dropdownView;
    }

    private class SpinnerViewHolder {
        TextView distName;
    }

    public void addItem(Opinet.DistrictCode obj) {
        mDistrictCodeList.add(obj);
    }

    // Invoked by postExecute() of SigunListTask to clear the currently populated items.
    public void removeAll() {
        mDistrictCodeList.clear();
    }

}