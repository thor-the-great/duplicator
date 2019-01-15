package duplicator;

import duplicator.serives.SynchronizerAPI;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class DLogger {
    static private Map<String, DLogger> instances = new WeakHashMap<>();
    private Logger logger;
    private final BlockingQueue<Message> queue;
    private final LoggerThread consumer;
    int CAPACITY = 100;

    private DLogger(String name, LOGGER_TYPE type) {
        if (type == LOGGER_TYPE.ANONYMOUS) {
            logger = Logger.getAnonymousLogger();
        } else if (type == LOGGER_TYPE.MAIN) {
            logger = Logger.getLogger(SynchronizerAPI.class.getName() + " Main events");
            FileHandler fileLogHandler = null;
            try {
                fileLogHandler = new FileHandler("./logs/duplicator.log", true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            fileLogHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileLogHandler);
        } else if (type == LOGGER_TYPE.ROLLING) {
            logger = Logger.getLogger(SynchronizerAPI.class.getName() + " Rolling operations");
            Date date = new Date();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd_hh_mm_ss");
            String strDate = dateFormat.format(date);
            FileHandler fileLogHandler = null;
            try {
                fileLogHandler = new FileHandler("./logs/duplicator_" + strDate + ".log");
            } catch (IOException e) {
                e.printStackTrace();
            }
            fileLogHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileLogHandler);
        }
        queue = new LinkedBlockingDeque(CAPACITY);
        consumer = new LoggerThread();
        consumer.start();
    }

    public static DLogger getInstance(String name, LOGGER_TYPE type) {
        if (!instances.containsKey(name + type.name())) {
            DLogger logger = new DLogger(name, type);
            instances.put(name, logger);
        }
        return instances.get(name);
    }

    public static DLogger getAnonymousLogger() {
        return getInstance("", LOGGER_TYPE.ANONYMOUS);
    }

    public void log(Level level, String msg) {
        try {
            queue.put(new Message(level, msg));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class LoggerThread extends Thread {
        public void run() {
            try {
                while (true) {
                    Message msg = queue.take();
                    logger.log(msg.level, msg.msg);
                }
            } catch (InterruptedException ignored) {

            }
        }
    }
}
class Message {
    Level level;
    String msg;

    Message(Level level, String msg) {
        this.level = level;
        this.msg = msg;
    }
}

