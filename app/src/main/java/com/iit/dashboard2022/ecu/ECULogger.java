package com.iit.dashboard2022.ecu;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.iit.dashboard2022.util.LogFileIO;
import com.iit.dashboard2022.util.Toaster;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ECULogger {
    private static final String LOG_MAP_START = "---[ LOG MAP START ]---\n";
    private static final String LOG_MAP_END = "---[ LOG MAP END ]---\n";
    final LogFileIO logFile;

    public ECULogger(Activity activity) {
        this.logFile = new LogFileIO(activity);
    }

    @WorkerThread
    public static String stringifyLogFile(File file) {
        byte[] bytes = LogFileIO.getBytes(file);
        String jsonStr = LogFileIO.getString(file, LOG_MAP_END);
        int logStart = jsonStr.getBytes().length;
        StringBuilder stringFnl = new StringBuilder();
        stringFnl.append(jsonStr);

        for (int i = logStart; i < file.length(); i += 16) {
            byte[] epochB = new byte[8];
            byte[] msg = new byte[8];
            try {
                System.arraycopy(bytes, i, epochB, 0, 8);
                System.arraycopy(bytes, i + 8, msg, 0, 8);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
                Toaster.showToast("Warning: log file has leftover bytes", Toaster.WARNING);
                break;
            }
            long epoch = ByteBuffer.wrap(epochB).order(ByteOrder.LITTLE_ENDIAN).getLong();
            long[] IDs = ECU.interpretMsg(msg);
            stringFnl.append(epoch).append(" ").append(IDs[0]).append(" ").append(IDs[1]).append(" ").append(IDs[2]).append("\n");
        }

        String fnl = stringFnl.toString();
        fnl = fnl.replace("\"", "\\\"");
        fnl = fnl.replace("\n", "\\n");

        if (fnl.length() != 0) {
            return fnl;
        }

        Toaster.showToast("Returning string interpretation", Toaster.WARNING);
        return LogFileIO.getString(file);
    }

    @WorkerThread
    public static String interpretRawData(String jsonStr, byte[] raw_data, int dataStart) {
        ECUKeyMap localEcuKeyMap = new ECUKeyMap(jsonStr);
        StringBuilder output = new StringBuilder(raw_data.length);

        if (localEcuKeyMap.loaded()) {
            for (int i = dataStart; i < raw_data.length; i += 8) {
                byte[] data_block = new byte[8];
                try {
                    System.arraycopy(raw_data, i, data_block, 0, 8);
                } catch (ArrayIndexOutOfBoundsException e) {
                    continue;
                }

                long[] IDs = ECU.interpretMsg(data_block);
                output.append(ECU.formatMsg(0, localEcuKeyMap.getTag((int) IDs[0]), localEcuKeyMap.getStr((int) IDs[1]), IDs[2]));
            }
        }

        return output.toString();
    }

    @WorkerThread
    public static String interpretLogFile(File file) {
        byte[] bytes = LogFileIO.getBytes(file);
        String jsonStr = LogFileIO.getString(file, LOG_MAP_END);
        int logStart = jsonStr.getBytes().length;
        String output = interpretRawData(jsonStr.substring(LOG_MAP_START.length()), bytes, logStart);
        if (output.length() == 0) {
            Toaster.showToast("Returning string interpretation", Toaster.WARNING);
            return LogFileIO.getString(file);
        }
        return output;
    }

    public File getActiveFile() {
        return logFile.getActiveFile();
    }

    public void newLog(@NonNull String rawJson) {
        logFile.newLog();
        logFile.write(LOG_MAP_START.getBytes());
        logFile.write(rawJson.getBytes());
        logFile.write("\n".getBytes());
        logFile.write(LOG_MAP_END.getBytes());
    }

    public void write(byte[] bytes) {
        logFile.write(bytes);
    }
}
