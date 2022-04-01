package com.iit.dashboard2022.ecu;

import android.util.Pair;
import android.view.Gravity;
import androidx.appcompat.app.AppCompatActivity;
import com.google.common.collect.Lists;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.iit.dashboard2022.logging.Log;
import com.iit.dashboard2022.logging.LogFile;
import com.iit.dashboard2022.logging.ToastLevel;
import com.iit.dashboard2022.util.ByteSplit;
import com.iit.dashboard2022.util.Constants;
import com.iit.dashboard2022.util.USBSerial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class ECU {
    private final USBSerial usbMethod;
    private final ECUMessageHandler ecuMessageHandler;
    private final ECUJUSB J_USB;
    private final List<Consumer<State>> stateListener = Collections.synchronizedList(Lists.newArrayList());
    private MODE interpreterMode = MODE.DISABLED;
    private int errorCount = 0;
    private State state = State.INITIALIZING;

    private final BlockingQueue<Pair<Long, byte[]>> payloadQueue = new LinkedBlockingQueue<>();

    public static ECU instance;

    private static final Logger logger = LoggerFactory.getLogger("ECU");

    public ECU(AppCompatActivity activity) {
        ECU.instance = this;
        J_USB = new ECUJUSB(this);
        ecuMessageHandler = new ECUMessageHandler();

        ecuMessageHandler.getStatistic(Constants.Statistics.State).addMessageListener(stat -> {
            Log.getLogger().error("Statty: " + stat.getAsInt());
            State state = State.getStateById(stat.getAsInt());
            if (state == null) {
                return;
            }
            this.state = state;
            stateListener.forEach(consumer -> consumer.accept(state));
        }, ECUStat.UpdateMethod.ON_VALUE_CHANGE);

        Thread ecuThread = new Thread(() -> {
            while (true) {
                try {
                    Pair<Long, byte[]> data = payloadQueue.take();
                    for (int i = 0; i < data.second.length; i += 8) {
                        byte[] data_block = new byte[8];
                        try {
                            System.arraycopy(data.second, i, data_block, 0, 8);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            logger.error("Received cutoff array.");
                            continue;
                        }
                        ECUPayload payload = new ECUPayload(data.first, data_block);
                        handlePayload(payload);
                    }
                } catch (InterruptedException e) {
                    Log.getLogger().warn("ECU Thread Interrupted", e);
                }
            }
        });
        ecuThread.setDaemon(true);
        ecuThread.setName("ECU-Thread");
        ecuThread.start();
        Runtime.getRuntime().addShutdownHook(new Thread(ecuThread::interrupt));

        // Start Serial
        usbMethod = new USBSerial(activity, 115200, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_2, UsbSerialPort.PARITY_NONE);
        usbMethod.setDataListener(this::receiveData);
        usbMethod.autoConnect(true);
        open();
    }

    private void receiveData(byte[] data) {
        if (J_USB.JUSB_requesting != 0 && J_USB.receive(data)) {
            return;
        }

        if (!ecuMessageHandler.loaded()) {
            if (errorCount == 0) {
                Log.toast("No JSON map Loaded", ToastLevel.WARNING, false, Gravity.START);
            }
            errorCount = ++errorCount % 8;
            return;
        }
        postPayload(data);
    }

    public void postPayload(byte[] data) {
        payloadQueue.add(new Pair<>(System.currentTimeMillis(), data));
    }

    public void issueCommand(Command command) {
        usbMethod.write(command.getData());
    }

    public void clear() {
        ecuMessageHandler.clear();
    }

    public void onStateChangeEvent(Consumer<State> consumer) {
        this.stateListener.add(consumer);
    }

    public void setInterpreterMode(MODE mode) {
        this.interpreterMode = mode;
    }

    /**
     * Log a message's raw data, if possible
     *
     * @param payload The payload from the ECU.
     */
    private void logRawData(ECUPayload payload) {
        LogFile activeLogFile = Log.getInstance().getActiveLogFile();
        if (activeLogFile != null) {
            activeLogFile.logBinaryStatistics((int) payload.getCallerId(), (int) payload.getValue());
        }
    }

    private void handlePayload(ECUPayload payload) {
        if (interpreterMode == MODE.HEX) {
            logger.debug(ByteSplit.bytesToHex(payload.getRawData()));
        } else if (interpreterMode == MODE.ASCII) {
            String message = ecuMessageHandler.getStr((int) payload.getStringId());
            int temp = (int) payload.getCallerId();
            if ((temp < 256 || temp > 4096) && message != null) {
                String comp = message.toLowerCase(Locale.ROOT);
                String val = " " + payload.getValue();
                if (comp.contains("[error]")) {
                    logger.error(message.replace("[ERROR]", "").trim() + val);
                } else if (comp.contains("[fatal]")) {
                    logger.error(message.replace("[FATAL]", "").trim() + val);
                } else if (comp.contains("[warn]")) {
                    logger.warn(message.replace("[WARN]", "").trim() + val);
                } else if (comp.contains("[debug]")) {
                    logger.debug(message.replace("[DEBUG]", "").trim() + val);
                } else {
                    logger.info(message.replace("[INFO]", "").replace("[ LOG ]", "").trim() + val);
                }
            }
        }
        ecuMessageHandler.updateStatistic((int) payload.getCallerId(), (int) payload.getStringId(), payload.getValue());
        logRawData(payload);
    }

    public ECUMessageHandler getMessageHandler() {
        return ecuMessageHandler;
    }

    public ECUJUSB getUsb() {
        return J_USB;
    }

    public void write(byte[] data) {
        usbMethod.write(data);
    }

    public void setConnectionListener(Consumer<Integer> statusListener) {
        usbMethod.setStatusListener(statusListener);
    }

    public boolean open() {
        return usbMethod.open();
    }

    public void close() {
        usbMethod.close();
    }

    public boolean isOpen() {
        return usbMethod.isOpen();
    }

    public boolean isAttached() {
        return usbMethod.isAttached();
    }

    /**
     * Gets current state of the vehicle
     *
     * @return the current state of the vehicle
     */
    public State getState() {
        return state;
    }

    public enum MODE {
        DISABLED,
        ASCII,
        HEX,
        RAW
    }

    public enum Command {
        CHARGE(123),
        SEND_CAN_BUS_MESSAGE(111),
        CLEAR_FAULT(45),
        TOGGLE_CAN_BUS_SNIFF(127),
        TOGGLE_MIRROR_MODE(90),
        ENTER_MIRROR_SET(-1),
        SEND_ECHO(84),
        TOGGLE_REVERSE(25),
        PRINT_LOOKUP(101),
        SET_SERIAL_VAR(61);

        private final byte[] data;

        Command(int id) {
            this.data = new byte[]{ Integer.valueOf(id).byteValue() };
        }

        public byte[] getData() {
            return data;
        }
    }

    public enum State { // Use actual name, brackets are added on when matching to actual state name
        INITIALIZING("Teensy Initialize"),
        PRE_CHARGE("PreCharge State"),
        IDLE("Idle State"),
        CHARGING("Charging State"),
        BUTTON("Button State"),
        DRIVING("Driving Mode State"),
        FAULT("Fault State");

        private final String name;
        private int id = -1;

        State(String title) {
            this.name = title;
        }

        public void setTagId(int id) {
            this.id = id;
        }

        public int getTagId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public static State getStateById(int id) {
            for (State state : State.values()) {
                if (state.getTagId() == id) {
                    return state;
                }
            }
            return null;
        }

        public static State getStateByName(String name) {
            for (State state : State.values()) {
                if (state.getName().equalsIgnoreCase(name)) {
                    return state;
                }
            }
            return null;
        }
    }
}
