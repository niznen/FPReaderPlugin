import { WebPlugin } from '@capacitor/core';
import type { FPReaderPlugin } from './definitions';
export declare class FPReaderWeb extends WebPlugin implements FPReaderPlugin {
    requestPermission(): Promise<any>;
    capture(): Promise<any>;
    getDeviceInfo(): Promise<string>;
}
