package sa.redbullmobile.fpreader;

import android.util.Log;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.getcapacitor.JSObject;

import com.integratedbiometrics.ibscanultimate.IBScan;
import com.integratedbiometrics.ibscanultimate.IBScanDevice;
import com.integratedbiometrics.ibscanultimate.IBScanDeviceListener;
import com.integratedbiometrics.ibscanultimate.IBScanException;
import com.integratedbiometrics.ibscanultimate.IBScanListener;

import com.getcapacitor.PluginCall;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;


public class FPReader implements IBScanListener, IBScanDeviceListener {

    private static final String TAG = "RBM";

    public IBScan IBActivityScan;
    public Boolean IsIBScan = null;
    public IBScanDevice IBActivityScanDevice = null;
    private PluginCall callbackContext = null;
    private String mDeviceSN = null;
    private int deviceId = 0;
    private IBScanDevice.FingerQualityState fingerQualy = IBScanDevice.FingerQualityState.FINGER_NOT_PRESENT;

    public String echo(String value) {
        Log.i(TAG, value);
        return value;
    }

    public FPReader() {
    }

    public FPReader(IBScan IBActivityScan, PluginCall call) throws IBScanException {
        Log.d(TAG, "Initializing");
        callbackContext = call;
        this.IBActivityScan = IBActivityScan;
        this.IBActivityScan.setScanListener(this);
    }

    public String GetDeviceInfo(){
        String serial = "";
        try {
            Log.d(TAG,"Device Count: "+IBActivityScan.getDeviceCount());
            //serial = IBActivityScan.getDeviceDescription(0).serialNumber;
        } catch (IBScanException e) {
            e.printStackTrace();
            return e.getMessage();
        }
        return serial;
    }

    public void SetCallbackContext(PluginCall callbackContext)  {
        this.callbackContext = callbackContext;
    }

    public void SetDeviceScanListener() {
        if (IBActivityScanDevice != null)
            IBActivityScanDevice.setScanDeviceListener(this);
    }

    public void SetDeviceSN(String SN) {
        mDeviceSN = SN;
    }

    public void OnPermissionGranted(boolean granted) {
        String message;
        String code;
        boolean status;
        if (granted) {
            IsIBScan = true;
            code = "0";
            status = true;
            message = "Permission to connect to the device was accepted!";
        }
        else {
            IsIBScan = null;
            code = "-1";
            status = false;
            message = "Permission to connect to the device was denied!";
        }
        callbackContext.setKeepAlive(true);
        returnResult(code, message, status);
    }

    public void ScanFingerprint(IBScanDevice device) throws Exception {
        IBScanDevice.ImageResolution imgRes = IBScanDevice.ImageResolution.RESOLUTION_500;
        boolean bAvailable = device.isCaptureAvailable(IBScanDevice.ImageType.FLAT_SINGLE_FINGER, imgRes);
        if (!bAvailable)
            throw new Exception(TAG+"The capture mode (" + IBScanDevice.ImageType.FLAT_SINGLE_FINGER + ") is not available");

        int captureOptions = 0;
        captureOptions |= IBScanDevice.OPTION_AUTO_CONTRAST;
        captureOptions |= IBScanDevice.OPTION_AUTO_CAPTURE;
        captureOptions |= IBScanDevice.OPTION_IGNORE_FINGER_COUNT;

        device.beginCaptureImage(IBScanDevice.ImageType.FLAT_SINGLE_FINGER, imgRes, captureOptions);

        Thread.sleep(2000);
    }

    private void returnResult(String code, String message, boolean isSuccess){
        JSObject ret = new JSObject();
        ret.put("code", code);
        ret.put("message",message);
        if(isSuccess)
            callbackContext.resolve(ret);
        else
            callbackContext.reject(message, code);
    }

