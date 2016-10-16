package com.example.zzw.watchpkgwithoutglass;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by Sebastian on 7/22/2016. This fragment implements a simple keyboard to give the user
 * the ability to enter his/her name. It then passes that name back to the main activity.
 */
public class KeyboardActivity extends WearableActivity {
    private String ip = "";
    private View mView;
    private TextView mTextTitle;
    private boolean lastIsSpace = false;
    private Intent intent;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.keyboard);
        intent = new Intent();

        mTextTitle = (TextView) findViewById(R.id.text_title);

        if (!ip.equals("")) {
            mTextTitle.setText(ip + "|"); // Add a cursor to the name
        }

        // Get all the buttons
        final Button num1 = (Button) findViewById(R.id.num1);
        final Button num2 = (Button) findViewById(R.id.num2);
        final Button num3 = (Button) findViewById(R.id.num3);
        final Button num4 = (Button) findViewById(R.id.num4);
        final Button num5 = (Button) findViewById(R.id.num5);
        final Button num6 = (Button) findViewById(R.id.num6);
        final Button num7 = (Button) findViewById(R.id.num7);
        final Button num8 = (Button) findViewById(R.id.num8);
        final Button num9 = (Button) findViewById(R.id.num9);
        final Button num0 = (Button) findViewById(R.id.num0);
        final Button bksp = (Button) findViewById(R.id.backspace);
        final Button point = (Button) findViewById(R.id.point);
        final Button enter = (Button) findViewById(R.id.enter);

        // Set up the buttons to do what we want them to do
        num1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ip = ip.replace("|", "");
                ip += "1|";
                mTextTitle.setText(ip);
            }
        });

        num2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ip = ip.replace("|", "");
                ip += "2|";
                mTextTitle.setText(ip);
            }
        });

        num3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ip = ip.replace("|", "");
                ip += "3|";
                mTextTitle.setText(ip);
            }
        });

        num4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ip = ip.replace("|", "");
                ip += "4|";
                mTextTitle.setText(ip);
            }
        });

        num5.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ip = ip.replace("|", "");
                ip += "5|";
                mTextTitle.setText(ip);
            }
        });

        num6.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ip = ip.replace("|", "");
                ip += "6|";
                mTextTitle.setText(ip);
            }
        });

        num7.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ip = ip.replace("|", "");
                ip += "7|";
                mTextTitle.setText(ip);
            }
        });

        num8.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ip = ip.replace("|", "");
                ip += "8|";
                mTextTitle.setText(ip);
            }
        });

        num9.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ip = ip.replace("|", "");
                ip += "9|";
                mTextTitle.setText(ip);
            }
        });

        num0.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ip = ip.replace("|", "");
                ip += "0|";
                mTextTitle.setText(ip);
            }
        });

        point.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ip = ip.replace("|", ""); // Remove cursor from name
                ip = ip + ".|"; // Add a space and the cursor back
                mTextTitle.setText(ip);
            }
        });

        bksp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ip = ip.replace("|", ""); // Remove cursor
                if (ip.length() > 0) { // Make sure the name already has some characters (in case of accidental presses, etc)
                    ip = ip.substring(0, ip.length() - 1); // Get rid of last character
                    ip = ip + "|"; // Re-add cursor
                    mTextTitle.setText(ip);
                    if (ip.length() > 1) {
                        Character lastChar = ip.charAt(ip.length() - 2);
                    }
                }
            }
        });

        enter.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ip = ip.replace("|", ""); // Get rid of cursor
                mTextTitle.setText("");
                intent.putExtra("ip", ip);
                setResult(Activity.RESULT_OK, intent);
                finish();
                ip = "";
            }
        });
    }
}
