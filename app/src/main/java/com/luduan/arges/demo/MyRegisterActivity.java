package com.luduan.arges.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import com.luduan.arges.client.RecognitionItem;
import com.luduan.arges.snack.OnRecognizedListener;

public class MyRegisterActivity extends com.luduan.arges.snack.ArgesRegisterActivity implements OnRecognizedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DemoApp app = (DemoApp) getApplication();
        setRecognitionServer(app.getArgesClient()); // 设置已经创建的ArgesClient实例

        setRecognitionGroup(app.GroupID); // 人脸入库所归属的分组

        setAcquisitionNumber(5); // 设置一次采集流程中采集的人脸照片数量

        //允许切换前后摄像头
        setCameraFacingSwitchable(true);

        setOnRecognizedListener(this);
    }

    // 人脸采集进度完成后，会检查该人脸是否已经在库中，如果是则调用该回调方法，开发者可以在该方法中控制处理行为
    @Override
    public boolean onRecognized(RecognitionItem recognitionItem) {
        setPersonID(recognitionItem.getPerson().getId(), false); // 设置本次采集结果覆盖原数据
        setPersonCode(recognitionItem.getPerson().getCode(), false); //程序填入用户代码，且用户界面不可更改
        setPersonName(recognitionItem.getPerson().getName(), true); //程序填入用户姓名，且用户界面可更改
        return true;
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
}
