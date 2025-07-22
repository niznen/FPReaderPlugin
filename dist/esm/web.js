import { WebPlugin } from '@capacitor/core';
export class FPReaderWeb extends WebPlugin {
    async requestPermission() {
        throw new Error('Plugin Not Available for Web');
    }
    async capture() {
        throw new Error('Plugin Not Available for Web');
    }
    async getDeviceInfo() {
        throw new Error('Plugin Not Available for Web');
    }
}
//# sourceMappingURL=web.js.map