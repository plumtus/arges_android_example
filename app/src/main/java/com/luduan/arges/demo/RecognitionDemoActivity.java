package com.luduan.arges.demo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;

import com.luduan.arges.client.ArgesClient;
import com.luduan.arges.client.ClientException;
import com.luduan.arges.client.Group;
import com.luduan.arges.client.RecognitionItem;
import com.luduan.arges.client.ServerException;
import com.luduan.arges.widget.ArgesFaceCameraView;
import com.luduan.arges.widget.CameraListener;
import com.luduan.arges.widget.Face;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecognitionDemoActivity extends AppCompatActivity implements CameraListener {
    private static final String TAG = RecognitionDemoActivity.class.getSimpleName();

    private static boolean EconoMode = true;

    private ArgesFaceCameraView cameraView;

    private Button startButton;

    private Button snapshotButton;

    private ImageView recogPhoto;

    private TextView recogName;

    private ProgressBar linvessIndicator;
    private TextView livenessValue;

    private ProgressBar confidenceIndicator;
    private TextView confidenceValue;

    private AtomicBoolean scanning = new AtomicBoolean(false);

    private AtomicBoolean imageLoading = new AtomicBoolean(false);

    private AtomicBoolean snapshot = new AtomicBoolean(false);

    private String recognitionGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition_demo);

        recognitionGroup = DemoApp.GroupID;

        cameraView = findViewById(R.id.recogCameraView);
        cameraView.register(DemoApp.ServerEndpoint, DemoApp.AppID, DemoApp.AppKey);  // 设置服务器访问参数
        cameraView.setHighDefinition(false);
        cameraView.setCameraListener(this);

        startButton = findViewById(R.id.recogStartButtion);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (scanning.get()) {
                    scanning.compareAndSet(true, false);
                    startButton.setText("开始");
                    cameraView.stopCapture();
                    snapshotButton.setEnabled(false);
                } else {
                    scanning.compareAndSet(false, true);
                    startButton.setText("停止");
                    cameraView.startCapture();
                    snapshotButton.setEnabled(true);
                }
            }
        });

        snapshotButton = findViewById(R.id.snapshotButtion);
        snapshotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
            }
        });

        recogPhoto = findViewById(R.id.recogPhoto);
        recogPhoto.setImageResource(android.R.color.transparent);

        recogName = findViewById(R.id.recogName);

        linvessIndicator = findViewById(R.id.livenessBar);
        linvessIndicator.setIndeterminate(false);
        linvessIndicator.setMax(100);

        livenessValue = findViewById(R.id.livenessValue);

        confidenceIndicator = findViewById(R.id.confidenceBar);
        confidenceIndicator.setIndeterminate(false);
        confidenceIndicator.setMax(100);

        confidenceValue = findViewById(R.id.confidenceValue);

        setResult(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> perms = new ArrayList<String>();
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.INTERNET);
            }
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.CAMERA);
            }
            if (perms.isEmpty()) { // 已经拥有全部权限,则直接打开摄像头
                cameraView.openAntiSpoofingCamera(EconoMode);
            } else {
                requestPermissions(perms.toArray(new String[0]), 0);
            }
        } else {
            cameraView.openAntiSpoofingCamera(EconoMode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.CAMERA) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    cameraView.openAntiSpoofingCamera(EconoMode);
                    break;
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.closeCamera();
    }

    private class ObjectWrapper {
        public RecognitionItem recognitionItem;
        public byte[] photo;

        public ObjectWrapper(RecognitionItem recognitionItem, byte[] photo) {
            this.recognitionItem = recognitionItem;
            this.photo = photo;
        }
    }

    @Override
    @WorkerThread
    public Object onCapture(@NonNull Face[] faces, @NonNull byte[] bytes) {
        if (faces.length == 0) {
            return new ObjectWrapper(null, bytes);
        }
        RecognitionItem recognitionItem = null;
        try {
            long bts = SystemClock.currentThreadTimeMillis();
            recognitionItem = cameraView.getArgesClient().recognize(recognitionGroup, faces[0].getImage(), true, true);
            long ets = SystemClock.currentThreadTimeMillis();
            Log.i(TAG, String.format("Recognition completed, elapsed %d milliseconds", ets - bts));
        } catch (ServerException e) {
            Log.w(TAG, "Can't recognize: " + e.getMessage());
            // Just for test
            if (e.getMessage().contains("Invalid group ID")) {
                try {
                    List<Group> groups = cameraView.getArgesClient().getGroups();
                    for (Group g : groups) {
                        Log.d(TAG, "Group[" + g.getId() + "] " + g.getName());
                    }
                    if (!groups.isEmpty()) {
                        recognitionGroup = groups.get(0).getId();
                    }
                } catch (ClientException e1) {
                    e1.printStackTrace();
                } catch (ServerException e1) {
                    e1.printStackTrace();
                }
            }
        } catch (ClientException e) {
            Log.e(TAG, "Failed to recognize", e);
        }
        return new ObjectWrapper(recognitionItem, bytes);
    }

    @Override
    @UiThread
    public void onPostCapture(Object o) {
        ObjectWrapper obj = (ObjectWrapper) o;

        if (snapshot.compareAndSet(true, false)) {
            if (obj.photo != null && obj.photo.length > 0) {
                (new SnapshotTask(this)).execute(obj.photo);
            }
        }

        if (obj.recognitionItem == null) {
            setResult(null);
            return;
        }
        if (obj.recognitionItem.getPerson() == null) {
            setResult(null);
            return;
        }
        setResult(obj.recognitionItem);
    }

    @Override
    @UiThread
    public void onFailedCapture(Reason reason) {
        if (reason == CameraListener.Reason.NO_FACE_FOUND) {
            setResult(null);
        } else if (reason == CameraListener.Reason.SPOOFING) {
            recogPhoto.setImageResource(R.drawable.anonymous);
        }
    }

    @Override
    @UiThread
    public void onCameraOpened() {
    }

    @Override
    public void onFillLightRequest(boolean b) {
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.getAction() == KeyEvent.ACTION_DOWN)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void takePhoto() {
        if (snapshot.compareAndSet(false, true)) {
            cameraView.snapshot();
        }
    }

    public void onPhotoSaved(String picFilename) {
        // TODD: Add your real action here
        Toast.makeText(this, picFilename, Toast.LENGTH_LONG).show();
    }

    private class ImageLoader extends AsyncTask<String, Void, Bitmap> {
        private final String TAG = getClass().getSimpleName();

        private ArgesClient argesClient;
        private ImageView bmImage;

        public ImageLoader(ArgesClient client, ImageView bmImage) {
            this.argesClient = client;
            this.bmImage = bmImage;
        }

        @Override
        public Bitmap doInBackground(String... urls) {
            if (!imageLoading.compareAndSet(false, true)) {
                return null;
            }
            try {
                String imgUrl = urls[0] + "?access_token=" + argesClient.getAccessToken();
                InputStream in = new java.net.URL(imgUrl).openStream();
                Bitmap image = BitmapFactory.decodeStream(in);
                return image;
            } catch (Exception e) {
                imageLoading.set(false);
                Log.e(TAG, "Download image error: " + e.getMessage(), e);
            } catch (ServerException e) {
                imageLoading.set(false);
                Log.e(TAG, "Draw image error: " + e.getMessage(), e);
            }
            return null;
        }

        @Override
        public void onPostExecute(Bitmap result) {
            if (result != null) {
                bmImage.setImageBitmap(result);
                imageLoading.set(false);
            }
        }
    }

    private void setResult(RecognitionItem item) {
        if (item == null) {
            recogPhoto.setImageResource(android.R.color.transparent);
            recogName.setText("");
            linvessIndicator.setProgress(0);
            livenessValue.setText("");
            confidenceIndicator.setProgress(0);
            confidenceValue.setText("");
        } else {
            (new ImageLoader(cameraView.getArgesClient(), recogPhoto)).execute(item.getPerson().getPhoto());
            recogName.setText(item.getPerson().getName());

            int lv = (int) (item.getLiveness() * 100);
            linvessIndicator.setProgress(lv);
            livenessValue.setText(lv + "%");

            int cf = (int) (item.getConfidence() * 100);
            confidenceIndicator.setProgress(cf);
            confidenceValue.setText(cf + "%");
        }
    }

    private class SnapshotTask extends AsyncTask<byte[], Void, String> {
        private RecognitionDemoActivity activity;

        public SnapshotTask(RecognitionDemoActivity activity) {
            this.activity = activity;
        }

        @Override
        protected String doInBackground(byte[]... bytes) {
            byte[] jpegData = bytes[0];
            try {
                return createImageFile(jpegData);
            } catch (IOException e) {
                Log.e(RecognitionDemoActivity.TAG, e.getLocalizedMessage());
                return null;
            }
        }

        @Override
        public void onPostExecute(String filePath) {
            if (filePath == null) {
                Toast.makeText(activity, "Cann't save picture to storage", Toast.LENGTH_LONG).show();
            } else {
                activity.onPhotoSaved(filePath);
            }
        }

        private String createImageFile(byte[] data) throws IOException {
            String imageFileName = "LUP_";
            File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(imageFile));
            bos.write(data);
            bos.close();
            return imageFile.getAbsolutePath();
        }
    }
}
