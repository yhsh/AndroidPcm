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
import java.util.Calendar;
import java.util.Locale;

/**
 * @author Zheng Cong
 * @date 2021/12/17 18:10
 */
public class PlayManagerUtils {
    private static final String TAG = "PlayManagerUtils";
    private WeakReference<Context> weakReference;
    private File recordFile;
    private boolean isRecording;

    /**
     * 16K采集率
     */
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

    private PlayManagerUtils() {
    }

    private static final PlayManagerUtils PLAY_MANAGER_UTILS = new PlayManagerUtils();

    public static PlayManagerUtils getInstance() {
        return PLAY_MANAGER_UTILS;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());

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
        try {
            //输出流
            OutputStream os = new FileOutputStream(recordFile);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(bos);
            int bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, AudioFormat.CHANNEL_IN_STEREO, audioEncoding);
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, AudioFormat.CHANNEL_IN_STEREO, audioEncoding, bufferSize);

            short[] buffer = new short[bufferSize];
            audioRecord.startRecording();
            Log.e(TAG, "开始录音");
            isRecording = true;
            while (isRecording) {
                int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);
                for (int i = 0; i < bufferReadResult; i++) {
                    dos.writeShort(buffer[i]);
                }
            }
            audioRecord.stop();
            dos.close();
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
        short[] music = new short[musicLength];
        try {
            InputStream is = new FileInputStream(recordFile);
            BufferedInputStream bis = new BufferedInputStream(is);
            DataInputStream dis = new DataInputStream(bis);
            int i = 0;
            while (dis.available() > 0) {
                music[i] = dis.readShort();
                i++;
            }
            dis.close();
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelConfiguration, audioEncoding, musicLength * 2, AudioTrack.MODE_STREAM);
            audioTrack.play();
            audioTrack.write(music, 0, musicLength);
            audioTrack.stop();
        } catch (Throwable t) {
            Log.e(TAG, "播放失败");
            showToast("播放失败");
        }
    }

    /**
     * 播放Pcm流,边读取边播
     */
    public void playPcm() {
        try {
//            recordFile = new File("/storage/emulated/0/Android/data/com.yhsh.recordpcm/cache/audio_cache/music.wav");
            //从音频文件中读取声音
            DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(recordFile)));
            //最小缓存区
            int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, AudioFormat.CHANNEL_OUT_STEREO, audioEncoding);
            //创建AudioTrack对象   依次传入 :流类型、采样率（与采集的要一致）、音频通道（采集是IN 播放时OUT）、量化位数、最小缓冲区、模式
            AudioTrack player = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, AudioFormat.CHANNEL_OUT_STEREO, audioEncoding, bufferSizeInBytes, AudioTrack.MODE_STREAM);
            short[] data = new short[bufferSizeInBytes];
            //byte[] data = new byte[bufferSizeInBytes];
            //开始播放
            player.play();
            while (true) {
                int i = 0;
                while (dis.available() > 0 && i < data.length) {
                    //录音时write Byte 那么读取时就该为readByte要相互对应
                    data[i] = dis.readShort();
                    //data[i] = dis.readByte();
                    i++;
                }
                player.write(data, 0, data.length);
                //表示读取完了
                if (i != bufferSizeInBytes) {
                    player.stop();//停止播放
                    player.release();//释放资源
                    dis.close();
                    showToast("播放完成了！！！");
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
        handler.post(() -> Toast.makeText(weakReference.get(), msg, Toast.LENGTH_LONG).show());
    }
}


