package com.luduan.arges.demo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.luduan.arges.client.RecognitionItem;
import com.luduan.arges.snack.ArgesRecognitionActivity;
import com.luduan.arges.snack.OnRecognizedListener;

public class MyRecognitionActivity extends ArgesRecognitionActivity implements OnRecognizedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DemoApp app = (DemoApp) getApplication();
        setRecognitionServer(app.getArgesClient()); // 设置已经创建的ArgesClient实例

        addRecognitionGroup(app.GroupID); //设置识别的目标分组

        setLivenessCheck(true); // 启用活体识别功能

        setOnRecognizedListener(this); //设置Listener
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.getAction() == KeyEvent.ACTION_DOWN)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onRecognized(RecognitionItem recognitionItem) {
        if (recognitionItem == null || recognitionItem.getConfidence() < 0.6) { // 缺省可信度阈值是0.5, 限制0.6或更高可以更校验严格
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("我不认识你").setTitle("提示").setPositiveButton("Close", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            }).create().show();
            return true;
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            if (recognitionItem.getLiveness() < 0.6) {
                builder.setTitle("提示").setMessage("姓名：" + recognitionItem.getPerson().getName() +
                        ", 但疑似照片照片或视频欺骗，请重试!");
            } else {
                builder.setMessage("您好:" + recognitionItem.getPerson().getName()).setTitle("欢迎");
            }
            Log.i("TEST", "Liveness:" + recognitionItem.getLiveness() + " Confidence: " + recognitionItem.getConfidence());
            builder.create().show();
            return true;
        }
    }
}
