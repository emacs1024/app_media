package dai.android.media;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import dai.android.media.camera.CameraActivity;
import dai.android.media.camera.CameraActivity2;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnGotoCamera2Activity).setOnClickListener(this);
        findViewById(R.id.btnGotoCameraActivity2).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btnGotoCamera2Activity: {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
                break;
            }

            case R.id.btnGotoCameraActivity2: {
                Intent intent = new Intent(MainActivity.this, CameraActivity2.class);
                startActivity(intent);
                break;
            }
        }

    }
}
