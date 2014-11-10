package de.eww.bibapp.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.eww.bibapp.R;
import de.eww.bibapp.model.PaiaItem;

/**
 * Created by christoph on 07.11.14.
 */
public class BorrowedAdapter extends RecyclerView.Adapter<BorrowedAdapter.ViewHolder> {

    private List<PaiaItem> mItemList;
    private Context mContext;
    private boolean mIsRequestPermitted;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView mAbout;
        public TextView mSignature;
        public TextView mDate;
        public TextView mQueue;
        public TextView mRenewals;
        public TextView mStatus;
        public CheckBox mCheckBox;

        public ViewHolder(View itemView) {
            super(itemView);

            mAbout = (TextView) itemView.findViewById(R.id.about);
            mSignature = (TextView) itemView.findViewById(R.id.signature);
            mDate = (TextView) itemView.findViewById(R.id.duedate);
            mQueue = (TextView) itemView.findViewById(R.id.queue);
            mRenewals = (TextView) itemView.findViewById(R.id.renewals);
            mStatus = (TextView) itemView.findViewById(R.id.status);
            mCheckBox = (CheckBox) itemView.findViewById(R.id.checkbox);
        }
    }

    // Suitable constructor for list type
    public BorrowedAdapter(List<PaiaItem> itemList, Context context, boolean isRequestPermitted) {
        mItemList = itemList;
        mContext = context;
        mIsRequestPermitted = isRequestPermitted;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new view
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_borrowed_view, parent, false);

        // Set the view's size, margins, paddings and layout parameters
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        PaiaItem item = mItemList.get(position);

        holder.mAbout.setText(item.getAbout());
        holder.mSignature.setText(item.getLabel());

        SimpleDateFormat dateFormatWithoutTime = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);
        SimpleDateFormat dateFormatWithTime = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY);

        if (item.getEndDate() != null) {
            Date endDate = item.getEndDate();
            String endDateString = dateFormatWithTime.format(endDate);

            if (endDateString.contains("00:00")) {
                endDateString = dateFormatWithoutTime.format(endDate);
            }

            holder.mDate.setText(endDateString);
        } else if (item.getDueDate() != null) {
            Date dueDate = item.getDueDate();

            String dueDateString = dateFormatWithTime.format(dueDate);

            if (dueDateString.contains("00:00")) {
                dueDateString = dateFormatWithoutTime.format(dueDate);
            }

            holder.mDate.setText(dueDateString);
        }

        holder.mQueue.setText(String.valueOf(item.getQueue()));
        holder.mRenewals.setText(String.valueOf(item.getRenewals()));

        int statusCode = item.getStatus();
        String[] statusTranslations = mContext.getResources().getStringArray(R.array.paia_service_status);
        holder.mStatus.setText(statusTranslations[statusCode]);

        if (item.isCanRenew() && mIsRequestPermitted) {
            holder.mCheckBox.setVisibility(View.VISIBLE);
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return (mItemList != null ? mItemList.size() : 0);
    }
}
