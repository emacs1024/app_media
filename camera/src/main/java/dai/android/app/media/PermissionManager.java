package dai.android.app.media;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

public final class PermissionManager {

    private final static int PERMISSIONS_OK = 1001;
    private final static String[] PERMISSIONS = {
            "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };


    public static boolean checkSelfPermission(Context context, String permission) {
        return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean checkSelfPermission(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (!checkSelfPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }


    public static boolean checkDefaultPermissions(Context context) {
        return checkSelfPermission(context, PERMISSIONS);
    }

    public static void requestDefaultPermissions(Activity activity) {
        requestPermissions(activity, PERMISSIONS);
    }

    public static void requestPermissions(Activity activity, String[] permissions) {
        if (!checkSelfPermission(activity, permissions)) {
            ActivityCompat.requestPermissions(activity, permissions, PERMISSIONS_OK);
        }
    }

    public static void onRequestPermissionsResult(Activity activity,
                                                  int requestCode,
                                                  @NonNull String[] permissions,
                                                  @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_OK) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showWaringDialog(activity);
            }
        }
    }


    private static void showWaringDialog(Context context) {
        androidx.appcompat.app.AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("警示")
                .setMessage("设置->应用和通知->'你的应用'->权限 检查相关权限")
                .setPositiveButton("确定", null).show();
    }


}
