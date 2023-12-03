package com.artuok.appwork.library;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.text.format.DateFormat;

import com.artuok.appwork.R;
import com.artuok.appwork.fragmets.HomeFragment;
import com.artuok.appwork.objects.Item;

import java.util.Arrays;
import java.util.List;

public class Constants {

    public static int VERSION = 8;
    public static String VERSION_CODE = "1.1.3";

    public static String ID_PUB_AD = "ca-app-pub-5838551368289900/9523174443";
//"ca-app-pub-5838551368289900/1451662327"
    public static String parseText(String input) {
        String formattedString = input.trim();

        formattedString = formattedString.replaceAll("\\n{2,}", "\n");

        return formattedString;
    }

    public static int[] backgroundLines = {R.string.in_stealth_mode, R.string.master_of_multitasking, R.string.working_in_shadows, R.string.ninja_productivity, R.string.operating_in_secret, R.string.work_in_silence, R.string.invisible_work};
    public static int[] ICON_LIST = {R.drawable.ic_centaur_heart,
            R.drawable.ic_discussion,
            R.drawable.ic_eagle_emblem,
            R.drawable.ic_graduate_cap,
            R.drawable.ic_spanner,
            R.drawable.ic_light_bulb
            };
    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = connectivityManager.getActiveNetwork();
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);

        return networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
    public static String getDayOfWeek(Context context, int dd) {
        switch (dd) {
            case 1:
                return context.getString(R.string.sunday);
            case 2:
                return context.getString(R.string.monday);
            case 3:
                return context.getString(R.string.tuesday);
            case 4:
                return context.getString(R.string.wednesday);
            case 5:
                return context.getString(R.string.thursday);
            case 6:
                return context.getString(R.string.friday);
            case 7:
                return context.getString(R.string.saturday);
            default:
                return "";
        }
    }
    public static String getMinDayOfWeek(Context context, int dayOfWeek) {
        switch (dayOfWeek) {
            case 0:
                return context.getString(R.string.min_sunday);
            case 1:
                return context.getString(R.string.min_monday);
            case 2:
                return context.getString(R.string.min_tuesday);
            case 3:
                return context.getString(R.string.min_wednesday);
            case 4:
                return context.getString(R.string.min_thursday);
            case 5:
                return context.getString(R.string.min_friday);
            case 6:
                return context.getString(R.string.min_saturday);
        }
        return "";
    }
    public static String getMonthMinor(Context context, int MM) {
        switch (MM) {
            case 0:
                return context.getString(R.string.m_january);
            case 1:
                return context.getString(R.string.m_february);
            case 2:
                return context.getString(R.string.m_march);
            case 3:
                return context.getString(R.string.m_april);
            case 4:
                return context.getString(R.string.m_may);
            case 5:
                return context.getString(R.string.m_june);
            case 6:
                return context.getString(R.string.m_july);
            case 7:
                return context.getString(R.string.m_august);
            case 8:
                return context.getString(R.string.m_september);
            case 9:
                return context.getString(R.string.m_october);
            case 10:
                return context.getString(R.string.m_november);
            case 11:
                return context.getString(R.string.m_december);
            default:
                return "";
        }
    }
    public static String getDateString(Context context, long time){
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(time);
        int day = c.get(java.util.Calendar.DAY_OF_MONTH);
        int month = c.get(java.util.Calendar.MONTH);
        int year = c.get(java.util.Calendar.YEAR);
        String dd = day < 10 ? "0" + day : "" + day;
        return dd + " " + Constants.getMonthMinor(context, month) + " " + year + " ";
    }
    public static String getTimeString(Context context, long time){
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(time);
        boolean hourFormat = DateFormat.is24HourFormat(context);
        int hour = c.get(java.util.Calendar.HOUR_OF_DAY);
        if (!hourFormat)
            hour = c.get(java.util.Calendar.HOUR) == 0 ? 12 : c.get(java.util.Calendar.HOUR);
        int minute = c.get(java.util.Calendar.MINUTE);
        String mn = minute < 10? "0" + minute : "" + minute;
        String times = hour+":"+mn;
        if (!hourFormat) {
            times += c.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM ? " a. m." : " p. m.";
        }

        return times;
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;

        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
}
