package com.hvaughan3.mlobjectdetectplugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import androidx.annotation.NonNull;

import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;

import java.io.FileNotFoundException;
import java.util.Objects;

public class MlObjectDetect extends CordovaPlugin {
  private static final String TAG = "MlObjectDetect";

  private static final int NORMFILEURI = 0; // Make bitmap without compression using uri from picture library
                                            // (NORMFILEURI & NORMNATIVEURI have same functionality in android)
  private static final int NORMNATIVEURI = 1; // Make compressed bitmap using uri from picture library for faster ocr
                                              // but might reduce accuracy (NORMFILEURI & NORMNATIVEURI have same
                                              // functionality in android)
  private static final int FASTFILEURI = 2; // Make uncompressed bitmap using uri from picture library (FASTFILEURI &
                                            // FASTFILEURI have same functionality in android)
  private static final int FASTNATIVEURI = 3; // Make compressed bitmap using uri from picture library for faster ocr
                                              // but might reduce accuracy (FASTFILEURI & FASTFILEURI have same
                                              // functionality in android)
  private static final int BASE64 = 4; // send base64 image instead of uri

  private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

  static {
    ORIENTATIONS.append(Surface.ROTATION_0, 0);
    ORIENTATIONS.append(Surface.ROTATION_90, 90);
    ORIENTATIONS.append(Surface.ROTATION_180, 180);
    ORIENTATIONS.append(Surface.ROTATION_270, 270);
  }

