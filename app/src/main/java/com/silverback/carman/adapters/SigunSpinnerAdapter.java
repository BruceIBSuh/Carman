package com.silverback.carman.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.Opinet;

import java.util.ArrayList;
import java.util.List;

/**
 * This apdater extends BaseAdapter that implements not only ListAdapter for ListView but also
 * SpinnerAdapter for Spinner, used in RegisterDialogFragment and SettingSpinnerDlgFragment as well
 * in order to enlist Sigun names based on a selected Sido.
 */
public class SigunSpinnerAdapter extends BaseAdapter {
    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SigunSpinnerAdapter.class);

    // Object references
    private List<Opinet.DistrictCode> mDistrictCodeList;
    private final LayoutInflater inflater;
    private SpinnerViewHolder viewHolder;

    // Constructor
    public SigunSpinnerAdapter(Context context){
        mDistrictCodeList = new ArrayList<>();
        inflater = LayoutInflater.from(context);
    }


    @Override
    public int getCount() {
        return mDistrictCodeList.size();
    }

    @Override
    public Opinet.DistrictCode getItem(int position) {
        log.i("dDistrictCode : %s", mDistrictCodeList.get(position));
        return mDistrictCodeList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position ;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        if(convertView == null) {
            viewHolder = new SpinnerViewHolder();
            convertView = inflater.inflate(R.layout.spinner_district_entry, viewGroup, false);
            viewHolder.distName = convertView.findViewById(R.id.tv_spinner_entry);
            convertView.setTag(viewHolder);

        } else viewHolder = (SpinnerViewHolder) convertView.getTag();

        viewHolder.distName.setText(mDistrictCodeList.get(position).getDistrictName());
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View dropdownView, ViewGroup parent) {
        if(dropdownView == null) {
            viewHolder = new SpinnerViewHolder();
            dropdownView = inflater.inflate(R.layout.spinner_district_dropdown, parent, false);
            viewHolder.distName = dropdownView.findViewById(R.id.tv_spinner_dropdown);
            dropdownView.setTag(viewHolder);

        } else viewHolder = (SpinnerViewHolder)dropdownView.getTag();

        viewHolder.distName.setText(mDistrictCodeList.get(position).getDistrictName());
        return dropdownView;
    }

    // ViewHolder class
    private static class SpinnerViewHolder {
        TextView distName;
    }

    // The following methods are invoked by the parent fragment of SettingSpinnerDlgFragment or
    // RegisterDialogFragment as well.
    public void addSigunList(List<Opinet.DistrictCode> sigunList) {
        mDistrictCodeList = sigunList;
    }
    public void removeAll() {
        mDistrictCodeList.clear();
    }

}
