package dai.android.app.media;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import dai.android.app.media.camera.CameraActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnGoToCameraActivity).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnGoToCameraActivity: {
                Intent intent = new Intent(getBaseContext(), CameraActivity.class);
                startActivity(intent);
                break;
            }
        }
    }
}
