package com.luduan.arges.demo;

import android.app.Application;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

public class DemoApp extends Application {
    public static String ServerEndpoint = "https://www.luduan.com.cn/developer"; // 开发平台地址
    //public static final String ServerEndpoint = "http://192.168.11.111:8080/server"; // 修改为本地服务器地址
    public static String AppID = "12345678abcd"; // 请修改为实际值，见Web管理端‘应用设置’页面
    public static String AppKey = "12345678abcdef00"; // 请修改为实际值，见Web管理端‘应用设置’页面
    public static String GroupID = "12345678abcd"; // 请修改为实际值，见Web管理端‘数据管理’页面

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            if (Arrays.asList(getResources().getAssets().list("")).contains("arges-client-config.properties")) {
                Properties properties = new Properties();
                properties.load(getResources().getAssets().open("arges-client-config.properties"));
                ServerEndpoint = properties.getProperty("endpoint");
                AppID = properties.getProperty("app-id");
                AppKey = properties.getProperty("app-key");
                GroupID = properties.getProperty("default-group");
            }
        } catch (IOException e) {
            Log.d("TEST", "Can not load ARGES configuration file", e);
        }
    }
}