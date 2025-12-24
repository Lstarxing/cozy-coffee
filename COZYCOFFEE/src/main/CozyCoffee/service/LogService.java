package service;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogService {
    private static final String LOG_FILE = "cozy_coffee.log";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void log(String level, String action, String message) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            String logEntry = String.format("[%s] [%s] %s - %s",
                    dateFormat.format(new Date()),
                    level,
                    action,
                    message);
            writer.println(logEntry);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}