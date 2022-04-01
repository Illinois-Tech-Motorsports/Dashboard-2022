package com.iit.dashboard2022.logging;

import android.os.Build;
import com.iit.dashboard2022.util.Constants;
import com.iit.dashboard2022.util.HawkUtil;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;

public class LogFile implements Closeable {

    private final long date;
    private final File dir;
    private final File logFile;
    private final File statsFile;
    private final File statsMapFile;

    private FileOutputStream outputStream = null;
    private FileOutputStream binaryStream = null;

    public LogFile(Map<String, String> statsMap) {
        this(System.currentTimeMillis() / 1000, statsMap);
    }

    public LogFile(long date) {
        this(date, null);
    }

    public LogFile(long date, Map<String, String> statsMap) {
        this.date = date;
        dir = new File(HawkUtil.getLogFilesDir(), String.valueOf(date));
        dir.mkdirs();
        logFile = new File(dir, "log.txt");
        statsFile = new File(dir, "log.stats");
        statsMapFile = new File(dir, "log.map.stats");
        if (statsMap != null) {
            try {
                FileWriter writer = new FileWriter(statsMapFile);
                Constants.GSON.toJson(statsMap, writer);
                writer.close();
                statsFile.createNewFile();
            } catch (IOException e) {
                Log.getLogger().error("Failed to write statistics map file for: " + getFileSize(), e);
            }
        }
    }

    public boolean delete() {
        logFile.delete();
        statsFile.delete();
        statsMapFile.delete();
        Log.getInstance().getLogs().remove(date);
        return dir.delete();
    }

    public String getDate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime time = Instant.ofEpochSecond(date).atZone(ZoneId.systemDefault()).toLocalDateTime();
            return time.format(Constants.DATE_FORMAT);
        }
        return null;
    }

    public String getFormattedName() {
        return getDate() + " - " + getFileSize();
    }

    public String getFileSize() {
        long totalBytes = logFile.length() + statsFile.length() + statsMapFile.length();
        return HawkUtil.humanReadableBytes(totalBytes);
    }

    public long getEpochSeconds() {
        return date;
    }

    public void toLog(String message) {
        if (outputStream == null) {
            try {
                if (!logFile.exists()) {
                    logFile.createNewFile();
                }
                outputStream = new FileOutputStream(logFile);
            } catch (IOException e) {
                Log.getLogger().error("Failed to create log file or open output stream", e);
            }
        }
        try {
            outputStream.write(message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.getLogger().error("Failed to write data to log file", e);
        }
    }

    public void logBinaryStatistics(int id, int data) {
        String out = String.format(Locale.ENGLISH, "%d %d %d\n", System.currentTimeMillis(), id, data);
        if (binaryStream == null) {
            try {
                if (!statsFile.exists()) {
                    statsFile.createNewFile();
                }
                binaryStream = new FileOutputStream(statsFile);
            } catch (IOException e) {
                Log.getLogger().error("Failed to create statistics file or open output stream", e);
            }
        }
        try {
            binaryStream.write(out.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.getLogger().error("Failed to write data to statistics file", e);
        }
    }

    @Override
    public void close() {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                Log.getLogger().error("Failed to close log file output stream", e);
            }
        }
        if (binaryStream != null) {
            try {
                binaryStream.close();
            } catch (IOException e) {
                Log.getLogger().error("Failed to close statistics file output stream", e);
            }
        }
    }

    public File getLogFile() {
        return logFile;
    }

    public File getStatsFile() {
        return statsFile;
    }

    public File getStatsMapFile() {
        return statsMapFile;
    }
}