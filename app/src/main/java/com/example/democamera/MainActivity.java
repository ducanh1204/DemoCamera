package com.example.democamera;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.extensions.HdrImageCaptureExtender;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Button btnShowCamera;
    private CustomButton btnTakePhoto;
    private ImageView img, btnClose;
    private RelativeLayout layoutCamera;
    private PreviewView previewView;
    private ImageCapture imageCapture;
    public Executor executor = Executors.newSingleThreadExecutor();
    public final int MY_PERMISSIONS_CODE_1 = 101;
    private View viewCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        img = findViewById(R.id.img);
        btnClose = findViewById(R.id.btn_close);
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        btnShowCamera = findViewById(R.id.btn_show_camera);
        layoutCamera = findViewById(R.id.layout_camera);
        previewView = findViewById(R.id.preview_view);
        viewCamera = findViewById(R.id.view_camera);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (layoutCamera.getVisibility() == View.VISIBLE) {
                    layoutCamera.setVisibility(View.GONE);
                }
            }
        });

        btnShowCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFullPermission() == false)
                    requestPermissions();
                else
                    startCamera();
            }
        });

        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });
    }

    public void startCamera() {
        layoutCamera.setVisibility(View.VISIBLE);
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    cameraProvider.unbindAll();
                    bindPreview(cameraProvider);

                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        Display display = previewView.getDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        Size size = new Size(metrics.widthPixels,metrics.heightPixels);

        Preview preview = new Preview.Builder()
        .setTargetRotation(display.getRotation())
                .setTargetResolution(size)
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .build();

        ImageCapture.Builder builder = new ImageCapture.Builder();
        builder.setTargetRotation(display.getRotation())
                .setTargetResolution(size)
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY);

        HdrImageCaptureExtender hdrImageCaptureExtender = HdrImageCaptureExtender.create(builder);
        if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
            hdrImageCaptureExtender.enableExtension(cameraSelector);
        }
        imageCapture = builder.build();
        preview.setSurfaceProvider(previewView.createSurfaceProvider());
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture);

    }


    private String getBatchDirectoryName() {
        String app_folder_path = "";
        app_folder_path = Environment.getExternalStorageDirectory().toString() + "/Pictures";
        return app_folder_path;
    }

    private int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    public void takePhoto() {
        layoutCamera.setVisibility(View.GONE);
        File file = new File(getBatchDirectoryName(), "image" + ".jpg");
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
        if (imageCapture == null) {
            return;
        }
        imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                if (file.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    ExifInterface exif = null;
                    try {
                        exif = new ExifInterface(file.getAbsoluteFile().toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    int rotationInDegrees = exifToDegrees(rotation);
                    Matrix matrix = new Matrix();
                    matrix.preRotate(rotationInDegrees);
                    Bitmap myBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    final Bitmap finalMyBitmap = cropImage(myBitmap, previewView, viewCamera);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            img.setImageBitmap(finalMyBitmap);
                        }
                    });
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException error) {
                error.printStackTrace();
            }
        });
    }

    private boolean isFullPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else
            return false;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                MY_PERMISSIONS_CODE_1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_CODE_1:
                int countGranted = 0;
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        countGranted++;
                    }
                }
                if (countGranted == grantResults.length) {
                    startCamera();
                } else {
                    Toast.makeText(MainActivity.this, "Yêu cầu cấp quyền cho ứng dụng", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private Bitmap cropImage(Bitmap bitmap, PreviewView previewView, View viewCamera) {
        int heightOriginal = previewView.getHeight();
        int widthOriginal = previewView.getWidth();

        int heightFrame = viewCamera.getHeight();
        int widthFrame = viewCamera.getWidth();
        int leftFrame = viewCamera.getLeft();
        int topFrame = viewCamera.getTop();

        int heightReal = bitmap.getHeight();
        int widthReal = bitmap.getWidth();

        int widthFinal = widthFrame * widthReal / widthOriginal;
        int heightFinal = heightFrame * heightReal / heightOriginal;
        int leftFinal = leftFrame * widthReal / widthOriginal;
        int topFinal = topFrame * heightReal / heightOriginal;
        Bitmap bitmapFinal = Bitmap.createBitmap(
                bitmap,
                leftFinal, topFinal, widthFinal, heightFinal
        );
        return bitmapFinal;
    }
}