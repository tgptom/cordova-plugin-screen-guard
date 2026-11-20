'use strict';
// Loads www/ScreenGuard.js with mocked cordova modules.
const assert = require('assert');
const fs = require('fs');
const path = require('path');

function load(platformId) {
    const calls = [];
    const fired = [];
    let readyCb;
    const mocks = {
        'cordova/exec': (s, e, service, action, args) => calls.push({ s, e, service, action, args }),
        'cordova/channel': { onCordovaReady: { subscribe: (fn) => { readyCb = fn; } } },
        cordova: { platformId, fireDocumentEvent: (type, data) => fired.push({ type, data }) }
    };
    const src = fs.readFileSync(path.join(__dirname, '../../www/ScreenGuard.js'), 'utf8');
    const module = { exports: {} };
    new Function('require', 'module', 'exports', src)((n) => mocks[n], module, module.exports);
    readyCb();
    return { api: module.exports, calls, fired };
}

// setSecure forwards booleans to native.
{
    const { api, calls } = load('android');
    api.setSecure(true, () => {}, () => {});
    api.setSecure(false);
    assert.deepStrictEqual(calls.map((c) => [c.service, c.action, c.args]),
        [['ScreenGuard', 'setSecure', [true]], ['ScreenGuard', 'setSecure', [false]]]);
}

// Non-boolean is rejected without calling native.
{
    const { api, calls } = load('android');
    assert.throws(() => api.setSecure('yes'), TypeError);
    let err;
    api.setSecure(1, null, (e) => { err = e; });
    setTimeout(() => {
        assert.strictEqual(err.code, 'INVALID_ARGUMENT');
        assert.strictEqual(calls.length, 0);
    }, 5);
}

// iOS subscribes once and re-fires native results as a "screenguard" document event.
{
    const { calls, fired } = load('ios');
    assert.strictEqual(calls.length, 1);
    assert.strictEqual(calls[0].action, 'watch');
    calls[0].s({ reason: 'screenshot' });
    assert.deepStrictEqual(fired, [{ type: 'screenguard', data: { reason: 'screenshot' } }]);
}

// Android does not subscribe.
assert.strictEqual(load('android').calls.length, 0);

setTimeout(() => console.log('JS tests passed'), 10);
