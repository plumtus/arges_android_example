package com.luduan.arges.demo;

import android.app.Application;

import com.luduan.arges.client.ArgesAndroidClient;
import com.luduan.arges.client.ArgesClient;
import com.luduan.arges.client.ClientException;

public class DemoApp extends Application {
    public static final String ServerEndpoint = "https://www.luduan.com.cn/developer"; // 开发平台地址
    //public static final String ServerEndpoint = "http://192.168.2.106:8080/server"; // 修改为本地服务器地址
    public static final String AppID = "arges"; // 请修改为实际值，见Web管理端‘应用设置’页面
    public static final String AppKey = "5863ff3c6a96cd5f"; // 请修改为实际值，见Web管理端‘应用设置’页面
    public static final String GroupID = "36dd28e9b3j1"; // 请修改为实际值，见Web管理端‘数据管理’页面

    private ArgesClient argesClient = null;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            argesClient = new ArgesAndroidClient(ServerEndpoint, AppID, AppKey);
        } catch (ClientException e) {
            throw new RuntimeException("ArgesClient can't login to server");
        }
    }

    public ArgesClient getArgesClient() {
        if (argesClient == null) {
            throw new RuntimeException("ArgesClient has not been initialized");
        }
        return argesClient;
    }
}
