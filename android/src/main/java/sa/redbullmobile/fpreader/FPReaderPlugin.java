package sa.redbullmobile.fpreader;

import android.app.Activity;
import android.hardware.usb.UsbManager;
import android.nfc.Tag;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.PermissionCallback;
import com.integratedbiometrics.ibscanultimate.IBScan;
import com.integratedbiometrics.ibscanultimate.IBScan.SdkVersion;
import com.integratedbiometrics.ibscanultimate.IBScanDevice;
import com.integratedbiometrics.ibscanultimate.IBScanException;

@CapacitorPlugin(name = "FPReader")
public class FPReaderPlugin extends Plugin{
    private FPReader ibActivityScanListener = null;
    private static final String TAG = "IBScanListenerPlugin";

    private Activity context;

    // actions
    private static final String ACTION_REQUEST_PERMISSION = "requestPermission";
    private static final String CAPTURE = "capture";
    private byte[] mRegisterTemplate;
    private int[] mMaxTemplateSize;
    private int mImageWidth;
    private int mImageHeight;
    private String mDeviceSN;
    private boolean mLed;


    long dwTimeStart = 0, dwTimeEnd = 0, dwTimeElapsed = 0;

    private UsbManager manager;


    public void load() {
        context = this.getActivity();
    }

    @PluginMethod
    public void getDeviceInfo(PluginCall call){
        if(ibActivityScanListener == null) {
            requestPermission(call);
        }
        JSObject ret = new JSObject();
        String serial = ibActivityScanListener.GetDeviceInfo();
        ret.put("serial", serial);
        debugMessage("Serial = "+serial);
        if((serial == null) || (serial.isEmpty()))
            call.reject("Failed to get data");
        else
            call.resolve(ret);
    }

    @PluginMethod
    public void requestPermission(PluginCall call) {
        if(ibActivityScanListener == null) {
            IBScan ibScan = IBScan.getInstance(this.getContext());
            ibActivityScanListener = new FPReader(ibScan, call);
            initDeviceSettings(call);
        }
        if(!hasRequiredPermissions())
            requestAllPermissions(call, "onPermsCallback");
    }

    @PermissionCallback
    private void onPermsCallback(PluginCall call){
        if (hasRequiredPermissions()) {
            IBScan ibScan = ibActivityScanListener.IBActivityScan;
            ibActivityScanListener.IsIBScan = true;
            try {
                SdkVersion sdkVersion = ibScan.getSdkVersion();
            } catch (IBScanException e) {
                e.printStackTrace();
            }
            call.resolve();
        } else {
            call.reject("Permission is required to take a picture");
        }
    }

    private void debugMessage(String message) {
        Log.d(TAG, ""+message);
    }

    private void sendError(long result, PluginCall call) {
        JSObject json = new JSObject();
        try {
            json.put("errorCode", result);
            json.put("device", mDeviceSN);
            json.put("deviceModel", android.os.Build.MODEL);

            call.reject(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
            call.reject("" + result);
        }
    }


    @PluginMethod
    public void capture(PluginCall callbackContext) {
        debugMessage("Capture x Pressed CAPTURE captureB64");
        dwTimeStart = System.currentTimeMillis();

        long result = 0;
        IBScanDevice ibScanDevice = ibActivityScanListener.IBActivityScanDevice;
        ibActivityScanListener.SetCallbackContext(callbackContext);

        if(ibScanDevice == null){
            IBScan ibScan = IBScan.getInstance(this.getContext());
            ibActivityScanListener = new FPReader(ibScan, callbackContext);
            initDeviceSettings(callbackContext);
            ibScanDevice = ibActivityScanListener.IBActivityScanDevice;
            ibActivityScanListener.SetCallbackContext(callbackContext);
        }

        try {
            ibActivityScanListener.ScanFingerprint(ibScanDevice);
        } catch (Exception e) {
            debugMessage(e.getMessage());
            sendError(result, callbackContext);
            return;
        }

        dwTimeEnd = System.currentTimeMillis();
        dwTimeElapsed = dwTimeEnd - dwTimeStart;

    }


    public void initDeviceSettings(PluginCall callbackContext) {
        IBScan ibScan = ibActivityScanListener.IBActivityScan;
        IBScanDevice device = ibActivityScanListener.IBActivityScanDevice;

        try {
            if (device == null || !device.isOpened()) {
                debugMessage("Before getDeviceDescription()\n");
                IBScan.DeviceDesc deviceDesc = ibScan.getDeviceDescription(0);
                debugMessage("After getDeviceDescription()\n");

                ibActivityScanListener.SetDeviceSN(deviceDesc.serialNumber);
                mDeviceSN = deviceDesc.serialNumber;
                debugMessage("Setting props: Device: " + deviceDesc.serialNumber);

                debugMessage("Before OpenDevice()\n");
                device = ibScan.openDevice(0);
                debugMessage("After OpenDevice()\n");
            }

            if (!device.isOpened())
                throw new Exception("Failed to open the device");

            ibActivityScanListener.IBActivityScanDevice = device;
            ibActivityScanListener.SetDeviceScanListener();
            debugMessage("Device Open Successful");
        } catch (Exception e) {
            debugMessage(""+e.getMessage());
            e.printStackTrace();
            callbackContext.reject("101");
            return;
        }
    }
}
