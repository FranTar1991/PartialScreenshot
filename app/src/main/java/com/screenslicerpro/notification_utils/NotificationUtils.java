package com.screenslicerpro.notification_utils;


import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_MUTABLE;
import static com.screenslicerpro.utils.UtilsKt.MY_INTENT_EXTRA;
import static com.screenslicerpro.utils.UtilsKt.MY_VIEW_ID;

import static org.chromium.base.ContextUtils.getApplicationContext;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.util.Pair;

import com.screenslicerpro.MainActivity;
import com.screenslicerpro.R;
import com.screenslicerpro.floatingCropWindow.CropViewFloatingWindowService;



public class NotificationUtils {

    public static final int N_ID_F_ScreenShot = 1337;
    public static final int N_ID_F_Floating = 1338;
    private static final String NOTIFICATION_CHANNEL_ID = "com.screenslicerpro";
    private static final String NOTIFICATION_CHANNEL_NAME ="com.screenslicerpro";

    private static int updateFlag;



    public static Pair<Integer, Notification> getNotification(@NonNull Context context, int id, int drawable) {
        createNotificationChannel(context);
        Notification notification = createNotification(context, drawable);
        NotificationManager notificationManager
                = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
        return new Pair<>(id, notification);
    }





    @TargetApi(Build.VERSION_CODES.O)
    private static void createNotificationChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    private static Notification createNotification(@NonNull Context context, int drawable) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            updateFlag = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;

        } else {
            updateFlag = PendingIntent.FLAG_UPDATE_CURRENT;

        }

        RemoteViews notificationLayout = new RemoteViews(context.getPackageName(),
                R.layout.custom_layout_notification);

        Intent cropWindowIntent = new Intent(context, CropViewFloatingWindowService.class);
        cropWindowIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);



        setHomeIntent(context, notificationLayout);
        setCloseIntent(context, notificationLayout);
        setGesturesIntent(context, notificationLayout, drawable, cropWindowIntent);
        setCropWindowIntent(context, notificationLayout, cropWindowIntent);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.recording))
                .setPriority(Notification.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setCustomContentView(notificationLayout)
                .setShowWhen(false);
        return builder.build();
    }

    private static void setHomeIntent(Context context, RemoteViews notificationLayout) {
        Intent homeIntent = new Intent(context, MainActivity.class);
        PendingIntent homePendingIntent = PendingIntent.getActivity(context, 0 ,
                homeIntent, FLAG_MUTABLE);
        notificationLayout.setOnClickPendingIntent(R.id.home_btn,homePendingIntent);
    }

    private static void setCloseIntent(Context context, RemoteViews notificationLayout) {
        Intent closeIntent = new Intent(context, NotificationBroadcastReceiver.class);
        PendingIntent closePendingIntent = PendingIntent.getBroadcast(context, 0 ,
                closeIntent, FLAG_MUTABLE);
        notificationLayout.setOnClickPendingIntent(R.id.close_btn,closePendingIntent);
    }

    private static void setGesturesIntent(Context context, RemoteViews notificationLayout,
                                          int drawable,
                                          Intent gesturesWindowIntent) {


        gesturesWindowIntent.putExtra(MY_INTENT_EXTRA,drawable);

        PendingIntent gesturePendingIntent = PendingIntent.getService(context,
                0,
                gesturesWindowIntent,
                updateFlag);
        notificationLayout.setOnClickPendingIntent(R.id.toggle_button, gesturePendingIntent);

        notificationLayout.setImageViewResource(R.id.toggle_button,drawable);


    }
    @SuppressLint("UnspecifiedImmutableFlag")
    private static void setCropWindowIntent(Context context,
                                            RemoteViews notificationLayout,
                                            Intent cropWindowIntent) {

        cropWindowIntent.putExtra(MY_INTENT_EXTRA,-1);

      PendingIntent cropWindowPendingIntent = PendingIntent.getService(context,
                0 ,
                cropWindowIntent,
              FLAG_IMMUTABLE);

        notificationLayout.setOnClickPendingIntent(R.id.crop_window_btn, cropWindowPendingIntent);


    }


}
