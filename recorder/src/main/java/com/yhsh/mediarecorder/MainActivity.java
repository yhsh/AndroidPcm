package com.yhsh.mediarecorder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.yhsh.mediarecorder.widget.RecordVoicePopWindow;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Locale;

/**
 * @author DELL
 */
public class MainActivity extends AppCompatActivity {
    private MediaRecorder mMediaRecorder;
    private static final String TAG = "MainActivity";
    private String filePath;
    private RecordVoicePopWindow recordVoicePopWindow;
    private File recordFile;
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (null != mMediaRecorder) {
                int db = mMediaRecorder.getMaxAmplitude() / 600;
                recordVoicePopWindow.updateCurrentVolume(db);
                sendEmptyMessageDelayed(0, 100);
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recordVoicePopWindow = new RecordVoicePopWindow(this);
        //请求麦克风权限
        requestPermission();
        Button btSpeak = findViewById(R.id.bt_speak);
        btSpeak.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startRecord();
                    break;
                case MotionEvent.ACTION_UP:
                    stopRecord();
                    //自动播放
//                    PlayUtils.getInstance().startPlay(this, new File("/storage/emulated/0/Android/data/com.yhsh.mediarecorder/cache/audio_cache/20211217_122950.wav"));
//                    PlayUtils.getInstance().startPlay(this, recordFile);
//                    AudioTrackPlayUtils.getInstance().createAudioTrack(new WeakReference<Context>(getApplicationContext()), "/storage/emulated/0/Android/data/com.yhsh.mediarecorder/cache/audio_cache/20211217_122950.wav");
                    AudioTrackPlayUtils.getInstance().createAudioTrack(new WeakReference<Context>(getApplicationContext()), recordFile.getAbsolutePath());
                    break;
                default:
                    break;
            }
            return true;
        });
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WAKE_LOCK}, 10086);
        }
    }

    private void stopRecord() {
        if (recordVoicePopWindow != null) {
            recordVoicePopWindow.dismiss();
        }
        try {
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;
            filePath = "";
        } catch (RuntimeException e) {
            Log.e(TAG, e.toString());
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;

            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }

            filePath = "";
        }
    }

    public void startRecord() {
        if (recordVoicePopWindow != null) {
            recordVoicePopWindow.showCancelTipView();
            recordVoicePopWindow.showRecordingTipView();
            recordVoicePopWindow.showAsDropDown(getWindow().getDecorView());
        }
        // 开始录音
        /* ①Initial：实例化MediaRecorder对象 */
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }
        try {
            // 设置麦克风
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            /*
             * ②设置输出文件的格式：THREE_GPP/MPEG-4/RAW_AMR/Default THREE_GPP(3gp格式
             * ，H263视频/ARM音频编码)、MPEG-4、RAW_AMR(只支持音频且音频编码要求为AMR_NB)
             */
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            /* ②设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default 声音的（波形）的采样 */
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            String fileName = DateFormat.format("yyyyMMdd_HHmmss", Calendar.getInstance(Locale.getDefault())) + ".m4a";
            File file = new File(getExternalCacheDir(), "audio_cache");
            if (!file.exists()) {
                file.mkdir();
            }
            String audioSaveDir = file.getAbsolutePath();
            Log.d(TAG, audioSaveDir);
            recordFile = new File(audioSaveDir, fileName);
            filePath = recordFile.getAbsolutePath();
            Log.d(TAG, filePath);
            /* ③准备 */
            mMediaRecorder.setOutputFile(filePath);
            mMediaRecorder.prepare();
            /* ④开始 */
            mMediaRecorder.start();
            //开始更新说话的音量
            handler.sendEmptyMessageDelayed(0, 500);
        } catch (IllegalStateException e) {
            Log.i(TAG, "call startAmr(File mRecAudioFile) failed!" + e.getMessage());
        } catch (IOException e) {
            Log.i(TAG, "call startAmr(File mRecAudioFile) failed!" + e.getMessage());
        }
    }
}