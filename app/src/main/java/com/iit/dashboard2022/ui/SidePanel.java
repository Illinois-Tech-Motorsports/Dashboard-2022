package com.iit.dashboard2022.ui;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.iit.dashboard2022.R;
import com.iit.dashboard2022.dialog.JSONDialog;
import com.iit.dashboard2022.ecu.ECU;
import com.iit.dashboard2022.ecu.ECUCommands;
import com.iit.dashboard2022.ecu.ECUCommunication;
import com.iit.dashboard2022.page.CarDashboard;
import com.iit.dashboard2022.page.LiveData;
import com.iit.dashboard2022.ui.anim.TranslationAnim;
import com.iit.dashboard2022.ui.widget.SideButton;
import com.iit.dashboard2022.ui.widget.SideRadio;
import com.iit.dashboard2022.ui.widget.SideSwitch;
import com.iit.dashboard2022.ui.widget.SideToggle;
import com.iit.dashboard2022.ui.widget.console.ConsoleWidget;

public class SidePanel extends ConstraintLayout {
    public final RadioGroup consoleRadioGroup;
    public final SideRadio asciiRadio, hexRadio, rawRadio;
    public final SideSwitch uiTestSwitch, nearbyAPISwitch, reverseSwitch, consoleSwitch;
    public final SideToggle chargeToggle, JSONToggle, connToggle;
    public final SideButton clearConsoleButton, canMsgButton, canEchoButton, enableNearby;

    public TranslationAnim consoleAnim;
    public final TranslationAnim sidePanelDrawerAnim;
    public ECU.MODE lastChecked = ECU.MODE.ASCII;

    public SidePanel(Context context) {
        this(context, null);
    }

    public SidePanel(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SidePanel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View.inflate(getContext(), R.layout.side_panel_layout, this);

        consoleRadioGroup = findViewById(R.id.consoleRadioGroup);
        asciiRadio = findViewById(R.id.asciiRButton);
        hexRadio = findViewById(R.id.hexRButton);
        rawRadio = findViewById(R.id.rawRButton);

        chargeToggle = findViewById(R.id.chargeToggle);
        JSONToggle = findViewById(R.id.JSONToggle);
        connToggle = findViewById(R.id.connToggle);

        nearbyAPISwitch = findViewById(R.id.nearbyAPISwitch);
        uiTestSwitch = findViewById(R.id.uiTestSwitch);
        reverseSwitch = findViewById(R.id.reverseSwitch);
        consoleSwitch = findViewById(R.id.consoleSwitch);

        clearConsoleButton = findViewById(R.id.clearConsoleButton);
        canMsgButton = findViewById(R.id.canMsgButton);
        canEchoButton = findViewById(R.id.canEchoButton);
        enableNearby = findViewById(R.id.enableNearby);

        uiTestSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> UITester.enable(isChecked));

        sidePanelDrawerAnim = new TranslationAnim(this, TranslationAnim.X_AXIS, TranslationAnim.ANIM_BACKWARD);
        sidePanelDrawerAnim.startWhenReady();
    }

    public void attach(Activity activity, ConsoleWidget console, CarDashboard dashboard, LiveData liveDataPage, ECU frontECU) {
        consoleAnim = console.getAnimator();
        consoleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                consoleAnim.reverse();
                console.enable(true);
                frontECU.setInterpreterMode(lastChecked);
            } else {
                consoleAnim.start();
                console.enable(false);
                frontECU.setInterpreterMode(ECU.MODE.DISABLED);
            }
        });

        consoleRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.asciiRButton) {
                lastChecked = ECU.MODE.ASCII;
            } else if (checkedId == R.id.hexRButton) {
                lastChecked = ECU.MODE.HEX;
            } else if (checkedId == R.id.rawRButton) {
                lastChecked = ECU.MODE.RAW;
            }
            frontECU.setInterpreterMode(lastChecked);
            console.setMode(lastChecked.name());
        });
        asciiRadio.setChecked(true);

        clearConsoleButton.setOnClickListener(v -> console.clear());

        JSONDialog dialog = new JSONDialog(activity, frontECU);

        JSONToggle.setOnClickListener(v -> dialog.showDialog());
        JSONToggle.setHasToggleMediator(true);
        frontECU.addStatusListener((jsonLoaded, raw) -> JSONToggle.post(() -> JSONToggle.setChecked(jsonLoaded)));
        frontECU.setLogListener(console::post);
        frontECU.setErrorListener((tag, msg) -> {
            console.systemPost(tag, msg);
            console.newError();
        });

        ECUCommunication com = frontECU.getEcuCommunicator();

        enableNearby.setOnClickListener(v -> com.openNearby());
        nearbyAPISwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            com.changeMethod(isChecked ? ECUCommunication.NEARBY : ECUCommunication.USB);
            if (isChecked)
                com.openNearby();
        });

        frontECU.setConnectionListener(connected -> {
            console.systemPost(ECU.LOG_TAG, connected ? "Serial Connected" : "Serial Disconnected");
            if (!connected) {
                console.setStatus(ConsoleWidget.Status.Disconnected);
            }
        });

        frontECU.setConnectionStateListener(open -> {
            connToggle.post(() -> connToggle.setChecked(open));
            console.setStatus(open ? ConsoleWidget.Status.Connected : (frontECU.isAttached() ? ConsoleWidget.Status.Attached : ConsoleWidget.Status.Disconnected));
            if (!open) {
                dashboard.reset();
                liveDataPage.reset();
            }
        });

        connToggle.setOnClickListener(v -> {
            if (frontECU.isOpen()) {
                frontECU.close();
            } else {
                frontECU.open();
            }
        });
        connToggle.setHasToggleMediator(true);
        chargeToggle.setHasToggleMediator(true);
        reverseSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            frontECU.issueCommand(ECUCommands.TOGGLE_REVERSE); // TODO: Use discrete ON / OFF, instead of a toggle
        });
        chargeToggle.setOnClickListener(v -> frontECU.issueCommand(ECUCommands.CHARGE));
    }

    public void onLayoutChange() {
        consoleAnim.reloadAutoSize();
        sidePanelDrawerAnim.reloadAutoSize();
    }
}
