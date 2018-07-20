package util;

public class Util {

    static private boolean isDist = false;

    public static void log(String log) {
        if(!isDist) System.out.println(log);
    }
}
