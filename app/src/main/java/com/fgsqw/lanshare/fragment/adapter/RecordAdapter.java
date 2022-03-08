package com.fgsqw.lanshare.fragment.adapter;


import android.annotation.SuppressLint;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.pojo.RecordFile;
import com.fgsqw.lanshare.utils.FileUtil;
import com.fgsqw.lanshare.utils.mUtil;

import java.util.List;


public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {
    private List<RecordFile> filelist;
    OnItemClickListener onItemClickListener;
    public static final int NOTIFY_PROGRESS = 1000;
    public static final int NOTIFY_MESSAGE = 1001;

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView mSize;
        ImageView mIcon;
        TextView mName;
        TextView mMessage;
        TextView mCencel;
        ProgressBar mProgressBar;
        View view;


        public ViewHolder(View view) {
            super(view);
            mName = view.findViewById(R.id.record_item_img_tv_name);
            mIcon = view.findViewById(R.id.record_item_img);
            mSize = view.findViewById(R.id.record_item_img_tv_size);
            mMessage = view.findViewById(R.id.record_item_img_tv_message);
            mCencel = view.findViewById(R.id.record_item_img_tv_cencel);
            mProgressBar = view.findViewById(R.id.record_item_prog);
            this.view = view;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, List payloads) {
        RecordFile recordFile = filelist.get(position);
        if (payloads.isEmpty()) {
            holder.mName.setText(mUtil.StringSize(recordFile.getName(), 30));
            holder.mIcon.setBackgroundResource(R.drawable.ic_file);
            holder.mSize.setText(FileUtil.computeSize(recordFile.getLength()));
            holder.mProgressBar.setProgress(recordFile.getProgress());

            if (recordFile.getSuccess() != null) {
                holder.mProgressBar.setVisibility(View.GONE);
                holder.mMessage.setVisibility(View.VISIBLE);
                holder.mMessage.setText(recordFile.getMessage());
                if (!recordFile.getSuccess()) {
                    holder.mMessage.setTextColor(Color.RED);
                } else {
                    holder.mMessage.setTextColor(0xFF939393);
                }
            } else {
                holder.mProgressBar.setVisibility(View.VISIBLE);
                holder.mMessage.setVisibility(View.GONE);
                holder.mMessage.setTextColor(0xFF939393);
            }

            if (onItemClickListener != null) {
                holder.mCencel.setOnClickListener(v -> onItemClickListener.onCloseClick(position));
                holder.view.setOnClickListener(p11 -> onItemClickListener.onClick(position));
            }


        } else /*if (payloads.get(0) instanceof Integer)*/ {
            int cmd = (int) payloads.get(0);
            if (cmd == NOTIFY_PROGRESS) {
                holder.mProgressBar.setProgress(recordFile.getProgress());
            } else if (cmd == NOTIFY_MESSAGE) {
                holder.mProgressBar.setVisibility(View.GONE);
                holder.mMessage.setVisibility(View.VISIBLE);
                holder.mMessage.setText(recordFile.getMessage());
                if (!recordFile.getSuccess()) {
                    holder.mMessage.setTextColor(Color.RED);
                }
            }

        }

    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public RecordAdapter(List<RecordFile> filelist) {
        this.filelist = filelist;
    }


    @SuppressLint("NotifyDataSetChanged")
    public void refresh(List<RecordFile> filelist) {
        this.filelist = filelist;
        notifyDataSetChanged();
    }

    public void updateProgress(RecordFile recordFile) {
        int index = filelist.indexOf(recordFile);
        if (index > -1) {
            // 局部刷新
            notifyItemChanged(index, NOTIFY_PROGRESS);
        }
    }

    public void updateMessage(RecordFile recordFile) {
        int index = filelist.indexOf(recordFile);
        if (index > -1) {
            // 局部刷新
            notifyItemChanged(index, NOTIFY_MESSAGE);
        }

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup p1, int viewType) {
        View view = LayoutInflater.from(p1.getContext()).inflate(R.layout.record_item, p1, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {

    }


    @Override
    public int getItemCount() {
        return filelist.size();
    }

    public interface OnItemClickListener {
        void onClick(int position);

        void onCloseClick(int position);

        void onLongClick(int position);
    }

}