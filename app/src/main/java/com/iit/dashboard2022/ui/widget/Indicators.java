package com.iit.dashboard2022.ui.widget;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.iit.dashboard2022.R;
import com.iit.dashboard2022.ui.UITester;

import java.util.Locale;

public class Indicators extends RelativeLayout implements UITester.TestUI {
    private static final Handler uiHandle = new Handler();

    private final RadioButton lagRadio, faultRadio, waitRadio, chargeRadio;
    private final TextView lagTimer;
    private final String lagTimerFormat;
    private float lagTimerLarge, LagTimerSmall;
    private String currentLagTime = "";

    public enum Indicator {
        Lag,
        Fault,
        Waiting,
        Charging,
    }

    public Indicators(Context context) {
        this(context, null);
    }

    public Indicators(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Indicators(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View.inflate(context, R.layout.widget_indicators, this);

        lagRadio = findViewById(R.id.lagRadio);
        faultRadio = findViewById(R.id.faultRadio);
        waitRadio = findViewById(R.id.waitRadio);
        chargeRadio = findViewById(R.id.chargeRadio);
        lagTimer = findViewById(R.id.lagTimer);

        lagTimerLarge = lagTimer.getTextSize() / getResources().getDisplayMetrics().scaledDensity;
        LagTimerSmall = lagTimerLarge / 1.2f;

        lagTimerFormat = context.getString(R.string.indicators_lag_timer_format);
        updateLagTime();

        for (Indicator i : Indicator.values()) {
            setIndicator(i, false);
        }

        UITester.addTest(this);
    }

    public void setIndicator(Indicator indicator, boolean enabled) {
        RadioButton rb;
        int visibility = enabled ? View.VISIBLE : View.GONE;
        switch (indicator) {
            case Lag:
                rb = lagRadio;
                lagTimer.setVisibility(visibility);
                break;
            case Fault:
                rb = faultRadio;
                break;
            case Waiting:
                rb = waitRadio;
                break;
            case Charging:
                rb = chargeRadio;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + indicator);
        }
        rb.setChecked(enabled);
        rb.setVisibility(visibility);
    }

    private void updateLagTime() {
        if (currentLagTime.length() > 5)
            lagTimer.setTextSize(LagTimerSmall);
        else
            lagTimer.setTextSize(lagTimerLarge);
        lagTimer.setText(currentLagTime);
    }

    public void setLagTime(long ms) {
        if (ms == 0) {
            currentLagTime = "";
        } else {
            currentLagTime = String.format(Locale.US, lagTimerFormat, ms);
        }
        uiHandle.post(this::updateLagTime);
    }

    @Override
    protected void finalize() throws Throwable {
        UITester.removeTest(this);
        super.finalize();
    }

    @Override
    public void testUI(float percent) {
        setLagTime((long) (percent * percent * 10000));
        if (percent == 0) {
            for (Indicator i : Indicator.values()) {
                setIndicator(i, false);
            }
        } else {
            for (Indicator i : Indicator.values()) {
                if (UITester.Rnd.nextFloat() > 0.9)
                    setIndicator(i, percent > 0.5);
            }
        }
    }
}
