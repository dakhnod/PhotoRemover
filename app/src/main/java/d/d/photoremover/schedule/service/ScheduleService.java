package d.d.photoremover.schedule.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import d.d.photoremover.R;
import d.d.photoremover.event.EventService;
import d.d.photoremover.schedule.ScheduledPhoto;
import d.d.photoremover.schedule.database.ScheduleDatabaseHelper;

public class ScheduleService extends Service {
    private final String SERVICE_NOTIFICATION_ID = "Schedule service";


    private NotificationManager notificationManager;
    private ScheduleDatabaseHelper scheduleHelper;


    @Override
    public void onCreate() {
        super.onCreate();

        this.notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        this.registerNotificationChannels();
        startForeground(2,
                new Notification.Builder(this, SERVICE_NOTIFICATION_ID)
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentTitle(getString(R.string.service_schedule_title))
                        .setContentText(getString(R.string.service_schedule_description))
                        .setOngoing(true)
                        .build()
        );

        this.scheduleHelper = new ScheduleDatabaseHelper(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(EventService.ACTION_SCHEDULE_PHOTO_EXPIRY.equals(intent.getAction())){
            ScheduledPhoto scheduledPhoto = (ScheduledPhoto) intent.getSerializableExtra("SCHEDULED_PHOTO");

            this.scheduleHelper.schedulePhotoExpiry(scheduledPhoto);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.scheduleHelper.close();
    }

    private void registerNotificationChannels() {
        NotificationChannel serviceChannel = new NotificationChannel(SERVICE_NOTIFICATION_ID, getString(R.string.service_schedule_channel_id), NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(serviceChannel);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}