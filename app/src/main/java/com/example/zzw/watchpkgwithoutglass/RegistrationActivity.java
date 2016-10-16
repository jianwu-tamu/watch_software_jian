package com.example.zzw.watchpkgwithoutglass;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;

public class RegistrationActivity extends Activity {

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        final String name = getIntent().getStringExtra("user_name");
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                mTextView.setText(name);
            }
        });
    }


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            RegistrationActivity.this.finish();
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        mHandler.sendEmptyMessageDelayed(0, 10000);
    }
}
