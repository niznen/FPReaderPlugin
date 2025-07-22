import { registerPlugin } from '@capacitor/core';
const FPReader = registerPlugin('FPReader', {
    web: () => import('./web').then(m => new m.FPReaderWeb()),
});
export * from './definitions';
export { FPReader };
//# sourceMappingURL=index.js.map