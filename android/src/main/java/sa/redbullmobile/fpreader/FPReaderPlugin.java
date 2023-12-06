package sa.redbullmobile.fpreader;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
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

import java.util.HashMap;
import java.util.Iterator;

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
    public void getDeviceInfo(PluginCall call) throws IBScanException {
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
    public void requestPermission(PluginCall call) throws IBScanException {
        debugMessage("RBM:"+"requestPermission");
        if(ibActivityScanListener == null) {
            IBScan ibScan = IBScan.getInstance(this.getContext());
            debugMessage("RBM:"+"gotInstance: "+ibScan.getDeviceCount());
            ibActivityScanListener = new FPReader(ibScan, call);
            debugMessage("RBM:"+"FPReader Initiated: "+ibActivityScanListener.GetDeviceInfo());
            initDeviceSettings(call);
        }
        if(!hasRequiredPermissions()) {
            debugMessage("RBM:"+"Else");
            requestAllPermissions(call, "onPermsCallback");
        }
    }

    @PermissionCallback
    private void onPermsCallback(PluginCall call){
        debugMessage("RBM:"+"onPermsCallback: "+hasRequiredPermissions()+"\n");
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
    public void capture(PluginCall callbackContext) throws IBScanException {
        debugMessage("RBM: Capture x Pressed CAPTURE captureB64");
        dwTimeStart = System.currentTimeMillis();

        long result = 0;
        IBScanDevice ibScanDevice = ibActivityScanListener.IBActivityScanDevice;
        ibActivityScanListener.SetCallbackContext(callbackContext);

        if(ibScanDevice == null){
            debugMessage("RBM: Re-inititing Device\n");
            IBScan ibScan = IBScan.getInstance(context);
            debugMessage("RBM: Initiated: "+ibScan.getDeviceCount()+"\n");
            ibActivityScanListener = new FPReader(ibScan, callbackContext);
            debugMessage("RBM: Created: "+ibActivityScanListener.GetDeviceInfo()+"\n");
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
                final UsbManager manager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
                final HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
                final Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
                UsbDevice usbDevice = deviceIterator.next();
                debugMessage("RBM:USBDevice: " + usbDevice.toString() + "\n");
                if(ibScan.getDeviceCount() > 0) {
                    debugMessage("RBM:Before getDeviceCount()" + ibScan.getDeviceCount() + "\n");
                    IBScan.DeviceDesc deviceDesc = ibScan.getDeviceDescription(0);
                    debugMessage("RBM:After getDeviceDescription()\n");

                    ibActivityScanListener.SetDeviceSN(deviceDesc.serialNumber);
                    mDeviceSN = deviceDesc.serialNumber;
                    //ibActivityScanListener.SetDeviceSN(usbDevice.getSerialNumber());
                    //mDeviceSN = usbDevice.getSerialNumber();
                    debugMessage("RBM:Setting props: Device: " + usbDevice.getSerialNumber());

                    debugMessage("RBM:Before OpenDevice()\n");
                    device = ibScan.openDevice(0);
                    debugMessage("RBM:After OpenDevice()\n");

                    if (!device.isOpened())
                        throw new Exception("Failed to open the device");

                    ibActivityScanListener.IBActivityScanDevice = device;
                    ibActivityScanListener.SetDeviceScanListener();
                    debugMessage("Device Open Successful");
                }else{
                    debugMessage("RBM:isScanDevice()" + IBScan.isScanDevice(usbDevice) + "\n");
                    final boolean isScanDevice = IBScan.isScanDevice(usbDevice);
                    if (isScanDevice) {
                        final boolean hasPermission = manager.hasPermission(usbDevice);
                        debugMessage("RBM:hasPermission()" + hasPermission + "\n");
                        if (!hasPermission)
                        {
                            debugMessage("RBM:before requestPermission()\n");
                            ibScan.requestPermission(usbDevice.getDeviceId());
                            debugMessage("RBM:after requestPermission()\n");
                            debugMessage("RBM:hasPermission: "+hasPermission+"\n");
                        }
                        //device = ibScan.openDevice(0);
                    }
                }
            }
        } catch (Exception e) {
            debugMessage(""+e.getMessage());
            e.printStackTrace();
            callbackContext.reject("101");
            return;
        }
    }
}
