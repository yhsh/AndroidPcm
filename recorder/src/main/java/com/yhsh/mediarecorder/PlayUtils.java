package com.yhsh.mediarecorder;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

/**
 * @author xiayiye5
 * @date 2021/12/17 13:22
 * 使用MediaPlayer播放
 */
public class PlayUtils {
    private MediaPlayer _mediaPlayer;

    private PlayUtils() {
    }

    private static final PlayUtils PLAY_UTILS = new PlayUtils();

    public static PlayUtils getInstance() {
        return PLAY_UTILS;
    }

    public void startPlay(Context mContext, File filePath) {
        resetMediaPlayer();
        Uri audioUri = Uri.fromFile(filePath);
        this._mediaPlayer = new MediaPlayer();
        this._mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            resetMediaPlayer();
            return true;
        });
        try {
            this._mediaPlayer.setDataSource(mContext, audioUri);
            this._mediaPlayer.setAudioStreamType(3);
            this._mediaPlayer.prepare();
            this._mediaPlayer.start();
            Toast.makeText(mContext, "正在开始播放", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(mContext, "播放出错了" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void resetMediaPlayer() {
        if (this._mediaPlayer != null) {
            try {
                this._mediaPlayer.stop();
                this._mediaPlayer.reset();
                this._mediaPlayer.release();
                this._mediaPlayer = null;
            } catch (IllegalStateException var2) {
                var2.printStackTrace();
            }
        }

    }


}
