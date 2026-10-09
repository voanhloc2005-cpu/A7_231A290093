package vn.edu.vhu.ltdd.a7permissions;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "a7_channel";

    private ImageView imgPreview;
    private SharedPreferences prefs;
    private boolean captureAfterPermission = false;

    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgPreview = findViewById(R.id.imgPreview);
        Button btnCameraPermission = findViewById(R.id.btnCameraPermission);
        Button btnTakePhoto = findViewById(R.id.btnTakePhoto);
        Button btnNotificationPermission =
                findViewById(R.id.btnNotificationPermission);
        Button btnSendNotification = findViewById(R.id.btnSendNotification);

        prefs = getSharedPreferences("permission_prefs", MODE_PRIVATE);

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                (Bitmap bitmap) -> {
                    if (bitmap != null) {
                        imgPreview.setImageBitmap(bitmap);
                    } else {
                        Toast.makeText(this, "Chưa chụp được ảnh",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        Toast.makeText(this, "Đã cấp quyền camera",
                                Toast.LENGTH_SHORT).show();
                        if (captureAfterPermission) {
                            captureAfterPermission = false;
                            cameraLauncher.launch(null);
                        }
                    } else {
                        captureAfterPermission = false;
                        handleCameraDenied();
                    }
                });

        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        Toast.makeText(this, "Đã cấp quyền thông báo",
                                Toast.LENGTH_SHORT).show();
                        sendNotification();
                    } else {
                        handleNotificationDenied();
                    }
                });

        createNotificationChannel();

        btnCameraPermission.setOnClickListener(v -> requestCameraPermission());

        btnTakePhoto.setOnClickListener(v -> {
            if (hasCameraPermission()) {
                cameraLauncher.launch(null);
            } else {
                captureAfterPermission = true;
                requestCameraPermission();
            }
        });

        btnNotificationPermission.setOnClickListener(
                v -> requestNotificationPermission());

        btnSendNotification.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(this,
                            Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission();
            } else {
                sendNotification();
            }
        });
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        if (hasCameraPermission()) {
            Toast.makeText(this, "Camera đã được cấp quyền",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        boolean asked = prefs.getBoolean("asked_camera", false);

        if (asked &&
                !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showSettingsDialog("Camera");
            return;
        }

        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new AlertDialog.Builder(this)
                    .setTitle("Cần quyền camera")
                    .setMessage("Ứng dụng cần quyền camera để chụp ảnh xem trước.")
                    .setNegativeButton("Hủy", (d, w) -> {
                        captureAfterPermission = false;
                    })
                    .setPositiveButton("Tiếp tục", (d, w) ->
                            launchCameraPermission())
                    .show();
        } else {
            launchCameraPermission();
        }
    }

    private void launchCameraPermission() {
        prefs.edit().putBoolean("asked_camera", true).apply();
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void handleCameraDenied() {
        if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showSettingsDialog("Camera");
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Quyền camera bị từ chối")
                    .setMessage("Bạn có thể cấp quyền camera để sử dụng chức năng chụp ảnh.")
                    .setPositiveButton("Thử lại", (d, w) ->
                            requestCameraPermission())
                    .setNegativeButton("Đóng", null)
                    .show();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) {
            sendNotification();
            return;
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            sendNotification();
            return;
        }

        boolean asked = prefs.getBoolean("asked_notification", false);

        if (asked &&
                !shouldShowRequestPermissionRationale(
                        Manifest.permission.POST_NOTIFICATIONS)) {
            showSettingsDialog("Thông báo");
            return;
        }

        if (shouldShowRequestPermissionRationale(
                Manifest.permission.POST_NOTIFICATIONS)) {
            new AlertDialog.Builder(this)
                    .setTitle("Cần quyền thông báo")
                    .setMessage("Cho phép ứng dụng gửi thông báo mẫu.")
                    .setNegativeButton("Hủy", null)
                    .setPositiveButton("Tiếp tục", (d, w) ->
                            launchNotificationPermission())
                    .show();
        } else {
            launchNotificationPermission();
        }
    }

    private void launchNotificationPermission() {
        prefs.edit().putBoolean("asked_notification", true).apply();
        notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS);
    }

    private void handleNotificationDenied() {
        if (Build.VERSION.SDK_INT >= 33 &&
                !shouldShowRequestPermissionRationale(
                        Manifest.permission.POST_NOTIFICATIONS)) {
            showSettingsDialog("Thông báo");
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Chưa được cấp quyền")
                    .setMessage("Bạn có thể thử cấp quyền thông báo lại.")
                    .setPositiveButton("Thử lại", (d, w) ->
                            requestNotificationPermission())
                    .setNegativeButton("Đóng", null)
                    .show();
        }
    }

    private void showSettingsDialog(String permissionName) {
        new AlertDialog.Builder(this)
                .setTitle("Cần bật quyền trong Cài đặt")
                .setMessage("Quyền " + permissionName
                        + " đã bị từ chối. Hãy mở Cài đặt ứng dụng để bật lại.")
                .setNegativeButton("Để sau", null)
                .setPositiveButton("Mở Cài đặt", (d, w) -> {
                    Intent intent = new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                })
                .show();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Thông báo Lab A7",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Kênh thông báo mẫu của Lab A7");

            NotificationManager manager =
                    getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void sendNotification() {
        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bạn chưa cấp quyền thông báo",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Lab A7")
                        .setContentText("Thông báo mẫu đã được gửi!")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);

        NotificationManagerCompat.from(this).notify(1001, builder.build());
    }
}