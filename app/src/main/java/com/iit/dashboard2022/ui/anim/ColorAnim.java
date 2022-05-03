package com.iit.dashboard2022.ui.anim;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import com.iit.dashboard2022.util.Constants;

public class ColorAnim {
    private final float[] anim_from = new float[3], anim_to = new float[3];
    private final float[] anim_hsv = new float[3];
    private final ValueAnimator anim;

    public ColorAnim(@NonNull Context context, @ColorRes int from, @ColorRes int to, @NonNull colorUpdater updater) {
        Color.colorToHSV(context.getColor(from), anim_from);
        Color.colorToHSV(context.getColor(to), anim_to);

        anim = ValueAnimator.ofFloat(0, 1);
        anim.setDuration(Constants.ANIM_DURATION);

        anim.addUpdateListener(animation -> {
            anim_hsv[0] = anim_from[0] + (anim_to[0] - anim_from[0]) * (4 * animation.getAnimatedFraction());
            anim_hsv[1] = anim_from[1] + (anim_to[1] - anim_from[1]) * animation.getAnimatedFraction();
            anim_hsv[2] = anim_from[2] + (anim_to[2] - anim_from[2]) * animation.getAnimatedFraction();
            updater.update(Color.HSVToColor(anim_hsv));
        });

    }

    public void reverse() {
        anim.reverse();
    }

    public void start() {
        anim.start();
    }

    public void cancel() {
        anim.cancel();
    }

    public interface colorUpdater {
        void update(@ColorInt int color);
    }

}
