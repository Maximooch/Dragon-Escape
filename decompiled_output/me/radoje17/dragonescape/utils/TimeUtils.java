package me.radoje17.dragonescape.utils;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class TimeUtils {
   public static String formatTime(long time) {
      time = Math.max(0L, time);
      long millis = time % 1000L;
      time /= 1000L;
      long hours = time / 3600L;
      time %= 3600L;
      long minutes = time / 60L;
      time %= 60L;
      String result = "";
      if (hours != 0L) {
         result = hours + ":";
      }

      DecimalFormat format = new DecimalFormat("00");
      DecimalFormat format1 = new DecimalFormat("000");
      return result + format.format(minutes) + ":" + format.format(time) + ":" + format1.format(millis);
   }

   public static String formatDateTime(long time) {
      return new SimpleDateFormat("dd.MM.yyyy. hh:mm").format(Long.valueOf(time));
   }
}
