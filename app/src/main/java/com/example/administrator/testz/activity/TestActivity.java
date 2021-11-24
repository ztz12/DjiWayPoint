package com.example.administrator.testz.activity;


import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.administrator.testz.R;


public class TestActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mLoginButton;
    private Button mWayPointButton;
    private Button mHotPointButton;
    private Button mVirtualStickyButton;
    private Button mCameraButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test2);

        initView();

        setListener();
    }

    private void initView() {
        mLoginButton = findViewById(R.id.btn_login);
        mWayPointButton = findViewById(R.id.btn_way_point);
        mHotPointButton = findViewById(R.id.btn_hot_point);
        mVirtualStickyButton = findViewById(R.id.btn_virtual_sticky_point);
        mCameraButton = findViewById(R.id.btn_way_camera);
    }

    private void setListener() {
        mLoginButton.setOnClickListener(this);
        mWayPointButton.setOnClickListener(this);
        mHotPointButton.setOnClickListener(this);
        mVirtualStickyButton.setOnClickListener(this);
        mCameraButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_login:
                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivity(loginIntent);
                break;
            case R.id.btn_way_point:
                Intent wayPointIntent = new Intent(this, MainActivity.class);
                startActivity(wayPointIntent);
                break;
            case R.id.btn_hot_point:
                Intent hotPointIntent = new Intent(this, HotPointActivity.class);
                startActivity(hotPointIntent);
                break;
            case R.id.btn_virtual_sticky_point:
                Intent virtualStickyIntent = new Intent(this, VirtualStickyActivity.class);
                startActivity(virtualStickyIntent);
                break;
            case R.id.btn_way_camera:
                Intent cameraIntent = new Intent(this, CameraActivity.class);
                startActivity(cameraIntent);
                break;
            default:
                break;
        }
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(TestActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
