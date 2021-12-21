package com.example.audiorecorddemo.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.audiorecorddemo.R;
import com.example.audiorecorddemo.view.widget.VoiceImageView;

import java.io.File;
import java.util.List;

/**
 * @author xiayiye5
 * @date 2021/12/17 10:11
 */
public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoHolder> {
    private List<File> list;
    /**
     * 当前播放动画的位置
     */
    private int mCurrentPlayAnimPosition = -1;

    @NonNull
    @Override
    public VideoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.community_adapter_chat_list_right_voice, parent, false);
        return new VideoHolder(rootView);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoHolder holder, int position) {
        holder.ivVoice.setOnClickListener(view -> onItemClickListener.onItemClick(position));
        if (mCurrentPlayAnimPosition == position) {
            holder.ivVoice.startPlay();
        } else {
            holder.ivVoice.stopPlay();
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void setNewData(List<File> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    static class VideoHolder extends RecyclerView.ViewHolder {

        private final VoiceImageView ivVoice;

        public VideoHolder(@NonNull View itemView) {
            super(itemView);
            ivVoice = itemView.findViewById(R.id.iv_voice);
        }
    }

    public interface OnItemClickListener {
        /**
         * 点击item方法
         *
         * @param position 返回点击position
         */
        void onItemClick(int position);
    }

    OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    /**
     * 开始播放动画
     *
     * @param position 位置
     */
    public void startPlayAnim(int position) {
        mCurrentPlayAnimPosition = position;
        notifyDataSetChanged();
    }

    /**
     * 停止播放动画
     */
    public void stopPlayAnim() {
        mCurrentPlayAnimPosition = -1;
        notifyDataSetChanged();
    }
}
