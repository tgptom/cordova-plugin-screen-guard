package com.tgptom.screenguard;

import android.app.Activity;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ScreenGuard extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (!"setSecure".equals(action)) {
            return false;
        }
        Object arg = args.opt(0);
        if (!(arg instanceof Boolean)) {
            callbackContext.error(error("INVALID_ARGUMENT", "enabled must be a boolean"));
            return true;
        }
        final boolean enabled = (Boolean) arg;
        final Activity activity = cordova.getActivity();
        if (activity == null) {
            callbackContext.error(error("NO_ACTIVITY", "No activity available"));
            return true;
        }
        activity.runOnUiThread(() -> {
            try {
                SecureFlagController.Result r = SecureFlagController.apply(activity, enabled);
                switch (r) {
                    case OK:
                        callbackContext.success();
                        break;
                    case NOT_MAIN_ACTIVITY:
                        callbackContext.error(error("NOT_MAIN_ACTIVITY",
                                "FLAG_SECURE is only applied to MainActivity, got " + activity.getClass().getName()));
                        break;
                    case CLEAR_UNSUPPORTED:
                        callbackContext.error(error("CLEAR_UNSUPPORTED",
                                "This device cannot clear FLAG_SECURE; screen protection is disabled to keep the screen capturable"));
                        break;
                    default:
                        callbackContext.error(error("SET_FAILED", "FLAG_SECURE could not be set"));
                }
            } catch (Exception e) {
                callbackContext.error(error("UNKNOWN", String.valueOf(e.getMessage())));
            }
        });
        return true;
    }

    /** Page reload/navigation: never carry FLAG_SECURE over to a new page. */
    @Override
    public void onReset() {
        clearOnUiThread();
    }

    @Override
    public void onDestroy() {
        clearOnUiThread();
    }

    private void clearOnUiThread() {
        final Activity activity = cordova.getActivity();
        if (activity == null || !SecureFlagController.isMainActivity(activity)) return;
        activity.runOnUiThread(() -> {
            try {
                SecureFlagController.apply(activity, false);
            } catch (Exception ignored) {
                // Window may already be gone.
            }
        });
    }

    private static JSONObject error(String code, String message) {
        JSONObject o = new JSONObject();
        try {
            o.put("code", code);
            o.put("message", message);
        } catch (JSONException ignored) {
            // Cannot happen with non-null string keys.
        }
        return o;
    }
}
