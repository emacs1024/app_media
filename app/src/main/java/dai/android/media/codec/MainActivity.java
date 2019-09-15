package dai.android.media.codec;

import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;

import dai.android.media.R;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private static final String TAG = "MainActivity";

    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSampleSurfaceView;
    private SurfaceView mPlaySurfaceView;

    private Camera mCamera;
    private Camera.Parameters parameters;
    private AvcEncoder mAvcEncoder;

    private VideoDecoder mVideoDecoder;

    private int WIDTH = 640;
    private int HEIGHT = 480;
    private int FRAME_RATE = 24;
    private int BIT_RATE = 8500 * 1000;

    private final static int CAMERA_OK = 1001;
    private final static String[] PERMISSIONS_STORAGE = {
            "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };

    private String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/myTestVideo_h264.mp4";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSampleSurfaceView = findViewById(R.id.sampleSurfaceView);
        mPlaySurfaceView = findViewById(R.id.playSurfaceView);

        findViewById(R.id.btnPlay).setOnClickListener(mViewClickListener);
        findViewById(R.id.btnSample).setOnClickListener(mViewClickListener);
        findViewById(R.id.btnStop).setOnClickListener(mViewClickListener);

        if (!supportAvcCodec()) {
            Log.e(TAG, "this device not support encoder for avc");
        }

        if (!checkPermissionAllGranted(PERMISSIONS_STORAGE)) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE, CAMERA_OK);
        } else {
            init();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_OK: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    init();
                } else {
                    showWaringDialog();
                }
            }
        }
    }

    private boolean supportAvcCodec() {
        for (int j = MediaCodecList.getCodecCount() - 1; j >= 0; j--) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(j);

            String[] types = codecInfo.getSupportedTypes();
            for (int i = 0; i < types.length; i++) {
                if (types[i].equalsIgnoreCase("video/avc")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void init() {
        mSurfaceHolder = mSampleSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
    }

    private void showWaringDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
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

    private void startCamera(Camera camera) {
        if (null == camera) {
            Log.e(TAG, "null 'Camera' object");
            return;
        }

        try {
            camera.setPreviewCallback(this);
            camera.setDisplayOrientation(90);
            if (parameters == null) {
                parameters = mCamera.getParameters();
            }
            parameters = camera.getParameters();
            parameters.setPreviewFormat(ImageFormat.NV21);
            parameters.setPreviewSize(WIDTH, HEIGHT);
            camera.setParameters(parameters);
            camera.setPreviewDisplay(mSurfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, "option camera failed.", e);
        }
    }

    private Camera getBackCamera() {
        try {
            Camera camera = null;
            camera = Camera.open(0);
            return camera;
        } catch (Exception e) {
            Log.e(TAG, "open camera failed", e);
        }
        return null;
    }

    private void startPlayVideo() {
        Log.d(TAG, "startPlayVideo");
        if (null == mVideoDecoder) {
            mVideoDecoder = new VideoDecoder();
        }

        //String uri = "https://youku.cdn-tudou.com/20180508/5787_8a713671/1000k/hls/index.m3u8";
        //String uri = "http://192.240.127.34:1935/live/cs14.stream/playlist.m3u8";
        String uri = path;

        Log.d(TAG, "Play Address: " + uri);

        mVideoDecoder.setDataSource(mPlaySurfaceView.getHolder().getSurface(), uri);
        mVideoDecoder.start();
    }

    private void stopAll() {
        if (null != mVideoDecoder) {
            mVideoDecoder.close();
        }

        if (mAvcEncoder != null) {
            mAvcEncoder.stopEncoderThread();
        }
    }

    private View.OnClickListener mViewClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnPlay: {
                    startPlayVideo();
                    break;
                }

                case R.id.btnSample: {
                    startCamera(mCamera);
                    break;
                }

                case R.id.btnStop: {
                    stopAll();
                    break;
                }
            }


        }
    };


    // implement of Camera.PreviewCallback
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (null != mAvcEncoder) {
            mAvcEncoder.put_YUV_data(data);
        }
    }


    //
    // { implement of SurfaceHolder.Callback
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = getBackCamera();
        //startCamera(mCamera);

        mAvcEncoder = new AvcEncoder(WIDTH, HEIGHT, FRAME_RATE, path);
        mAvcEncoder.startEncoderThread();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (null != mCamera) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        if (null != mAvcEncoder) {
            mAvcEncoder.stopEncoderThread();
        }
    }
    // } implement of SurfaceHolder.Callback
    //


}
