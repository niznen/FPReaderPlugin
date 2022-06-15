import { WebPlugin } from '@capacitor/core';

import type { FPReaderPlugin } from './definitions';

export class FPReaderWeb extends WebPlugin implements FPReaderPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
