/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.ocr.demo;

import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private AlertDialog.Builder alertDialog;
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET
    };
    private static final int PERMISSION_REQUEST_CODE = 0; // 系统权限管理页面的参数

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        alertDialog = new AlertDialog.Builder(this);

        findViewById(R.id.general_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GeneralActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.idcard_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, IDCardActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.bankcard_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BankCardActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.meter_card_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BankCardActivity.class);
                startActivity(intent);
            }
        });

        // 请选择您的初始化方式
        // initAccessToken();
        initAccessTokenWithAkSk();
        // OCR.getInstance().initWithToken(getApplicationContext(), "您获取的oauth access_token");

        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                && lacksPermissions(PERMISSIONS)) {
            requestPermissions(PERMISSIONS);
        }
    }

    private void initAccessToken() {

        OCR.getInstance().initAccessToken(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken accessToken) {
                String token = accessToken.getAccessToken();
            }

            @Override
            public void onError(OCRError error) {
                error.printStackTrace();
                alertText("licence方式获取token失败", error.getMessage());
            }
        }, getApplicationContext());
    }

    private void initAccessTokenWithAkSk() {
        OCR.getInstance().initAccessTokenWithAkSk(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken result) {
                String token = result.getAccessToken();
            }

            @Override
            public void onError(OCRError error) {
                error.printStackTrace();
                alertText("AK，SK方式获取token失败", error.getMessage());
            }
        }, getApplicationContext(), "XUAld8l5ElTSmhYf34QdeUuW", "TEHBw9dyj9Tfw3OHlviV8GE4Gehok6lu");
    }

    private void alertText(String title, String message) {
        boolean isNeedLoop = false;
        if (Looper.myLooper() == null) {
            Looper.prepare();
            isNeedLoop = true;
        }
        alertDialog.setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
        if (isNeedLoop) {
            Looper.loop();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[]  permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //initAccessToken();
        } else {
            Toast.makeText(getApplicationContext(), "需要android.permission.WRITE_EXTERNAL_STORAGE", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放内存资源
        OCR.getInstance().release();
    }

    private boolean lacksPermissions(String... permissions) {
        for (String permission : permissions) {
            if (lacksPermissions(permission)) {
                return true;
            }
        }
        return false;
    }

    //判断是否缺少权限
    private boolean lacksPermissions(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED;
    }

    //请求权限兼容低版本
    private void requestPermissions(String... permissions) {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    private boolean hasAllPermissionsGranted(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }
}
