package com.silverback.carman.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.Lists;
import com.silverback.carman.BaseActivity;
import com.silverback.carman.MainActivity;
import com.silverback.carman.R;
import com.silverback.carman.databinding.MainRecyclerEvCollapsedBinding;
import com.silverback.carman.databinding.MainRecyclerEvExpandedBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.StationEvRunnable;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class StationEvAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final LoggingHelper log = LoggingHelperFactory.create(StationEvAdapter.class);
    public static final int VIEW_COLLAPSED = 0;
    public static final int VIEW_EXPANDED = 1;

    private final OnExpandItemClicked callback;
    private final AsyncListDiffer<MainActivity.MultiTypeEvItem> mDiffer;
    private final DecimalFormat df;
    private Context context;

    public interface OnExpandItemClicked {
        void onEvExpandIconClicked(String name, int position, int count);
    }

    public StationEvAdapter(OnExpandItemClicked callback) {
        this.callback = callback;
        mDiffer = new AsyncListDiffer<>(this, DIFF_CALLBACK_EV);
        df = (DecimalFormat) NumberFormat.getInstance(Locale.getDefault());
        df.applyPattern("#,###");
        df.setDecimalSeparatorAlwaysShown(false);
    }

    public void submitEvList(List<MainActivity.MultiTypeEvItem> evList) {
        mDiffer.submitList(Lists.newArrayList(evList)/*, postingAdapterCallback::onSubmitPostingListDone*/);
    }

    private static class CollapsedViewHolder extends RecyclerView.ViewHolder {
        private final MainRecyclerEvCollapsedBinding binding;
        public CollapsedViewHolder(View itemView) {
            super(itemView);
            binding = MainRecyclerEvCollapsedBinding.bind(itemView);
        }

        ImageView getChgrStateView() { return binding.ivChgrStatus; }
        TextView getOpenChgrCountView() { return binding.tvCntOpen; }
        TextView getEvStationName() {
            return binding.tvEvName;
        }
        TextView getDistanceView() { return binding.tvDistance;}
        TextView getCntChgrView() { return binding.tvCntChgr; }
        TextView getLimitDetailView() { return binding.tvLimitDetail; }
        TextView getChgrTypeView() { return binding.tvChgrType;}
        TextView getChgrUpdate() { return binding.tvChgrUpdate; }
        ImageView getImageView() { return binding.imgviewExpand; }



    }

    private static class ExpandedViewHolder extends RecyclerView.ViewHolder {
        private final MainRecyclerEvExpandedBinding binding;
        public ExpandedViewHolder(View itemView) {
            super(itemView);
            binding = MainRecyclerEvExpandedBinding.bind(itemView);
        }
        
        ImageView getChgrStateView() { return binding.imgviewExpanded; }
        TextView getChgrIdView() { return binding.tvChgrId; }
        TextView getChgrLabel() { return binding.tvChgrLabel; }
        TextView getChgrDetail() { return binding.tvChgrDetail; }
        TextView getChgrOutput() { return binding.tvChgrOutput; }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();

        switch(viewType) {
            case VIEW_COLLAPSED:
                View view = LayoutInflater.from(context).inflate(R.layout.main_recycler_ev_collapsed, parent, false);
                return new CollapsedViewHolder(view);

            case VIEW_EXPANDED:
            default:
                View view2 = LayoutInflater.from(context).inflate(R.layout.main_recycler_ev_expanded, parent, false);
                return new ExpandedViewHolder(view2);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        //if(evList.size() == 0) return;
        //StationEvRunnable.Item info = evList.get(position);
        int viewType = mDiffer.getCurrentList().get(position).getViewType();
        MainActivity.MultiTypeEvItem multiItem = mDiffer.getCurrentList().get(position);
        StationEvRunnable.Item info = multiItem.getItem();

        switch(viewType) {
            case VIEW_COLLAPSED:
                CollapsedViewHolder holder = (CollapsedViewHolder)viewHolder;
                String limitDetail = (TextUtils.isEmpty(info.getLimitDetail()))?
                        context.getString(R.string.main_ev_no_limit) : info.getLimitDetail();
                // Set the indicator to show a charger state.
                holder.getChgrStateView().setImageDrawable(getCollapsedDrawable(info));

                // Set the number of chargers available to charge
                if(info.getCntOpen() > 0) {
                    final String cntChargerOpen = String.valueOf(info.getCntOpen());
                    holder.getOpenChgrCountView().setText(cntChargerOpen);
                } else holder.getOpenChgrCountView().setText("");

                // Set the EV station name
                String stnName = info.getStdNm().replaceAll(MainActivity.regexEvName, "");
                holder.getEvStationName().setText(stnName);

                // Set the number of chargers in an EV station and the multi-charger image overlapped.
                // Click event handler is set to expand chargers in an Ev station.
                holder.getCntChgrView().setText(String.valueOf(info.getCntCharger()));
                if(info.getCntCharger() > 1) {
                    holder.getImageView().setVisibility(View.VISIBLE);
                    holder.getImageView().setOnClickListener(view -> callback.onEvExpandIconClicked(
                            stnName, holder.getBindingAdapterPosition(), info.getCntCharger()
                    ));
                } else holder.getImageView().setVisibility(View.GONE);

                holder.getLimitDetailView().setText(limitDetail);
                holder.getDistanceView().setText(df.format(info.getDistance()));
                holder.getChgrTypeView().setText(info.getChgerType());

                String update = (!TextUtils.isEmpty(info.getStatUpdDt())) ?
                        BaseActivity.formatMilliseconds("HH:mm:ss", Long.parseLong(info.getStatUpdDt())) :
                        context.getString(R.string.main_chgr_no_data);
                holder.getChgrUpdate().setText(update);

                break;

            case VIEW_EXPANDED:
                ExpandedViewHolder expandedHolder = (ExpandedViewHolder) viewHolder;
                String name = info.getStdNm() + "_" + info.getChgerId();

                expandedHolder.getChgrIdView().setText(name);
                expandedHolder.getChgrStateView().setImageDrawable(getExpandedDrawable(info.getStat()));

                String output = (!TextUtils.isEmpty(info.getOutput())) ?
                        info.getOutput().concat(context.getString(R.string.unit_kw)) :
                        context.getString(R.string.main_chgr_no_data);
                expandedHolder.getChgrOutput().setText(output);

                String lastUpdate = (!TextUtils.isEmpty(info.getStatUpdDt())) ?
                        info.getStatUpdDt() : context.getString(R.string.main_chgr_no_data);


                String detail = "";
                String label = "";
                switch(info.getStat()) {
                    case 2:
                        label = context.getString(R.string.main_chgr_label_last);
                        detail = (!TextUtils.isEmpty(info.getLastTedt()) ?
                                BaseActivity.formatMilliseconds("hh:mm:ss a", Long.parseLong(info.getLastTedt())):
                                context.getString(R.string.main_chgr_no_data));

                        break;
                    case 3:
                        label = context.getString(R.string.main_chgr_label_start);
                        detail = (!TextUtils.isEmpty(info.getLastTedt()) ?
                                BaseActivity.formatMilliseconds("hh:mm:ss a", Long.parseLong(info.getNowTsdt())):
                                context.getString(R.string.main_chgr_no_data));
                        break;
                    case 1:
                        label = context.getString(R.string.main_chgr_label_reason);
                        detail = context.getString(R.string.main_chgr_state_disconntected);
                        break;
                    case 4:
                        label = context.getString(R.string.main_chgr_label_reason);
                        detail = context.getString(R.string.main_chgr_state_inspect);
                        break;
                    case 5:
                        label = context.getString(R.string.main_chgr_label_reason);
                        detail = context.getString(R.string.main_chgr_state_shutdown);
                        break;
                    case 9:
                        label = context.getString(R.string.main_chgr_label_reason);
                        detail = context.getString(R.string.main_chgr_state_nosignal);
                        break;
                }

                expandedHolder.getChgrLabel().setText(label);
                expandedHolder.getChgrDetail().setText(detail);
                /*
                if(info.getStat() == 3) {
                    String chargeTime = BaseActivity.formatMilliseconds("h:mm:ss a", Long.parseLong(info.getNowTsdt()));
                    expandedHolder.getChgrDetail().setVisibility(View.VISIBLE);
                    expandedHolder.getChgrDetail().setText(chargeTime);
                }
                 */
        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position,
                                 @NonNull List<Object> payloads) {
        //holder.setIsRecyclable(false);
        if (payloads.isEmpty()) super.onBindViewHolder(holder, position, payloads);
        else {
            log.i("partial binding: %s", payloads);
        }
    }


    @Override
    public int getItemCount() {
        //return evList.size();
        //log.i("mDiffer size: %s", mDiffer.getCurrentList().size());
        return mDiffer.getCurrentList().size();
    }

    @Override
    public int getItemViewType(int position) {
        return mDiffer.getCurrentList().get(position).getViewType();
    }

    private Drawable getCollapsedDrawable(StationEvRunnable.Item info) {
        int resource;
        if(info.getCntOpen() > 0) resource = android.R.drawable.presence_online;
        else if(info.getCntCharging() > 0) resource = android.R.drawable.presence_away;
        else resource = android.R.drawable.presence_busy;
        return ContextCompat.getDrawable(context, resource);
    }

    private Drawable getExpandedDrawable(int state) {
        int resource;
        switch(state) {
            case 2: resource = android.R.drawable.presence_online; break;
            case 3: resource = android.R.drawable.presence_away; break;
            default: resource = android.R.drawable.presence_busy; break;
        }

        return ContextCompat.getDrawable(context, resource);
    }

    private static final DiffUtil.ItemCallback<MainActivity.MultiTypeEvItem> DIFF_CALLBACK_EV =
            new DiffUtil.ItemCallback<MainActivity.MultiTypeEvItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull MainActivity.MultiTypeEvItem oldItem,
                                               @NonNull MainActivity.MultiTypeEvItem newItem) {
                    return oldItem.hashCode() == newItem.hashCode();
                }

                @Override
                public boolean areContentsTheSame(@NonNull MainActivity.MultiTypeEvItem oldItem,
                                                  @NonNull MainActivity.MultiTypeEvItem newItem) {

                    if(oldItem.getViewType() == newItem.getViewType()) {
                        if(oldItem.getViewType() == 0) {
                            return oldItem.getItemId().equals(newItem.getItemId());
                        } else if(oldItem.getViewType() == 1) {
                            return oldItem.getItemId().equals(newItem.getItemId());
                        }
                    }
                    return false;

                }

                public Object getChangePayload(@NonNull MainActivity.MultiTypeEvItem oldItem,
                                               @NonNull MainActivity.MultiTypeEvItem newItem) {
                    return super.getChangePayload(oldItem, newItem);
                }
            };


    public enum ChargerState {
        MAL_COMMUNICATION(1), CHARGER_READY(2), CHARGER_OCCUPIED(3), CHARGER_CLOSED(4),
        CHARGER_CHECK(5), CHARGER_UNKNOWN(9);

        int state;
        ChargerState(int state) {
            this.state = state;
        }

        public int getState() {
            return state;
        }
    }



}
