package dai.android.app.media.camera;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import dai.android.app.media.PermissionManager;
import dai.android.app.media.R;

// document address:
// 1. https://blog.csdn.net/sjy0118/article/details/78748941
// 2. https://blog.csdn.net/hexingen/article/details/79290046
// 3. https://github.com/66668/googleDemo_Camera2
// 4. https://www.cnblogs.com/azraelly/archive/2013/01/01/2841269.html


public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        PermissionManager.requestDefaultPermissions(this);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        PermissionManager.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }
}
