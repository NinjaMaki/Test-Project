package ninja.maki.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    public static void log(String str, Type type) {
        switch (type) {
            case SUCCESS: {
                System.out.println("\033[32;1m" + "[" + getTime() + "] " + str + "\033[0m");
                break;
            }
            case INFO: {
                System.out.println("\033[33;1m" + "[" + getTime() + "] " + str + "\033[0m");
                break;
            }
            case ERROR: {
                System.out.println("\033[31;1m" + "[" + getTime() + "] " + str + "\033[0m");
                break;
            }
        }
    }

    public static String getTime() {
        SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return time.format(new Date());
    }

    public enum Type {
        SUCCESS, INFO, ERROR
    }
}
