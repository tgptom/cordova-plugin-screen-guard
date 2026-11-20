package com.tgptom.screenguard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.UiAutomation;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.tgptom.screenguard.test.MainActivity;
import com.tgptom.screenguard.test.OtherActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Verifies with real screenshots (shell `screencap`, same as `adb shell screencap`) that FLAG_SECURE blocks capture
 * and that clearing it restores capture. Run on API 24..29 in CI.
 */
@RunWith(AndroidJUnit4.class)
public class SecureFlagScreenshotTest {

    private static final long TIMEOUT_MS = 10_000;

    private ActivityScenario<? extends Activity> scenario;

    @Before
    public void setUp() {
        SecureFlagController.resetClearUnsupported(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @After
    public void tearDown() {
        if (scenario != null) scenario.close();
        SecureFlagController.resetClearUnsupported(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @Test
    public void screenshotVisibleByDefault() {
        scenario = ActivityScenario.launch(MainActivity.class);
        assertTrue("content should be capturable before enabling", awaitCapturable(true));
    }

    @Test
    public void enableBlocksAndDisableRestoresScreenshots() {
        scenario = ActivityScenario.launch(MainActivity.class);
        assertTrue(awaitCapturable(true));

        assertEquals(SecureFlagController.Result.OK, apply(true));
        assertTrue(secure());
        assertTrue("FLAG_SECURE should block screenshots on API " + Build.VERSION.SDK_INT,
                awaitCapturable(false));

        assertEquals(SecureFlagController.Result.OK, apply(false));
        assertFalse(secure());
        assertTrue("clearing FLAG_SECURE must restore screenshots on API " + Build.VERSION.SDK_INT,
                awaitCapturable(true));
    }

    @Test
    public void repeatedTogglingAlwaysRestoresScreenshots() {
        scenario = ActivityScenario.launch(MainActivity.class);
        for (int i = 0; i < 3; i++) {
            assertEquals(SecureFlagController.Result.OK, apply(true));
            assertTrue("cycle " + i + " blocked", awaitCapturable(false));
            assertEquals(SecureFlagController.Result.OK, apply(false));
            assertTrue("cycle " + i + " restored", awaitCapturable(true));
        }
    }

    @Test
    public void enableIsRefusedOnDevicesThatCannotClear() {
        scenario = ActivityScenario.launch(MainActivity.class);
        SecureFlagController.markClearUnsupported(
                InstrumentationRegistry.getInstrumentation().getTargetContext());

        assertEquals(SecureFlagController.Result.CLEAR_UNSUPPORTED, apply(true));
        assertFalse(secure());
        assertTrue(awaitCapturable(true));
    }

    @Test
    public void onlyMainActivityIsAffected() {
        scenario = ActivityScenario.launch(OtherActivity.class);
        assertEquals(SecureFlagController.Result.NOT_MAIN_ACTIVITY, apply(true));
        assertFalse(secure());
        assertTrue(awaitCapturable(true));
    }

    private SecureFlagController.Result apply(boolean enabled) {
        AtomicReference<SecureFlagController.Result> r = new AtomicReference<>();
        scenario.onActivity(a -> r.set(SecureFlagController.apply(a, enabled)));
        return r.get();
    }

    private boolean secure() {
        AtomicReference<Boolean> r = new AtomicReference<>();
        scenario.onActivity(a -> r.set(SecureFlagController.isSecure(a.getWindow())));
        return r.get();
    }

    /** Polls until two consecutive screenshots show the magenta content (or don't). */
    private boolean awaitCapturable(boolean expected) {
        long end = SystemClock.uptimeMillis() + TIMEOUT_MS;
        int consecutive = 0;
        while (SystemClock.uptimeMillis() < end) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            Bitmap shot = screencap();
            if (shot != null) {
                boolean visible = isContentColor(shot.getPixel(shot.getWidth() / 2, shot.getHeight() / 2));
                shot.recycle();
                consecutive = visible == expected ? consecutive + 1 : 0;
                if (consecutive >= 2) return true;
            }
            SystemClock.sleep(250);
        }
        return false;
    }

    /**
     * UiAutomation.takeScreenshot() is captured by system_server, which may include
     * secure layers, so it is not a valid oracle. `screencap` runs as the shell user
     * and renders FLAG_SECURE windows black, like ordinary screenshots.
     */
    private static Bitmap screencap() {
        UiAutomation ua = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        ParcelFileDescriptor pfd = ua.executeShellCommand("screencap -p");
        try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(pfd)) {
            return BitmapFactory.decodeStream(in);
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean isContentColor(int px) {
        int c = MainActivity.COLOR;
        return Math.abs(Color.red(px) - Color.red(c)) < 24
                && Math.abs(Color.green(px) - Color.green(c)) < 24
                && Math.abs(Color.blue(px) - Color.blue(c)) < 24;
    }
}
