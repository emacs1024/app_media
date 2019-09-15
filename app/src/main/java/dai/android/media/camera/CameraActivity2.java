package dai.android.media.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import dai.android.media.R;

public class CameraActivity2 extends AppCompatActivity {
    private final static String TAG = "CameraActivity2";

    private final static int CAMERA_OK = 1001;
    private final static String[] PERMISSIONS = {
            "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };


    private SurfaceView mCameraSurfaceView;
    private ImageReader mImageReader;


    private Handler mWH;
    private Handler mMH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);

        HandlerThread thread = new HandlerThread("CameraThread");
        thread.start();
        mWH = new Handler(thread.getLooper());
        mMH = new Handler(Looper.getMainLooper());

        if (!checkPermissionAllGranted(PERMISSIONS)) {
            ActivityCompat.requestPermissions(CameraActivity2.this, PERMISSIONS, CAMERA_OK);
        } else {
            init();
        }
    }


    private void init() {
        mCameraSurfaceView = findViewById(R.id.cameraSurfaceView);
        SurfaceHolder sh = mCameraSurfaceView.getHolder();
        if (null != sh) {
            sh.addCallback(mShCallback);
        }
    }

    private void showWaringDialog() {
        androidx.appcompat.app.AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("警告！")
                .setMessage("请前往设置->应用->PermissionDemo->权限中打开相关权限，否则功能无法正常运行！")
                .setPositiveButton("确定", (dialog1, which) -> {
                    // 一般情况下如果用户不授权的话，功能是无法运行的，做退出处理
                    finish();
                }).show();
    }

    private boolean checkPermissionAllGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_OK: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //init();
                } else {
                    showWaringDialog();
                }
            }
        }
    }

    private void openCamera() {
        if (!checkPermissionAllGranted(PERMISSIONS)) {
            ActivityCompat.requestPermissions(CameraActivity2.this, PERMISSIONS, CAMERA_OK);
            return;
        }

        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (null == cameraManager) {
            Log.e(TAG, "can not get the 'CameraManager'");
            return;
        }

        String[] cameraList = null;
        try {
            cameraList = cameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.e(TAG, "can not get camera id list", e);
            return;
        }

        if (null == cameraList || cameraList.length <= 0) {
            Log.e(TAG, "no camera info");
            return;
        }

        for (String id : cameraList) {
            CameraCharacteristics cameraCharacteristics = null;
            try {
                cameraCharacteristics = cameraManager.getCameraCharacteristics(id);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                continue;
            }

            Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
            if (null == facing) {
                continue;
            }
            if (facing != CameraCharacteristics.LENS_FACING_BACK) {
                continue;
            }

            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                cameraManager.openCamera(id, mStateCallback, mWH);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }


    private final SurfaceHolder.Callback mShCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated");
            openCamera();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };


    // inner class of CameraDevice.StateCallback
    protected final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };
}
