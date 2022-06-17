import { WebPlugin } from '@capacitor/core';

import type { FPReaderPlugin } from './definitions';

export class FPReaderWeb extends WebPlugin implements FPReaderPlugin {
  async requestPermission(): Promise<any> {
    throw new Error('Plugin Not Available for Web');
  }
  async capture(): Promise<any> {
    throw new Error('Plugin Not Available for Web');
  }

  async getDeviceInfo(): Promise<string> {
    throw new Error('Plugin Not Available for Web');
  }
}
