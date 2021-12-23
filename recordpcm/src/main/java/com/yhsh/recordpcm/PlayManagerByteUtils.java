package com.yhsh.recordpcm;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Zheng Cong
 * @date 2021/12/17 18:10
 */
public class PlayManagerByteUtils {
    private static final String TAG = "PlayManagerUtil";
    private WeakReference<Context> weakReference;
    private File recordFile;
    private boolean isRecording;
    /**
     * 是否播放,默认播放
     */
    private boolean isPlay = true;
    /**
     * 最多只能存2条记录
     */
    private final List<File> filePathList = new ArrayList<>(2);

    /**
     * 16K采集率
     * 44.1K采集率
     */
//    int sampleRateInHz = 44100;
    int sampleRateInHz = 16000;
    /**
     * 格式
     */
//    int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    int channelConfiguration = AudioFormat.CHANNEL_OUT_STEREO;
//    int channelConfiguration = AudioFormat.CHANNEL_IN_STEREO;
    /**
     * 16Bit
     */
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    private PlayManagerByteUtils() {
    }

    private static final PlayManagerByteUtils PLAY_MANAGER_UTILS = new PlayManagerByteUtils();

    public static PlayManagerByteUtils getInstance() {
        return PLAY_MANAGER_UTILS;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());

    ThreadPoolExecutor mExecutorService = new ThreadPoolExecutor(3, 5, 1, TimeUnit.MINUTES, new LinkedBlockingDeque<>(10));

    public void startRecord(WeakReference<Context> weakReference) {
        this.weakReference = weakReference;
        Log.e(TAG, "开始录音");
        //生成PCM文件
        String fileName = DateFormat.format("yyyyMMdd_HHmmss", Calendar.getInstance(Locale.getDefault())) + "_xiayiye5.pcm";
        File file = new File(weakReference.get().getExternalCacheDir(), "audio_cache");
        if (!file.exists()) {
            file.mkdir();
        }
        String audioSaveDir = file.getAbsolutePath();
        Log.e(TAG, audioSaveDir);
        recordFile = new File(audioSaveDir, fileName);
        Log.e(TAG, "生成文件" + recordFile);
        //如果存在，就先删除再创建
        if (recordFile.exists()) {
            recordFile.delete();
            Log.e(TAG, "删除文件");
        }
        try {
            recordFile.createNewFile();
            Log.e(TAG, "创建文件");
        } catch (IOException e) {
            Log.e(TAG, "未能创建");
            throw new IllegalStateException("未能创建" + recordFile.toString());
        }
        if (filePathList.size() == 2) {
            filePathList.clear();
        }
        filePathList.add(recordFile);
        try {
            //输出流
            OutputStream os = new FileOutputStream(recordFile);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(bos);
            int bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, AudioFormat.CHANNEL_IN_STEREO, audioEncoding);
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, AudioFormat.CHANNEL_IN_STEREO, audioEncoding, bufferSize);

            byte[] buffer = new byte[bufferSize];
            audioRecord.startRecording();
            Log.e(TAG, "开始录音");
            long startTime = System.currentTimeMillis();
            isRecording = true;
            while (isRecording) {
                int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);
                for (int i = 0; i < bufferReadResult; i++) {
                    dos.writeByte(buffer[i]);
                }
            }
            audioRecord.stop();
            dos.close();
            long endTime = System.currentTimeMillis();
            float recordTime = (endTime - startTime) / 1000f;
            Log.e(TAG, "录音总时长：" + recordTime + "s");
        } catch (Throwable t) {
            Log.e(TAG, "录音失败");
            showToast("录音失败");
        }
    }

    /**
     * 播放pcm流的方法,一次性读取所有Pcm流，读完后在开始播放
     */
    public void playAllRecord() {
        if (recordFile == null) {
            return;
        }
//        recordFile = new File("/storage/emulated/0/Android/data/com.yhsh.recordpcm/cache/audio_cache/music.wav");
        //读取文件
        int musicLength = (int) (recordFile.length() / 2);
        byte[] music = new byte[musicLength];
        try {
            InputStream is = new FileInputStream(recordFile);
            BufferedInputStream bis = new BufferedInputStream(is);
            DataInputStream dis = new DataInputStream(bis);
            int i = 0;
            while (dis.available() > 0) {
                music[i] = dis.readByte();
                i++;
            }
            dis.close();
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelConfiguration, audioEncoding, musicLength * 2, AudioTrack.MODE_STREAM);
            audioTrack.play();
            audioTrack.write(music, 0, musicLength);
            audioTrack.stop();
        } catch (Throwable e) {
            Log.e(TAG, "播放失败" + e.getMessage());
            showToast("播放失败" + e.getMessage());
            e.printStackTrace();
        }
    }


    public void playPcm(boolean isChecked) {
        if (isChecked) {
            //两首一起播放
            for (File recordFiles : filePathList) {
                mExecutorService.execute(() -> playPcmData(recordFiles));
            }
        } else {
            //只播放最后一次录音
            playPcmData(recordFile);
        }
    }

    /**
     * 播放Pcm流,边读取边播
     */
    private void playPcmData(File recordFiles) {
        Log.e(TAG, "打印线程" + Thread.currentThread().getName());
        try {
//            recordFiles = new File("/storage/emulated/0/Android/data/com.yhsh.recordpcm/cache/audio_cache/music.wav");
            DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(recordFiles)));
            //最小缓存区
            int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, AudioFormat.CHANNEL_OUT_STEREO, audioEncoding);
            AudioTrack player = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, AudioFormat.CHANNEL_OUT_STEREO, audioEncoding, bufferSizeInBytes, AudioTrack.MODE_STREAM);
            byte[] data = new byte[bufferSizeInBytes];
            long startTime = System.currentTimeMillis();
            //开始播放
            player.play();
            while (true) {
                int i = 0;
                while (dis.available() > 0 && i < data.length) {
                    if (isPlay) {
                        data[i] = dis.readByte();
                        i++;
                    } else {
                        Log.e(TAG, "暂停播放了：");
                    }
                }
                player.write(data, 0, data.length);
                //表示读取完了
                if (i != bufferSizeInBytes) {
                    player.stop();//停止播放
                    player.release();//释放资源
                    dis.close();
                    long endTime = System.currentTimeMillis();
                    float time = (endTime - startTime) / 1000f;
                    Log.e(TAG, "播放完成总计：" + time + "s");
                    showToast("播放完成总时长为：" + time + "s");
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "播放异常: " + e.getMessage());
            showToast("播放异常！！！！");
            e.printStackTrace();
        }
    }

    public File getRecordFile() {
        return recordFile;
    }

    public void setRecord(boolean isRecording) {
        this.isRecording = isRecording;
    }

    private void showToast(String msg) {
        if (null != weakReference) {
            handler.post(() -> Toast.makeText(weakReference.get(), msg, Toast.LENGTH_LONG).show());
        }
    }

    public void setPlayStatus(boolean isPlay) {
        this.isPlay = isPlay;
    }
}


