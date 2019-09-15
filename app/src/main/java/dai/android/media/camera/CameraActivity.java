package dai.android.media.camera;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import dai.android.media.R;


public class CameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (null == savedInstanceState) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, Camera2Fragment.newInstance())
                    .commit();
        }
    }

}