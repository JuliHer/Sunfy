package com.artuok.appwork.library;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public class Constants {

    public static int VERSION = 3;
    public static String VERSION_CODE = "1.0.2";

    public static String parseText(String input) {
        String formattedString = input.trim();

        formattedString = formattedString.replaceAll("\\n{2,}", "\n");

        return formattedString;
    }

    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = connectivityManager.getActiveNetwork();
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);

        return networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
