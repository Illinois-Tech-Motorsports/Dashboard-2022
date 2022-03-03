package com.iit.dashboard2022.util;

import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;

/**
 * Utility class with general helpers for the dashboard
 *
 * @author Noah Husby
 */
public class HawkUtil {

    /**
     * Sets the window's flags according to pre-set parameters
     *
     * @param window {@link Window}
     */
    public static void setWindowFlags(Window window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.getInsetsController().hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        } else {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                                                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                                                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }
}
