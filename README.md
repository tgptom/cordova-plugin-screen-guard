# cordova-plugin-screen-guard

Protect sensitive screens, such as OTP or pairing codes, from screenshots.

| Platform | Supported versions | Behaviour |
|---|---|---|
| Android | cordova-android 14, 15 (API 24+) | `FLAG_SECURE` on **MainActivity** blocks screenshots and screen recording |
| iOS | cordova-ios 7, 8 | **Cannot block screenshots.** Fires a `screenguard` event so the app can hide the content |

## Install

```sh
cordova plugin add https://github.com/tgptom/cordova-plugin-screen-guard.git
```

## API

```js
ScreenGuard.setSecure(enabled, success, error);
```

- `enabled` (`boolean`): turns protection on or off.
- `success()`: called when the change is applied.
- `error({ code, message })`: called with one of these codes: `INVALID_ARGUMENT`, `NOT_MAIN_ACTIVITY`, `NO_ACTIVITY`, `CLEAR_UNSUPPORTED`, `SET_FAILED` or `UNKNOWN`.

### Example

```js
document.addEventListener('deviceready', function () {
  ScreenGuard.setSecure(true);            // entering the OTP screen

  // iOS only: hide the OTP when the app is about to lose focus or after a screenshot.
  document.addEventListener('screenguard', function (e) {
    if (e.reason === 'resignActive' || e.reason === 'screenshot') hideOtp();
    if (e.reason === 'becomeActive') showOtp();
  });
});

// Leaving the OTP screen, or showing a pairing code that must stay screenshotable:
ScreenGuard.setSecure(false);
```

## Android

- On the UI thread, the plugin calls `addFlags(FLAG_SECURE)` when enabled and `clearFlags(FLAG_SECURE)` when disabled.
- The flag is only applied when the Cordova activity is `MainActivity`. Any other activity gets the error `NOT_MAIN_ACTIVITY`.
- **The flag is never left on permanently.** After clearing, the plugin checks the window flags. If the flag is still set, it tries again with a fresh copy of the `LayoutParams`. If that also fails:
  - `setSecure(false)` reports `CLEAR_UNSUPPORTED`.
  - The device is remembered in SharedPreferences, and every later `setSecure(true)` is refused with `CLEAR_UNSUPPORTED`.

  Losing protection is safer than blocking screenshots forever, because some content (such as pairing codes) must stay screenshotable.
- The flag is also cleared on page reload/navigation (`onReset`) and in `onDestroy`.

## iOS

iOS has no supported API to block screenshots. This plugin deliberately does **not** use the private "secure `UITextField`" layer trick, because it can black out the whole WebView.

While `setSecure(true)` is active, the plugin fires a document event called `screenguard`. Its `reason` property is one of:

| `reason` | Source notification |
|---|---|
| `resignActive` | `UIApplicationWillResignActiveNotification` (app switcher, Control Center, incoming call, …) |
| `screenshot` | `UIApplicationUserDidTakeScreenshotNotification` |
| `becomeActive` | `UIApplicationDidBecomeActiveNotification` (so the app can show the content again) |

> ⚠️ A screenshot taken while the OTP is visible **still succeeds**. The `screenshot` event arrives only after the screenshot has been taken, so it can only clear or rotate the OTP afterwards.

A page reload resets the plugin to disabled.

## Tests

- `npm test` runs the JS unit tests.
- `tests/android` is an instrumented test project. It compiles the real plugin sources against `org.apache.cordova:framework`, takes real screenshots with shell `screencap`, and checks that:
  - enabling the flag blocks capture,
  - clearing it restores capture, including over repeated toggles,
  - devices that cannot clear the flag never get it enabled,
  - only `MainActivity` is affected.

  CI runs these tests on emulators for **API 24, 25, 26, 27, 28 and 29**:

  ```sh
  cd tests/android && gradle connectedDebugAndroidTest   # -PcordovaFramework=14.0.1 to test against 14
  ```
- CI also builds sample apps with cordova-android 14 and 15, and with cordova-ios 7 and 8.
