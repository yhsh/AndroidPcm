package com.yhsh.mediarecorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * @author Zheng Cong
 * @date 2021/12/17 16:37
 */
public class RecordPcmUtils {
    private static final String TAG = "RecordPcmUtils";
    int SAMPLE_RATE_IN_HZ = 44100;
    int CHANNEL_CONFIGURATION = AudioFormat.CHANNEL_IN_MONO;
    int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    //    val instance = AudioRecordRecorderService();
    private static final RecordPcmUtils RECORD_PCM_UTILS = new RecordPcmUtils();
    private AudioRecord mAudioRecord;
    private String outputFilePath;
    private Thread recordThread;
    private boolean isRecording;
    private int bufferSizeInBytes;

    private RecordPcmUtils() {
    }

    public static RecordPcmUtils getInstance() {
        return RECORD_PCM_UTILS;
    }

    public void initMeta() {
        mAudioRecord.release();

        bufferSizeInBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, CHANNEL_CONFIGURATION, AUDIO_FORMAT);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_IN_HZ, CHANNEL_CONFIGURATION, AUDIO_FORMAT, bufferSizeInBytes);

        if (mAudioRecord == null || mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            SAMPLE_RATE_IN_HZ = 16000;
            bufferSizeInBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, CHANNEL_CONFIGURATION, AUDIO_FORMAT);
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_IN_HZ, CHANNEL_CONFIGURATION, AUDIO_FORMAT, bufferSizeInBytes);
        }

        if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.d(TAG, "出错了");
        }
    }

    public void start(String filePath) {
        if (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
//            startRecording();
        }
        isRecording = true;
        recordThread = new Thread(new RecordThread(), "RecordThread");
        outputFilePath = filePath;
        recordThread.start();
    }

    class RecordThread implements Runnable {

        @Override
        public void run() {
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(outputFilePath);
                byte[] audioSamples = new byte[bufferSizeInBytes];
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            while (isRecording) {
//                int audioSampleSize = getAudioRecordBuffer(bufferSizeInBytes, audioSamples)
//                if (audioSampleSize != 0) {
//                    outputStream.write(audioSamples);
//                }

            }
        }
    }
}
