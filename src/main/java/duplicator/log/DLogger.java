package duplicator.log;

import duplicator.serives.SynchronizerAPI;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class DLogger {
    static private Map<String, DLogger> instances = new ConcurrentHashMap<>();
    private ExecutorService exec;
    private Logger logger;
    private final BlockingQueue<Message> queue;
    //private final LoggerThread consumer;
    int CAPACITY = 5000;

    private DLogger(String name, LOGGER_TYPE type) {
        int NUM_OF_EXECUTORS = 5;
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
            NUM_OF_EXECUTORS = 20;
        } else if (type == LOGGER_TYPE.ROLLING) {
            logger = Logger.getLogger(name);
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
            NUM_OF_EXECUTORS = 20;
        }
        queue = new LinkedBlockingDeque(CAPACITY);
        exec = Executors.newFixedThreadPool(NUM_OF_EXECUTORS);
        exec.submit(()-> {
            try {
                while (true) {
                    Message msg = queue.take();
                    if (msg == Message._SHUTDOWN)
                        break;
                    logger.log(msg.level, msg.msg);
                }
            } catch (InterruptedException ignored) {
                System.err.println("interrupted");
                Thread.currentThread().interrupt();
            }
        });
    }

    public static DLogger getInstance(String name, LOGGER_TYPE type) {
        String logName = name + type.name();
        if (!instances.containsKey(logName)) {
            DLogger logger = new DLogger(name, type);
            instances.put(logName, logger);
        }
        return instances.get(logName);
    }

    public static DLogger getAnonymousLogger() {
        String logName = "A_" + LOGGER_TYPE.ANONYMOUS.name();
        return getInstance(logName, LOGGER_TYPE.ANONYMOUS);
    }

    public void log(Level level, String msg) {
        exec.execute(()-> {
            try {
                queue.put(new Message(level, msg));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public static void shutdown() {
        System.err.println("Shuting down logger");
        for(String key : instances.keySet()) {
            DLogger logger = instances.get(key);
            try {
                logger.queue.put(Message._SHUTDOWN);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.exec.shutdown();
            /*try {
                logger.exec.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
        }
    }
}
class Message {
    Level level;
    String msg;

    static Message _SHUTDOWN = new Message(Level.ALL, "SHUTDOWN");

    Message(Level level, String msg) {
        this.level = level;
        this.msg = msg;
    }
}

