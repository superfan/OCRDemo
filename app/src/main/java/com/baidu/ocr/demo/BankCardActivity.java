/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.ocr.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.BankCardParams;
import com.baidu.ocr.sdk.model.BankCardResult;
import com.baidu.ocr.ui.camera.CameraActivity;
import com.squareup.picasso.Picasso;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class BankCardActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_IMAGE = 101;
    private static final int REQUEST_CODE_CAMERA = 102;
    private TextView infoTextView;
    private ImageView imageView;
    private TextView textView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank_card);

        infoTextView = (TextView) findViewById(R.id.info_text_view);

        findViewById(R.id.gallery_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int ret = ActivityCompat.checkSelfPermission(BankCardActivity.this, Manifest.permission
                        .READ_EXTERNAL_STORAGE);
                if (ret != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(BankCardActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            1000);
                    return;
                }
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
            }
        });

        findViewById(R.id.camera_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setVisibility(View.GONE);
                textView.setVisibility(View.GONE);

                Intent intent = new Intent(BankCardActivity.this, CameraActivity.class);
                intent.putExtra(CameraActivity.KEY_OUTPUT_FILE_PATH,
                        FileUtil.getSaveFile(getApplication()).getAbsolutePath());
                intent.putExtra(CameraActivity.KEY_CONTENT_TYPE,
                        CameraActivity.CONTENT_TYPE_BANK_CARD);
                startActivityForResult(intent, REQUEST_CODE_CAMERA);
            }
        });

        imageView = (ImageView) findViewById(R.id.iv_response);
        textView = (TextView) findViewById(R.id.tv_response);
        progressBar = (ProgressBar) findViewById(R.id.pb_upload);

        myHandler = new MyHandler(this);
    }

    private void recBankCard(String filePath) {
        BankCardParams param = new BankCardParams();
        param.setImageFile(new File(filePath));
        OCR.getInstance().recognizeBankCard(param, new OnResultListener<BankCardResult>() {
            @Override
            public void onResult(BankCardResult result) {
                String res = String.format("卡号：%s\n类型：%s\n发卡行：%s",
                        result.getBankCardNumber(),
                        result.getBankCardType().name(),
                        result.getBankName());
                infoTextView.setText(res);
            }

            @Override
            public void onError(OCRError error) {
                infoTextView.setText(error.getMessage());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String filePath = getRealPathFromURI(uri);
            recBankCard2(filePath);
        }

        if (requestCode == REQUEST_CODE_CAMERA && resultCode == Activity.RESULT_OK) {
            recBankCard2(FileUtil.getSaveFile(getApplicationContext()).getAbsolutePath());
        }
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    private void recBankCard2(String filePath) {
        File srcFile = new File(filePath);
        if (!srcFile.exists()) {
            Toast.makeText(this, "照片不存在!", Toast.LENGTH_LONG).show();
            return;
        }

        File dir = new File(Environment.getExternalStorageDirectory(), "sh3h");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File destFile = new File(Environment.getExternalStorageDirectory(), String.format(Locale.CHINA, "sh3h/%d.jpg", System.currentTimeMillis()));
        if (!copyFile(filePath, destFile.getPath())) {
            return;
        }

        //OkHttpClient client = new OkHttpClient();
        HttpLoggingInterceptor logInterceptor = new HttpLoggingInterceptor(new HttpLogger());
        logInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .addNetworkInterceptor(logInterceptor)
                .build();

        progressBar.setVisibility(View.VISIBLE);
        String url = "http://128.1.6.50:5000/upload";
        RequestBody fileBody = RequestBody.create(MediaType.parse("image/jpeg"), destFile);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", destFile.getName(), fileBody).build();
        Request requestPostFile = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        client.newCall(requestPostFile).enqueue(new Callback() {
            private String text;

            @Override
            public void onFailure(Call call, final IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(BankCardActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                text = null;
                try {
                    if (response.body() != null) {
                        text = response.body().string();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (text == null) {
                    text = response.toString();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(BankCardActivity.this, text != null ? text : "", Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.GONE);
                        myHandler.sendMessage(myHandler.obtainMessage(MY_MSG_1, text != null ? text : ""));
                    }
                });
            }
        });
    }

    public void updateViews(String imgName, String text) {
        Picasso.get()
                .load(String.format("http://128.1.6.50:5000/images/%s", imgName))
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(imageView);
        textView.setText(text);
        imageView.setVisibility(View.VISIBLE);
        textView.setVisibility(View.VISIBLE);
    }

    public static boolean copyFile(String oldPath, String newPath) {
        boolean isok = true;
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(oldPath);
            if (oldfile.exists()) { //文件存在时
                InputStream inStream = new FileInputStream(oldPath); //读入原文件
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1024];
                int length;
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread; //字节数 文件大小
                    //System.out.println(bytesum);
                    fs.write(buffer, 0, byteread);
                }
                fs.flush();
                fs.close();
                inStream.close();
            } else {
                isok = false;
            }
        } catch (Exception e) {
            // System.out.println("复制单个文件操作出错");
            // e.printStackTrace();
            isok = false;
        }

        return isok;
    }

    class HttpLogger implements HttpLoggingInterceptor.Logger {
        @Override
        public void log(String message) {
            Log.d("HttpLogInfo", message);
        }
    }

    private MyHandler myHandler;
    private static final int MY_MSG_1 = 123;

    private static class MyHandler extends Handler {
        private Context context;

        public MyHandler(Context context) {
            this.context = context;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MY_MSG_1:
                    if (context instanceof BankCardActivity) {
                        ((BankCardActivity) context).parseMyMessage((BankCardActivity) context, (String) msg.obj);
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    }

    private void parseMyMessage(BankCardActivity activity, String text) {
        try {
            if (text == null) {
                return;
            }

            final String imgStart = "<img src=/images/";
            final String imgEnd = ">";
            int indexStart = text.indexOf(imgStart);
            int indexEnd = text.indexOf(imgEnd);
            if (indexStart == -1
                    || indexEnd == -1
                    || indexStart >= indexEnd
                    || indexStart + imgStart.length() >= indexEnd) {
                return;
            }
            String imgName = text.substring(indexStart + imgStart.length(), indexEnd);

            final String divStart = "<div>";
            final String divEnd = "</div>";
            indexStart = text.indexOf(divStart);
            indexEnd = text.indexOf(divEnd);
            if (indexStart == -1
                    || indexEnd == -1
                    || indexStart >= indexEnd
                    || indexStart + divStart.length() >= indexEnd) {
                return;
            }
            String divText = text.substring(indexStart + divStart.length(), indexEnd);
            activity.updateViews(imgName, divText);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
