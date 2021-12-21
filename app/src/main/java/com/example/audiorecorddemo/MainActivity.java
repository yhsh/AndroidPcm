package com.example.audiorecorddemo;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.audiorecorddemo.view.RecordVoicePopWindow;
import com.example.audiorecorddemo.view.VideoAdapter;
import com.example.audiorecorddemo.view.widget.RecordAudioButton;

import java.io.File;
import java.util.List;


public class MainActivity<T extends MainContract.Presenter> extends AppCompatActivity implements MainContract.View {

    LinearLayout mRoot;
    //消息列表
    RecyclerView mRvMsg;
    //底部录制按钮
    RecordAudioButton mBtnVoice;

    private Context mContext;
    /**
     * 适配器
     */
    private final VideoAdapter mAdapter = new VideoAdapter();
    //提示
    private RecordVoicePopWindow mRecordVoicePopWindow;
    private MainContract.Presenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mPresenter = new MainPresenter<MainContract.View>(this, this);
        setContentView(R.layout.activity_main);
        requestPermission();//请求麦克风权限
        initView();//初始化布局
        mPresenter.init();
    }

    private void initView() {
        mRoot = findViewById(R.id.root);
        mRvMsg = findViewById(R.id.rvMsg);
        mBtnVoice = findViewById(R.id.btnVoice);
        mBtnVoice.setOnVoiceButtonCallBack(new RecordAudioButton.OnVoiceButtonCallBack() {
            @Override
            public void onStartRecord() {
                mPresenter.startRecord();
            }

            @Override
            public void onStopRecord() {
                mPresenter.stopRecord();
            }

            @Override
            public void onWillCancelRecord() {
                mPresenter.willCancelRecord();
            }

            @Override
            public void onContinueRecord() {
                mPresenter.continueRecord();
            }
        });
        mRvMsg.setLayoutManager(new LinearLayoutManager(mContext));
        mAdapter.setOnItemClickListener(position -> mPresenter.startPlayRecord(position));
        mRvMsg.setAdapter(mAdapter);
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WAKE_LOCK}, 10086);
        }
    }


    @Override
    public void showList(List<File> list) {
        mAdapter.setNewData(list);
    }

    @Override
    public void showNormalTipView() {
        if (mRecordVoicePopWindow == null) {
            mRecordVoicePopWindow = new RecordVoicePopWindow(mContext);
        }
        mRecordVoicePopWindow.showAsDropDown(mRoot);
    }

    @Override
    public void showTimeOutTipView(int remainder) {
        if (mRecordVoicePopWindow != null) {
            mRecordVoicePopWindow.showTimeOutTipView(remainder);
        }
    }

    @Override
    public void showRecordingTipView() {
        if (mRecordVoicePopWindow != null) {
            mRecordVoicePopWindow.showRecordingTipView();
        }
    }

    @Override
    public void showRecordTooShortTipView() {
        if (mRecordVoicePopWindow != null) {
            mRecordVoicePopWindow.showRecordTooShortTipView();
        }
    }

    @Override
    public void showCancelTipView() {
        if (mRecordVoicePopWindow != null) {
            mRecordVoicePopWindow.showCancelTipView();
        }
    }

    @Override
    public void hideTipView() {
        if (mRecordVoicePopWindow != null) {
            mRecordVoicePopWindow.dismiss();
        }
    }

    @Override
    public void updateCurrentVolume(int db) {
        if (mRecordVoicePopWindow != null) {
            mRecordVoicePopWindow.updateCurrentVolume(db);
        }
    }

    @Override
    public void startPlayAnim(int position) {
        mAdapter.startPlayAnim(position);
    }

    @Override
    public void stopPlayAnim() {
        mAdapter.stopPlayAnim();
    }
}
