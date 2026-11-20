package com.tgptom.screenguard;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.Window;
import android.view.WindowManager;

/**
 * Adds/clears FLAG_SECURE on a window. Must be called on the UI thread.
 *
 * Safety rule: the flag must never be left on permanently. If a device ever
 * refuses to clear FLAG_SECURE, that is persisted and future enable requests
 * are refused, so the app degrades to "no protection" rather than
 * "screenshots blocked forever".
 */
public final class SecureFlagController {

    static final String PREFS = "cordova-plugin-screen-guard";
    static final String KEY_CLEAR_UNSUPPORTED = "clearUnsupported";
    static final String MAIN_ACTIVITY = "MainActivity";

    public enum Result { OK, NOT_MAIN_ACTIVITY, CLEAR_UNSUPPORTED, SET_FAILED }

    private SecureFlagController() {}

    public static boolean isMainActivity(Activity activity) {
        return activity != null && MAIN_ACTIVITY.equals(activity.getClass().getSimpleName());
    }

    public static boolean isSecure(Window window) {
        return (window.getAttributes().flags & WindowManager.LayoutParams.FLAG_SECURE) != 0;
    }

    public static Result apply(Activity activity, boolean enabled) {
        if (!isMainActivity(activity)) {
            return Result.NOT_MAIN_ACTIVITY;
        }
        return enabled ? enable(activity) : disable(activity);
    }

    private static Result enable(Activity activity) {
        Window window = activity.getWindow();
        if (isClearUnsupported(activity)) {
            // Make sure it is off; never turn it on again on this device.
            clear(window);
            return Result.CLEAR_UNSUPPORTED;
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        return isSecure(window) ? Result.OK : Result.SET_FAILED;
    }

    private static Result disable(Activity activity) {
        if (clear(activity.getWindow())) {
            return Result.OK;
        }
        markClearUnsupported(activity);
        return Result.CLEAR_UNSUPPORTED;
    }

    /** Returns true if FLAG_SECURE is no longer set. */
    static boolean clear(Window window) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        if (!isSecure(window)) {
            return true;
        }
        // Fallback for windows whose flags were mutated elsewhere: push a fresh copy.
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(window.getAttributes());
        lp.flags &= ~WindowManager.LayoutParams.FLAG_SECURE;
        window.setAttributes(lp);
        return !isSecure(window);
    }

    static boolean isClearUnsupported(Context ctx) {
        return prefs(ctx).getBoolean(KEY_CLEAR_UNSUPPORTED, false);
    }

    static void markClearUnsupported(Context ctx) {
        prefs(ctx).edit().putBoolean(KEY_CLEAR_UNSUPPORTED, true).apply();
    }

    static void resetClearUnsupported(Context ctx) {
        prefs(ctx).edit().remove(KEY_CLEAR_UNSUPPORTED).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
