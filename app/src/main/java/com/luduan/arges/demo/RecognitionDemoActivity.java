package com.luduan.arges.demo;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.luduan.android.widget.ArgesFaceCameraView;
import com.luduan.android.widget.CameraListener;
import com.luduan.android.widget.Face;
import com.luduan.android.widget.LensFacing;
import com.luduan.arges.client.ArgesClient;
import com.luduan.arges.client.ClientException;
import com.luduan.arges.client.RecognitionItem;
import com.luduan.arges.client.ServerException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecognitionDemoActivity extends AppCompatActivity implements CameraListener {
    private static final String TAG = RecognitionDemoActivity.class.getSimpleName();

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

    private ArgesClient argesClient;

    private String recognitionGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition_demo);

        DemoApp app = (DemoApp) getApplication();
        argesClient = app.getArgesClient();
        recognitionGroup = app.GroupID;

        cameraView = findViewById(R.id.recogCameraView);
        cameraView.setHighDefinition(false);
        cameraView.setCameraListener(this);
        //cameraView.setRequireOriginalPicture(true); // Only required for snapshot function.

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
        snapshotButton.setEnabled(false);
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
            checkStoragePermission();
        }
        cameraView.openCamera(LensFacing.Front);
    }

    private static final int WRITE_REQUEST_CODE = 0x111;

    @TargetApi(Build.VERSION_CODES.M)
    public void checkStoragePermission() {
        int permissionCheckRead = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheckRead != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            requestPermissions(permissions, WRITE_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
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
            recognitionItem = argesClient.recognize(recognitionGroup, faces[0].getImage(), true, true);
        } catch (ServerException e) {
            Log.w(TAG, "Can't recognize: " + e.getMessage());
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
        }
    }

    @Override
    @UiThread
    public void onCameraOpened() {
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
        if (scanning.get()) {
            snapshot.compareAndSet(false, true);
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
            (new ImageLoader(argesClient, recogPhoto)).execute(item.getPerson().getPhoto());
            recogName.setText(item.getPerson().getName());

            int lv = (int) (item.getLiveness() * 100);
            linvessIndicator.setProgress(lv);
            livenessValue.setText(Integer.toString(lv) + "%");

            int cf = (int) (item.getConfidence() * 100);
            confidenceIndicator.setProgress(cf);
            confidenceValue.setText(Integer.toString(cf) + "%");
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
