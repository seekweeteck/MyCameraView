package my.edu.tarc.mycamview;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MyCamPreview";
    private Camera mCamera;
    private CameraPreview mPreview;
    ImageButton buttonCapture, buttonNew;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonCapture = (ImageButton)findViewById(R.id.button_capture);
        buttonNew = (ImageButton)findViewById(R.id.button_new);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(checkCameraHardware(this)==false){
            Toast.makeText(this, "No camera hardware found", Toast.LENGTH_LONG).show();
        }else{
            //Create an instance of Camera
            mCamera = getCameraInstance();

            // get Camera parematers
            Camera.Parameters params = mCamera.getParameters();
            List<Camera.Size> sizes = params.getSupportedPictureSizes();

            //------------------------------------- New
            Camera.Size pictureS = MyCamPara.getInstance().getPictureSize(sizes, 800);
            if(pictureS != null){
                params.setPictureSize(pictureS.width, pictureS.height);
            }else{
                pictureS = sizes.get(0);

                for (int i = 0; i < sizes.size(); i++) {
                    if (sizes.get(i).width < pictureS.width)
                        pictureS = sizes.get(i);
                }
            }

            //Set the focus mode
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            params.setPictureFormat(ImageFormat.JPEG);

            //params.setPictureSize(size.width, size.height);

            mCamera.setParameters(params);
            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(this, mCamera);
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(mPreview);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCameraAndPreview();
    }

    //    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            Log.e("MyCamPreview", "failed to open Camera");
        }
        return c; // returns null if camera is unavailable
    }

    private boolean safeCameraOpen(int id) {
        boolean qOpened = false;

        try {
            releaseCameraAndPreview();
            mCamera = Camera.open(id);
            qOpened = (mCamera != null);
        } catch (Exception e) {
            Log.e(getString(R.string.app_name), "failed to open Camera");
            e.printStackTrace();
        }

        return qOpened;
    }

    private void releaseCameraAndPreview() {
        mPreview.setCamera(null);
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    public void takePicture(View v){
        Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
            public void onShutter() {
                AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                mgr.playSoundEffect(AudioManager.FLAG_PLAY_SOUND);
            }
        };

        Camera.PictureCallback mPicture = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                File pictureFile = getOutputMediaFile();
                if (pictureFile == null) {
                    Log.d(TAG, "Error creating media file, check storage permissions: ");
                    return;
                }
                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                } catch (FileNotFoundException e) {
                    Toast.makeText(getApplicationContext(), "Error:" + e.getMessage(), Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "Error:" + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        };

        mCamera.takePicture(shutterCallback, null, mPicture);
        buttonCapture.setEnabled(false);
        buttonNew.setEnabled(true);
        //mCamera.stopPreview();
    }

    public void newPreview(View v){
        mCamera.startPreview();
        buttonCapture.setEnabled(true);
        buttonNew.setEnabled(false);
    }

    private static File getOutputMediaFile() {
        //Create a directory to store photos: MyCameraApp
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory
                (Environment.DIRECTORY_PICTURES),"MyCamera");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("My Camera App", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").
                format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + ".jpg");
        return mediaFile;
    }


}
