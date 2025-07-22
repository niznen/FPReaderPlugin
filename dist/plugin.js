var capacitorFPReader = (function (exports, core) {
    'use strict';

    const FPReader = core.registerPlugin('FPReader', {
        web: () => Promise.resolve().then(function () { return web; }).then(m => new m.FPReaderWeb()),
    });

    class FPReaderWeb extends core.WebPlugin {
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

    var web = /*#__PURE__*/Object.freeze({
        __proto__: null,
        FPReaderWeb: FPReaderWeb
    });

    exports.FPReader = FPReader;

    Object.defineProperty(exports, '__esModule', { value: true });

    return exports;

})({}, capacitorExports);
//# sourceMappingURL=plugin.js.map
