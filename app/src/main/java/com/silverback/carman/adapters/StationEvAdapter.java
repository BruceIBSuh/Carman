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

    private OnExpandItemClicked callback;
    private final AsyncListDiffer<MainActivity.MultiTypeEvItem> mDiffer;
    private final DecimalFormat df;
    private Context context;

    public interface OnExpandItemClicked {
        void onExpandIconClicked(String name, int position, int count);
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

        ImageView getChgrStatusView() { return binding.ivChgrStatus; }
        TextView getEvStationName() {
            return binding.tvEvName;
        }
        TextView getDistanceView() { return binding.tvDistance;}
        //TextView getChargerIdView() { return binding.tvChgrId; }
        TextView getBizNameView() { return binding.tvBizName; }
        TextView getLimitDetailView() { return binding.tvLimitDetail; }
        TextView getChargerTypeView() { return binding.tvChgrType;}
        ImageView getImageView() { return binding.imgviewExpand; }


    }

    private static class ExpandedViewHolder extends RecyclerView.ViewHolder {
        private final MainRecyclerEvExpandedBinding binding;
        public ExpandedViewHolder(View itemView) {
            super(itemView);
            binding = MainRecyclerEvExpandedBinding.bind(itemView);
        }
        
        ImageView getChgrStatusView() { return binding.imgviewExpanded; }
        TextView getChgrIdView() { return binding.tvChgrId; }
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
                String charId = "_" + Integer.parseInt(info.getChgerId());

                log.i("collapsed station status: %s", info.getIsAnyChargerOpen());
                holder.getChgrStatusView().setImageDrawable(getCollapsedStatusImage(info.getIsAnyChargerOpen()));

                holder.getEvStationName().setText(info.getStdNm().replaceAll("\\d*\\([\\w\\s]*\\)", ""));
                //holder.getChargerIdView().setText(charId);
                //holder.getChargerStatus().setText(String.valueOf(info.getStat()));
                //holder.getBizNameView().setText(info.getBusiNm());
                holder.getBizNameView().setText(String.valueOf(info.getCntCharger()));
                if(info.getCntCharger() > 1) {
                    holder.getImageView().setVisibility(View.VISIBLE);
                    holder.getImageView().setOnClickListener(view -> callback.onExpandIconClicked(
                            info.getStdNm(), holder.getBindingAdapterPosition(), info.getCntCharger()
                    ));
                } else holder.getImageView().setVisibility(View.GONE);

                holder.getLimitDetailView().setText(limitDetail);

                holder.getDistanceView().setText(df.format(info.getDistance()));
                holder.getChargerTypeView().setText(info.getChgerType());
                break;

            case VIEW_EXPANDED:
                ExpandedViewHolder expandedHolder = (ExpandedViewHolder) viewHolder;
                int res = (info.getStat() == 2)?android.R.drawable.presence_online: android.R.drawable.presence_away;
                String name = info.getStdNm() + "_" + info.getChgerId();

                expandedHolder.getChgrIdView().setText(name);
                expandedHolder.getChgrStatusView().setImageDrawable(ContextCompat.getDrawable(context, res));
                break;
        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
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

    private Drawable getCollapsedStatusImage(boolean isChargerOpen) {
        int res = (isChargerOpen)?android.R.drawable.presence_online:android.R.drawable.presence_away;
        return ContextCompat.getDrawable(context, res);
    }

    private Drawable getStatusImage(int status) {
        switch(status) {
            case 1: return ContextCompat.getDrawable(context, android.R.drawable.presence_away);
            case 2: return ContextCompat.getDrawable(context, android.R.drawable.presence_online);
            case 3: return ContextCompat.getDrawable(context, android.R.drawable.presence_busy);
            case 4: case 9: return ContextCompat.getDrawable(context, R.drawable.bg_circle_gray);
            default: return null;
        }
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

}
