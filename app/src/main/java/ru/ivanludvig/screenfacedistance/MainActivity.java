package ru.ivanludvig.screenfacedistance;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    TextView  textView;
    Context context;
    float F = 1f;           //focal length
    float sensorX, sensorY; //camera sensor parameters
    float angleX, angleY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            Toast.makeText(this, "Grant Permission and restart app", Toast.LENGTH_SHORT).show();
        }
        else {
            Camera camera = frontCam();
            Camera.Parameters campar = camera.getParameters();
            F = campar.getFocalLength();
            angleX = campar.getHorizontalViewAngle();
            angleY = campar.getVerticalViewAngle();
            sensorX = (float) (Math.tan(Math.toRadians(angleX/2))*2*F);
            sensorY = (float) (Math.tan(Math.toRadians(angleY/2))*2*F);
            camera.stopPreview();
            camera.release();
            textView = findViewById(R.id.text);
            createCameraSource();
        }

    }


    private Camera frontCam() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            Log.v("CAMID", camIdx+"");
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    Log.e("FAIL", "Camera failed to open: " + e.getLocalizedMessage());
                }
            }
        }

        return cam;
    }


    public void createCameraSource() {
        FaceDetector detector = new FaceDetector.Builder(this)
                .setTrackingEnabled(true)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .build();
        detector.setProcessor(new LargestFaceFocusingProcessor(detector, new FaceTracker()));

        CameraSource cameraSource = new CameraSource.Builder(this, detector)
                .setRequestedPreviewSize(1024, 768)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build();

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
            cameraSource.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }


    }

    private class FaceTracker extends Tracker<Face> {


        private FaceTracker() {

        }

        @Override
        public void onUpdate(Detector.Detections<Face> detections, Face face) {
            float p =(float) Math.sqrt(
                    (Math.pow((face.getLandmarks().get(Landmark.LEFT_EYE).getPosition().x-
                            face.getLandmarks().get(Landmark.RIGHT_EYE).getPosition().x), 2)+
                            Math.pow((face.getLandmarks().get(Landmark.LEFT_EYE).getPosition().y-
                                    face.getLandmarks().get(Landmark.RIGHT_EYE).getPosition().y), 2)));

            float H = 63;
            float d = F*(H/sensorX)*(768/(2*p));

            showStatus("focal length: "+F+
                    "\nsensor width: "+sensorX
                    +"\ndistance: "+String.format("%.1f",d+"mm"));
        }

        @Override
        public void onMissing(Detector.Detections<Face> detections) {
            super.onMissing(detections);
            showStatus("face not detected");
        }

        @Override
        public void onDone() {
            super.onDone();
        }
    }


    public void showStatus(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(message);
            }
        });
    }

}
