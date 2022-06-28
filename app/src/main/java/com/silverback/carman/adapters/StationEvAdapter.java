package com.silverback.carman.adapters;

import static com.silverback.carman.BoardActivity.AD_VIEW_TYPE;
import static com.silverback.carman.BoardActivity.CONTENT_VIEW_TYPE;

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
import com.silverback.carman.R;
import com.silverback.carman.databinding.MainRecyclerviewEvBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.StationEvRunnable;
import com.silverback.carman.utils.MultiTypePostingItem;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class StationEvAdapter extends RecyclerView.Adapter<StationEvAdapter.ViewHolder> {
    private static final LoggingHelper log = LoggingHelperFactory.create(StationEvAdapter.class);

    private OnExpandItemClicked callback;
    private final AsyncListDiffer<StationEvRunnable.Item> mDiffer;
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

    public void submitEvList(List<StationEvRunnable.Item> evList) {
        mDiffer.submitList(Lists.newArrayList(evList)/*, postingAdapterCallback::onSubmitPostingListDone*/);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final MainRecyclerviewEvBinding binding;
        public ViewHolder(View itemView) {
            super(itemView);
            binding = MainRecyclerviewEvBinding.bind(itemView);
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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.main_recyclerview_ev, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //if(evList.size() == 0) return;
        //StationEvRunnable.Item info = evList.get(position);
        StationEvRunnable.Item info = mDiffer.getCurrentList().get(position);
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
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        //holder.setIsRecyclable(false);
        if (payloads.isEmpty()) super.onBindViewHolder(holder, position, payloads);
        else {
            log.i("partial binding: %s", payloads);
            for(Object obj : payloads) {
                if(obj instanceof StationEvRunnable.Item) {
                    holder.getEvStationName().setText(((StationEvRunnable.Item) obj).getChgerId());
                }

            }
        }
    }


    @Override
    public int getItemCount() {
        //return evList.size();
        //log.i("mDiffer size: %s", mDiffer.getCurrentList().size());
        return mDiffer.getCurrentList().size();
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

    private static final DiffUtil.ItemCallback<StationEvRunnable.Item> DIFF_CALLBACK_EV =
            new DiffUtil.ItemCallback<StationEvRunnable.Item>() {
                @Override
                public boolean areItemsTheSame(@NonNull StationEvRunnable.Item oldItem,
                                               @NonNull StationEvRunnable.Item newItem) {
                    return oldItem.hashCode() == newItem.hashCode();
                }

                @Override
                public boolean areContentsTheSame(@NonNull StationEvRunnable.Item oldItem,
                                                  @NonNull StationEvRunnable.Item newItem) {
                    /*
                    if(oldItem.getViewType() == newItem.getViewType()) {
                        if(oldItem.getViewType() == CONTENT_VIEW_TYPE) {
                            return oldItem.getDocument().equals(newItem.getDocument());
                        } else if(oldItem.getViewType() == AD_VIEW_TYPE) {
                            return oldItem.getViewId() == newItem.getViewId();
                        }
                    }

                     */
                    return oldItem.getChgerId().equals(newItem.getChgerId());

                }

                public Object getChangePayload(@NonNull StationEvRunnable.Item oldItem,
                                               @NonNull StationEvRunnable.Item newItem) {
                    return super.getChangePayload(oldItem, newItem);
                }
            };

}
