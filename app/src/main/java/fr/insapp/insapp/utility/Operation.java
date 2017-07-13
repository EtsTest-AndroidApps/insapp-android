package fr.insapp.insapp.utility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Antoine on 24/09/2016.
 */

public class Operation {

    public static Date stringToDate(String format, String dateInString, boolean timeZoneUTC){
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+0:00"));

        Date date = null;
        try {
            date = formatter.parse(dateInString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static String displayedDate(Date date){
        Date atm = Calendar.getInstance().getTime();

        final long diff = atm.getTime() - date.getTime();
        final long diffMinutes = diff / (60 * 1000) % 60;
        final long diffHours = diff / (60 * 60 * 1000);
        final int diffInDays = (int) ((diff) / (1000 * 60 * 60 * 24));

        if (diffInDays >= 7) {
            // at least 1 week
            return Integer.toString(diffInDays / 7) + "sem";
        }

        if (diffInDays >= 1) {
            // at least 1 day
            return Long.toString(diffInDays) + "j";
        }


        if (diffHours >= 1) {
            // at least 1 hour
            return Long.toString(diffHours) + "h";
        }

        return Long.toString(diffMinutes) + "m";
    }

    public static String drawableProfileName(String promo, String gender){
        String drawable_string = "avatar";

        if (!promo.equals("") && !gender.equals("")) {
            String userPromotion = promo.toLowerCase();

            if (userPromotion.contains("personnel")) {
                userPromotion = "worker";
            }
            else if (userPromotion.contains("alternant")) {
                userPromotion = "apprentice";
            }
            else if (!userPromotion.contains("stpi") && Character.isDigit(userPromotion.charAt(0))) {
                userPromotion = userPromotion.substring(1);
            }

            drawable_string += "_" + userPromotion;
            drawable_string += "_" + gender;
        }
        else {
            drawable_string = "avatar_default";
        }

        return drawable_string;
    }
}
