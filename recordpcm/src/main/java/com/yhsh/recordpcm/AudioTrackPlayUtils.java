package com.yhsh.recordpcm;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Zheng Cong
 * @date 2021/12/17 14:24
 * 使用AudioTrack播放
 */
public class AudioTrackPlayUtils {
    private static final String TAG = "AudioTrackPlayUtils";
    private static final AudioTrackPlayUtils AUDIO_TRACK_PLAY_UTILS = new AudioTrackPlayUtils();
    private Status mStatus;
    private WeakReference<Context> mContext;
    private String mFilePath;

    private AudioTrackPlayUtils() {
    }

    public static AudioTrackPlayUtils getInstance() {
        return AUDIO_TRACK_PLAY_UTILS;
    }

    //下面这个是采样率需要知道音频文件的采样率对比设置，例如我目前播放的文件的采样率是48Khz
    int sampleRateInHz = 48_000;
    int channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * 播放本地wav方法
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void playWavRecord() {
        int bufferSize = AudioTrack.getMinBufferSize(48_000, channelConfig, audioFormat);
        AudioTrack audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(48_000)
                        .setChannelMask(channelConfig)
                        .build())
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferSize)
                .build();
        FileInputStream fis = null;
        try {
            File recordFile = new File("/storage/emulated/0/Android/data/com.yhsh.recordpcm/cache/audio_cache/music.wav");
            fis = new FileInputStream(recordFile);
            audioTrack.play();
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                Log.e(TAG, "开始写入播放数据 " + len);
                audioTrack.write(buffer, 0, len);
            }

        } catch (Exception e) {
            Log.e(TAG, "播放异常: " + e.getMessage());
        } finally {
            Log.e(TAG, "播放完毕 ");
            audioTrack.stop();
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    ThreadPoolExecutor mExecutorService = new ThreadPoolExecutor(3, 5, 1, TimeUnit.MINUTES, new LinkedBlockingDeque<>(10));
    AudioTrack mAudioTrack;
    int mBufferSizeInBytes;

    public void createAudioTrack(WeakReference<Context> context, String filePath) throws IllegalStateException {
        mContext = context;
        mFilePath = filePath;
        mBufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        Log.d(TAG, "打印读写大小" + mBufferSizeInBytes);
        if (mBufferSizeInBytes <= 0) {
            throw new IllegalStateException("AudioTrack is not available " + mBufferSizeInBytes);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mAudioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRateInHz)
                            .setChannelMask(channelConfig)
                            .build())
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(mBufferSizeInBytes)
                    .build();
        } else {
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelConfig, audioFormat, mBufferSizeInBytes, AudioTrack.MODE_STREAM);
        }
        mStatus = Status.STATUS_READY;
        start();
    }

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    public void start() throws IllegalStateException {
        if (mStatus == Status.STATUS_NO_READY || mAudioTrack == null) {
            throw new IllegalStateException("播放器尚未初始化");
        }
        if (mStatus == Status.STATUS_START) {
            throw new IllegalStateException("正在播放...");
        }
        Log.d(TAG, "===start===");
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    playAudioData();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext.get(), "播放出错", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
        mStatus = Status.STATUS_START;
    }

    private void playAudioData() throws IOException {
        DataInputStream dis = null;
        try {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext.get(), "播放开始", Toast.LENGTH_SHORT).show();
                }
            });
            FileInputStream fis = new FileInputStream(mFilePath);
            dis = new DataInputStream(new BufferedInputStream(fis));
            byte[] bytes = new byte[mBufferSizeInBytes];
            int len;
            long startTime = System.currentTimeMillis();
            mAudioTrack.play();
            // write 是阻塞的方法
            while ((len = dis.read(bytes)) != -1 && mStatus == Status.STATUS_START) {
                mAudioTrack.write(bytes, 0, len);
            }
            Log.d(TAG, "总共耗时" + (System.currentTimeMillis() - startTime) / 1000 + "s");
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext.get(), "播放结束", Toast.LENGTH_SHORT).show();
                }
            });
        } finally {
            if (dis != null) {
                dis.close();
            }
        }
    }

    enum Status {
        STATUS_START,
        STATUS_READY,
        STATUS_NO_READY,
        STATUS_STOP
    }

    public void stop() throws IllegalStateException {
        Log.d(TAG, "===stop===");
        if (mStatus == Status.STATUS_NO_READY || mStatus == Status.STATUS_READY) {
            throw new IllegalStateException("播放尚未开始");
        } else {
            mAudioTrack.stop();
            mStatus = Status.STATUS_STOP;
            release();
        }
    }

    public void release() {
        Log.d(TAG, "==release===");
        if (mAudioTrack != null) {
            mAudioTrack.release();
            mAudioTrack = null;
        }
        mStatus = Status.STATUS_NO_READY;
    }
}
