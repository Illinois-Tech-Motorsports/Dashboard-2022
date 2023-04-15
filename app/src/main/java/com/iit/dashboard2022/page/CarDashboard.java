package com.iit.dashboard2022.page;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.iit.dashboard2022.R;
import com.iit.dashboard2022.ecu.ECU;
import com.iit.dashboard2022.ecu.Metric;
import com.iit.dashboard2022.ecu.State;
import com.iit.dashboard2022.ui.UITester;
import com.iit.dashboard2022.ui.widget.Indicators;
import com.iit.dashboard2022.ui.widget.SpeedText;
import com.iit.dashboard2022.ui.widget.StartLight;
import com.iit.dashboard2022.ui.widget.gauge.LinearGauge;
import com.iit.dashboard2022.ui.widget.gauge.SpeedGauge;

import java.util.Locale;

public class CarDashboard extends Page implements UITester.TestUI {
    private StartLight dashStartLight;
    private SpeedGauge sgL, sgR;
    private LinearGauge batteryGauge, powerGauge;
    private LinearGauge lTempGauge, rTempGauge;
    private SpeedText speedometer;
    private Indicators indicators;
    private String limitFormat;
    private ECU frontECU;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.tab_dashboard_layout, container, false);
        sgL = rootView.findViewById(R.id.speedGaugeLeft);
        sgR = rootView.findViewById(R.id.speedGaugeRight);
        batteryGauge = rootView.findViewById(R.id.batteryGauge);
        powerGauge = rootView.findViewById(R.id.powerGauge);
        lTempGauge = rootView.findViewById(R.id.lTempGauge);
        rTempGauge = rootView.findViewById(R.id.rTempGauge);
        speedometer = rootView.findViewById(R.id.speedometer);
        indicators = rootView.findViewById(R.id.indicators);
        dashStartLight = rootView.findViewById(R.id.dashStartLight);

        limitFormat = inflater.getContext().getString(R.string.limit_format);

        reset();
        UITester.addTest(this);
        return rootView;
    }

    @Override
    public void onDestroy() {
        UITester.removeTest(this);
        super.onDestroy();
    }

    public void setSpeedPercentage(float percent) {
        sgL.setTaper(percent);
        sgR.setTaper(percent);
        sgL.setPercent(percent);
        sgR.setPercent(percent);
    }

    public void setBatteryPercentage(float percent) {
        batteryGauge.setPercent(percent);
        batteryGauge.setValue(Math.round(percent * 100f));
    }

    public void setLeftTempValue(int value) {
        lTempGauge.setValue(value);
    }

    public void setLeftTempPercentage(float percent) {
        lTempGauge.setPercent(percent);
    }

    public void setRightTempValue(int value) {
        rTempGauge.setValue(value);
    }

    public void setRightTempPercentage(float percent) {
        rTempGauge.setPercent(percent);
    }

    public void setPowerValue(int value) {
        powerGauge.setValue(value);
    }

    public void setPowerPercentage(float percent) {
        powerGauge.setPercent(percent);
    }

    public void setPowerLimit(int limit) {
        powerGauge.setBottomText(String.format(Locale.US, limitFormat, limit));
    }

    public void setSpeedValue(long mph) {
        speedometer.setText(String.valueOf(mph)); // Auto-sized textView seems to cause a lot of allocations
    }

    public void setIndicator(Indicators.Indicator indicator, boolean enabled) {
        indicators.setIndicator(indicator, enabled);
    }

    public void setLagTime(long ms) {
        indicators.setLagTime(ms);
    }

    public void setStartLight(boolean isOn) {
        dashStartLight.setLight(isOn);
    }

    public void setState(CharSequence state) {
        dashStartLight.setState(state);
    }

    public void setECU(ECU frontECU) {
        this.frontECU = frontECU;
    }

    public void reset() {
        if (dashStartLight != null) {
            dashStartLight.postDelayed(() -> {
                setSpeedPercentage(0);
                setBatteryPercentage(0);
                setPowerPercentage(0);
                setPowerValue(0);
                setPowerLimit(0);
                powerGauge.invalidate();
                setSpeedValue(0);
                setLagTime(0);
                for (Indicators.Indicator indicator : Indicators.Indicator.values()) {
                    setIndicator(indicator, false);
                }
                setStartLight(false);
                if (frontECU != null) {
                    Metric.STATE.update(-1);
                }
                setState(State.INITIALIZING.getName());
            }, 20);
        }
    }

    @NonNull
    @Override
    public String getTitle() {
        return "Dashboard";
    }

    @Override
    public void testUI(float percent) {
        setSpeedPercentage(percent);
        setBatteryPercentage(percent);
        setPowerPercentage(percent);
        setRightTempPercentage(percent);
        setLeftTempPercentage(percent);
        setLeftTempValue((int) (percent * 400));
        setRightTempValue((int) (percent * 400));
        setPowerValue((int) (percent * 1000));
        setPowerLimit((int) (percent * 1000));
        setSpeedValue((int) (300 * percent));
        setStartLight(percent > 0.5f);
        if (percent == 0) {
            setState("Current State");
            for (Indicators.Indicator i : Indicators.Indicator.values()) {
                setIndicator(i, false);
            }
        } else {
            setState(UITester.rndStr((int) (percent * 25)));
            for (Indicators.Indicator i : Indicators.Indicator.values()) {
                if (i != Indicators.Indicator.Lag && UITester.Rnd.nextFloat() > 0.9) {
                    setIndicator(i, percent > 0.5);
                }
            }
            setIndicator(Indicators.Indicator.Lag, true);
            setLagTime((long) (percent * 5000));
        }
    }
}
