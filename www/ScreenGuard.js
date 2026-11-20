var exec = require('cordova/exec');
var channel = require('cordova/channel');
var cordova = require('cordova');

var SERVICE = 'ScreenGuard';

var ScreenGuard = {
    /**
     * Android: adds/clears FLAG_SECURE on MainActivity.
     * iOS: arms/disarms the "screenguard" document event (iOS cannot block screenshots).
     */
    setSecure: function (enabled, success, error) {
        if (typeof enabled !== 'boolean') {
            var err = { code: 'INVALID_ARGUMENT', message: 'enabled must be a boolean' };
            if (typeof error === 'function') {
                setTimeout(function () { error(err); }, 0);
                return;
            }
            throw new TypeError(err.message);
        }
        exec(success, error, SERVICE, 'setSecure', [enabled]);
    }
};

// iOS delivers notifications through a long-lived callback and they are
// re-dispatched as a document event: document.addEventListener('screenguard', fn).
channel.onCordovaReady.subscribe(function () {
    if (cordova.platformId !== 'ios') return;
    exec(function (info) {
        cordova.fireDocumentEvent('screenguard', info || {});
    }, function () {}, SERVICE, 'watch', []);
});

module.exports = ScreenGuard;