    private void sendError(long result) {
        JSObject json = new JSObject();
        try {
            json.put("errorCode", result);
            json.put("device", mDeviceSN);
            json.put("deviceModel", android.os.Build.MODEL);

            callbackContext.reject(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
            callbackContext.reject("" + result);
        }
    }


    private void debugMessage(String message) {
        Log.d(TAG, message);
    }


    @Override
    public void deviceCommunicationBroken(IBScanDevice ibScanDevice) {

    }

    @Override
    public void deviceImagePreviewAvailable(IBScanDevice ibScanDevice, IBScanDevice.ImageData imageData) {
        Log.d(TAG, "Device Image Preview Available");
    }

    @Override
    public void deviceFingerCountChanged(IBScanDevice device, IBScanDevice.FingerCountState fingerState) {
        Log.d(TAG, "Device Finger Count Changed");
        if (fingerState == IBScanDevice.FingerCountState.NON_FINGER) {
            Log.d(TAG, "NON_FINGER");
            try {
                device.cancelCaptureImage();
            } catch (IBScanException e) {
                e.printStackTrace();
            }

            Log.d(TAG, "Error 57");
            sendError(57);
        }
    }

    @Override
    public void deviceFingerQualityChanged(IBScanDevice device, IBScanDevice.FingerQualityState[] fingerQualities) {
        fingerQualy = IBScanDevice.FingerQualityState.FINGER_NOT_PRESENT;
        for (IBScanDevice.FingerQualityState state : fingerQualities) {
            if (state != IBScanDevice.FingerQualityState.FINGER_NOT_PRESENT) {
                fingerQualy = state;
                break;
            }
        }
        Log.d(TAG, "Finger Qualy: " + fingerQualy);
    }

    @Override
    public void deviceAcquisitionBegun(IBScanDevice ibScanDevice, IBScanDevice.ImageType imageType) {

    }

    @Override
    public void deviceAcquisitionCompleted(IBScanDevice ibScanDevice, IBScanDevice.ImageType imageType) {

    }

    @Override
    public void deviceImageResultAvailable(IBScanDevice ibScanDevice, IBScanDevice.ImageData imageData, IBScanDevice.ImageType imageType, IBScanDevice.ImageData[] imageData1) {

    }

    @Override
    public void deviceImageResultExtendedAvailable(IBScanDevice device, IBScanException imageStatus, IBScanDevice.ImageData image, IBScanDevice.ImageType imageType, int detectedFingerCount, IBScanDevice.ImageData[] segmentImageArray, IBScanDevice.SegmentPosition[] segmentPositionArray) {
        Log.d(TAG, "Device Image Result Extended Available");
        if (imageStatus != null &&  imageStatus.getType().compareTo(IBScanException.Type.INVALID_PARAM_VALUE) <= 0) {
            Log.d(TAG, "Error 57");
            sendError(57);
            return;
        }

        if (fingerQualy == IBScanDevice.FingerQualityState.FINGER_NOT_PRESENT) {
            Log.d(TAG, "FINGER_NOT_PRESENT");
            try {
                ScanFingerprint(device);
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
                Log.d(TAG, "Error 57");
                sendError(57);
            }

            return;
        }

        if (fingerQualy != IBScanDevice.FingerQualityState.GOOD && fingerQualy != IBScanDevice.FingerQualityState.FAIR) {
            Log.d(TAG, "Finger Quality State Is Bad");
            try {
                ScanFingerprint(device);
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
                Log.d(TAG, "Error 57");
                sendError(57);
            }

            return;
        }

        if (segmentImageArray.length == 1)
            image = segmentImageArray[0];

        Log.d(TAG, "image.isFinal = " + image.isFinal);

        try {
            Log.d(TAG, "image.buffer.length" + image.buffer.length);

            Log.d(TAG, "wsqEncodeToMem");
            Object[] obj = IBActivityScanDevice.wsqEncodeToMem(image.buffer, image.width, image.height, image.pitch, image.bitsPerPixel, (int)image.resolutionX, .75, "");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj[0]);
            oos.flush();
            byte [] wsqBytes1 = bos.toByteArray();
            byte [] wsqBytes = new byte[wsqBytes1.length - 27];
            System.arraycopy(wsqBytes1, 27, wsqBytes, 0, wsqBytes1.length - 27);
            String source = Base64.encodeToString(wsqBytes, Base64.DEFAULT);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            image.toBitmap().compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] pngBytes = baos.toByteArray();
            String base64 = Base64.encodeToString(pngBytes, Base64.DEFAULT);

            int quality = (fingerQualy == IBScanDevice.FingerQualityState.GOOD) ? 80 : 60;

            JSObject json = new JSObject();
            try {
                json.put("img", base64);
                json.put("qlty", quality);
                json.put("source", source);
                json.put("width", image.width);
                json.put("height", image.height);
                json.put("device", mDeviceSN);
                json.put("deviceModel", android.os.Build.MODEL);
                callbackContext.setKeepAlive(true);
                callbackContext.resolve(json);
            } catch (Exception e) {
                e.printStackTrace();
                sendError(101);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(57);
        }
    }

    @Override
    public void devicePlatenStateChanged(IBScanDevice ibScanDevice, IBScanDevice.PlatenState platenState) {

    }

    @Override
    public void deviceWarningReceived(IBScanDevice ibScanDevice, IBScanException e) {

    }

    @Override
    public void devicePressedKeyButtons(IBScanDevice ibScanDevice, int i) {

    }

    @Override
    public void scanDeviceAttached(int deviceId) {
        IsIBScan = true;
        if (!IBActivityScan.hasPermission(deviceId))
            IBActivityScan.requestPermission(deviceId);
    }

    @Override
    public void scanDeviceDetached(int deviceId) {
        Log.d(TAG, "Device is detached");
        IsIBScan = null;
        if (IBActivityScanDevice != null) {
            try {
                IBActivityScanDevice.close();
            } catch (IBScanException e) {
                e.printStackTrace();
                debugMessage(e.getMessage());
            }
        }

        IBActivityScanDevice = null;
    }

    @Override
    public void scanDevicePermissionGranted(int deviceid, boolean granted) {
        deviceId = deviceid;
        OnPermissionGranted(granted);
    }

    @Override
    public void scanDeviceCountChanged(int deviceCount) {
        if (deviceCount > 0)
            IsIBScan = true;
        else
            IsIBScan = null;
    }

    @Override
    public void scanDeviceInitProgress(int i, int i1) {

    }

    @Override
    public void scanDeviceOpenComplete(int i, IBScanDevice ibScanDevice, IBScanException e) {

    }
}
