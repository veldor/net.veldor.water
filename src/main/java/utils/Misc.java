package utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Misc {
    public static String getCurrentTime(){
        return new SimpleDateFormat("HH:mm:ss ").format(new Date());
    }
}
