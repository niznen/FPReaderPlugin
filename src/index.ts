import { registerPlugin } from '@capacitor/core';

import type { FPReaderPlugin } from './definitions';

const FPReader = registerPlugin<FPReaderPlugin>('FPReader', {
  web: () => import('./web').then(m => new m.FPReaderWeb()),
});

export * from './definitions';
export { FPReader };