  @Override
  public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext)
      throws JSONException {
    if (action.equals("detectObject")) {
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          try {
            int argstype = NORMFILEURI;
            String argimagestr = "";

            try {
              Log.d("argsbeech", args.toString());

              argstype = args.getInt(0);
              argimagestr = args.getString(1);
            } catch (Exception e) {
              callbackContext.error("Argument error");
              PluginResult r = new PluginResult(PluginResult.Status.ERROR);
              callbackContext.sendPluginResult(r);
              return;
            }

            Bitmap bitmap = null;
            Uri uri = null;

            if (argstype == NORMFILEURI || argstype == NORMNATIVEURI || argstype == FASTFILEURI
                || argstype == FASTNATIVEURI) {
              try {
                if (!argimagestr.trim().equals("")) {
                  String imagestr = argimagestr;

                  // code block that allows this plugin to directly work with document scanner
                  // plugin and camera plugin
                  if (imagestr.substring(0, 6).equals("file://")) {
                    imagestr = argimagestr.replaceFirst("file://", "");
                  }
                  //

                  uri = Uri.parse(imagestr);

                  if ((argstype == NORMFILEURI || argstype == NORMNATIVEURI) && uri != null) { // normal ocr
                    bitmap = MediaStore.Images.Media
                        .getBitmap(cordova.getActivity().getBaseContext().getContentResolver(), uri);
                  } else if ((argstype == FASTFILEURI || argstype == FASTNATIVEURI) && uri != null) {// fast ocr (might
                                                                                                     // be less
                                                                                                     // accurate)
                    bitmap = decodeBitmapUri(cordova.getActivity().getBaseContext(), uri);
                  }
                } else {
                  callbackContext.error("Object Detection Image Uri or Base64 string is empty");
                  PluginResult r = new PluginResult(PluginResult.Status.ERROR);
                  callbackContext.sendPluginResult(r);
                }
              } catch (Exception e) {
                e.printStackTrace();
                callbackContext.error("Object Detection Exception");
                PluginResult r = new PluginResult(PluginResult.Status.ERROR);
                callbackContext.sendPluginResult(r);
              }
            } else if (argstype == BASE64) {
              if (!argimagestr.trim().equals("")) {
                byte[] decodedString = Base64.decode(argimagestr, Base64.DEFAULT);
                bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
              } else {
                callbackContext.error("Object Detection Image Uri or Base64 string is empty");
                PluginResult r = new PluginResult(PluginResult.Status.ERROR);
                callbackContext.sendPluginResult(r);
              }
            } else {
              callbackContext.error("Object Detection Non existent argument. Use 0, 1, 2 , 3 or 4");
              PluginResult r = new PluginResult(PluginResult.Status.ERROR);
              callbackContext.sendPluginResult(r);
              return;
            }

            if (bitmap == null) {
              callbackContext.error("Object Detection Error in uri or base64 data!");
              PluginResult r = new PluginResult(PluginResult.Status.ERROR);
              callbackContext.sendPluginResult(r);
              return;
            }

            int rotationCompensation = getRotationCompensation(false);

            // Multiple object detection in static images
            ObjectDetectorOptions options = new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .enableClassification() // Optional
                .build();

            ObjectDetector objectDetector = ObjectDetection.getClient(options);

            InputImage image = InputImage.fromBitmap(bitmap, rotationCompensation);
            objectDetector.process(image).addOnSuccessListener(result -> {
              try {
                Log.d(TAG, "Object Detection Results: " + result);
                callbackContext.success(String.valueOf(result));
                PluginResult r = new PluginResult(PluginResult.Status.OK);
                callbackContext.sendPluginResult(r);
              } catch (Exception e) {
                callbackContext.error(String.valueOf(e));
                PluginResult r = new PluginResult(PluginResult.Status.ERROR);
                callbackContext.sendPluginResult(r);
              }
            }).addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
                callbackContext.error("Error with Object Detection Module");
                PluginResult r = new PluginResult(PluginResult.Status.ERROR);
                callbackContext.sendPluginResult(r);
              }
            });
          } catch (Exception e) {
            callbackContext.error("Object Detection Main loop Exception");
            PluginResult r = new PluginResult(PluginResult.Status.ERROR);
            callbackContext.sendPluginResult(r);
          }
        }
      });

      return true;
    }

    return false;
  }

  private Bitmap decodeBitmapUri(Context ctx, Uri uri) throws FileNotFoundException {
    int targetW = 600;
    int targetH = 600;
    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
    bmOptions.inJustDecodeBounds = true;
    BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bmOptions);
    int photoW = bmOptions.outWidth;
    int photoH = bmOptions.outHeight;

    int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
    bmOptions.inJustDecodeBounds = false;
    bmOptions.inSampleSize = scaleFactor;

    return BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bmOptions);
  }

  private int getRotationCompensation(Boolean isFrontFacing) {
      int rotationCompensation = 0;
      try {
          Activity activity = this.cordova.getActivity();

          if (activity == null || activity.isFinishing()) {
            return rotationCompensation;
          }

          // Get the device's current rotation relative to its "native" orientation.
          // Then, from the ORIENTATIONS table, look up the angle the image must be
          // rotated to compensate for the device's rotation.
          int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();

          // Get the device's sensor orientation.
          CameraManager cManager = (CameraManager) activity.getApplicationContext().getSystemService(Context.CAMERA_SERVICE);

          rotationCompensation = ORIENTATIONS.get(deviceRotation);
          Log.d(TAG, "tryAcquire");

          String cameraId = getCameraId(cManager, isFrontFacing);

          int sensorOrientation = cManager
                  .getCameraCharacteristics(cameraId)
                  .get(CameraCharacteristics.SENSOR_ORIENTATION);

          if (isFrontFacing) {
              rotationCompensation = (sensorOrientation + rotationCompensation) % 360;
          } else { // back-facing
              rotationCompensation = (sensorOrientation - rotationCompensation + 360) % 360;
          }
      } catch (CameraAccessException e) {
          e.printStackTrace();
      }

      Log.d(TAG, "Rotation Compensation: " + rotationCompensation);

      return rotationCompensation;
  }

  private String getCameraId(CameraManager cManager, Boolean isFrontFacing) {
    try {
      String cameraId = null;

      int targetOrientation = isFrontFacing == true ? CameraCharacteristics.LENS_FACING_FRONT
          : CameraCharacteristics.LENS_FACING_BACK;

      for (int i = 0; i < cManager.getCameraIdList().length; i++) {
        cameraId = cManager.getCameraIdList()[i];
        int cameraOrientation = cManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING);
        if (cameraOrientation == targetOrientation) {
          return cameraId;
        }
      }

      return cameraId;
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }

    Log.d(TAG, "Camera ID: " + cameraId);

    return null;
  }
}
