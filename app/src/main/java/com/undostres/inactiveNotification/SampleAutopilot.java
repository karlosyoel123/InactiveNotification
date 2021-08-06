package com.undostres.inactiveNotification;

import android.content.Context;

import androidx.annotation.NonNull;

import com.urbanairship.AirshipConfigOptions;
import com.urbanairship.Autopilot;
import com.urbanairship.UAirship;

public class SampleAutopilot extends Autopilot {

    @Override
    public void onAirshipReady(@NonNull UAirship airship) {
        airship.getPushManager().setUserNotificationsEnabled(true);

        // Additional Airship SDK setup
    }

    @Override
    public AirshipConfigOptions createAirshipConfigOptions(@NonNull Context context) {
        AirshipConfigOptions options = new AirshipConfigOptions.Builder()
                .setDevelopmentAppKey("vcMNMmSTRRCJf4zeVWy0yA")
                .setDevelopmentAppSecret("CEwUmIykR3iNXFy55NgplA")
                .setProductionAppKey("vcMNMmSTRRCJf4zeVWy0yA")
                .setProductionAppSecret("CEwUmIykR3iNXFy55NgplA")
                //.setInProduction(!BuildConfig.DEBUG)
                //.setNotificationIcon(R.drawable.ic_notification)
                //.setNotificationAccentColor(ContextCompat(getContext(), R.color.accent))
                .setNotificationChannel("customChannel")
                // Uncomment this if your app uses Airship's EU cloud site
                //.setSite(AirshipConfigOptions.SITE_EU)
                .build();

        return options;
    }
}
