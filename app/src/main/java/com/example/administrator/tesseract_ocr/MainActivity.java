package com.example.administrator.tesseract_ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView tvResult;
    private ImageView ivPic;
    private Button btnEng;
    private Button btnClear;
    private Button btnChisim;

    private TessBaseAPI mTess;
    private Handler handler;
    private String path;
    private boolean tag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
        setListener();
    }

    private void initView() {
        tvResult = (TextView) findViewById(R.id.tv_result);
        ivPic = (ImageView) findViewById(R.id.iv);
        btnEng = (Button) findViewById(R.id.btn_eng);
        btnClear = (Button) findViewById(R.id.btn_clear);
        btnChisim = (Button) findViewById(R.id.btn_chi_sim);
    }

    private void initData() {
        handler = new InnHandler();
        path = getDiskCacheDir(this);
        mTess = new TessBaseAPI();

        //存放tessdata的路径，注意放到一个目录的下面，即dir1/dir2/xxxx
        File dir = new File(path, "tessdata");
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void setListener() {
        btnEng.setOnClickListener(this);
        btnChisim.setOnClickListener(this);
        btnClear.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_eng:
                initTessBaseData("eng", R.mipmap.eng);
                break;
            case R.id.btn_chi_sim:
                initTessBaseData("chi_sim", R.mipmap.chi_sim);
                break;
            case R.id.btn_clear:
                tvResult.setText("");
                break;
            default:
                break;
        }
    }

    /**
     * @param language 样本匹配库
     */
    private void initTessBaseData(final String language, final int imgId) {
        tag = false;
        tvResult.setText("识别中。。。");
        ivPic.setImageBitmap(BitmapFactory.decodeResource(getResources(), imgId));
        new Thread(new Runnable() {
            @Override
            public void run() {
                tag = mTess.init(path, language);
                if (tag) {
                    Message msg = new Message();
                    msg.what = 1;
                    msg.obj = imgId;
                    handler.sendMessage(msg);
                }
            }
        }).start();
    }

    /**
     * 对图片的识别操作(耗时操作，放到线程里)
     *
     * @param imgId 图片资源ID
     */
    private void method(int imgId) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), imgId);
        ivPic.setImageBitmap(bitmap);
        if (tag) {
            mTess.setImage(bitmap);
            //开启一个线程
            new InnThread().start();
        }
    }

    /**
     * 获取缓存的目录
     *
     * @param context 上下文对象
     * @return cache目录的路径
     */
    public static String getDiskCacheDir(Context context) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            //SD卡存在，或者sd卡不可移动
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return cachePath;
    }


    private class InnThread extends Thread {
        @Override
        public void run() {
            super.run();
            synchronized (this) {
                String result = mTess.getUTF8Text();
                Message msg = new Message();
                msg.what = 2;
                msg.obj = result;
                handler.sendMessage(msg);
            }
        }
    }


    /**
     * 在UI线程中刷新结果
     */
    private class InnHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    if (tag) {
                        int imgId = (int) msg.obj;
                        method(imgId);
                    }
                    break;
                case 2:
                    String result = (String) msg.obj;
                    tvResult.setText(String.valueOf("结果:" + result));
                    break;
                default:
                    break;
            }

        }
    }


}
