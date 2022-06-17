export interface FPReaderPlugin {
  getDeviceInfo(): Promise<any>;
  requestPermission(): Promise<any>;
  capture(): Promise<any>;
}
