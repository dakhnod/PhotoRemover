package d.d.photoremover.event;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import d.d.photoremover.R;
import d.d.photoremover.schedule.ScheduledPhoto;
import d.d.photoremover.schedule.service.ScheduleService;

public class EventService extends Service {
    private int lastImageID;
    private NotificationManager notificationManager;

    private final String SERVICE_NOTIFICATION_ID = "event_service";
    private final String PHOTO_NOTIFICATION_ID = "photo_notification";

    public final static String ACTION_SCHEDULE_PHOTO_EXPIRY = "action_schedule_photo_expiry";

    private int NOTIFICATION_TIMEOUT_MS = 2 * 60 * 1000; // 2 minutes
    private int PHOTO_EXPIRY_DURATION = 10 * 1000; // 10 secs

    private final String TAG = getClass().getName();

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(getClass().getName(), "onCreate()");

        registerContentObserver();

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        registerNotificationChannels();
        startForeground(1,
                new Notification.Builder(this, SERVICE_NOTIFICATION_ID)
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentTitle(getString(R.string.service_event_title))
                        .setContentText(getString(R.string.service_event_description))
                        .setOngoing(true)
                        .build()
        );
    }


    private void registerNotificationChannels() {
        NotificationChannel serviceChannel = new NotificationChannel(SERVICE_NOTIFICATION_ID, getString(R.string.service_event_notification_channel_name), NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(serviceChannel);


        NotificationChannel photoNotificationChannel = new NotificationChannel(PHOTO_NOTIFICATION_ID, getString(R.string.new_photo_notification), NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(photoNotificationChannel);
    }

    private void registerContentObserver() {
        getContentResolver()
                .registerContentObserver(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        true,
                        imageObserver
                );
        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{MediaColumns._ID}, null, null, "date_added DESC");
        if (cursor != null && cursor.moveToFirst()) {
            lastImageID = cursor.getInt(cursor.getColumnIndex(MediaColumns._ID));
            cursor.close();
        }
        Log.d(getClass().getName(), "last image ID: " + lastImageID);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(getClass().getName(), "onStartCommand()");

        if (Intent.ACTION_SEND.equals(intent.getAction()) || Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            handleFileSend(intent);
        } else if(ACTION_SCHEDULE_PHOTO_EXPIRY.equals(intent.getAction())){
            int notificationId = intent.getIntExtra("NOTIFICATION_ID", -1);
            Log.d(TAG, "notif id: " + notificationId);
            if(notificationId != -1){
                notificationManager.cancel(notificationId);
                Toast.makeText(this, getString(R.string.message_deletion_scheduled), Toast.LENGTH_SHORT).show();
            }

            ScheduledPhoto scheduledPhoto = (ScheduledPhoto) intent.getSerializableExtra("SCHEDULED_PHOTO");
            Log.d(TAG, "uri: " + scheduledPhoto.getUri());
            scheduledPhoto.setExpiryDurationFromNow(this.PHOTO_EXPIRY_DURATION);

            intent.setClass(this, ScheduleService.class);
            startService(intent);
        }

        return START_STICKY;
    }

    private void handleFileSend(Intent intent) {
        startService(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(getClass().getName(), "onDestroy()");

        getContentResolver().unregisterContentObserver(imageObserver);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    ContentObserver imageObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);

            handleUri(uri);
        }
    };

    private void notifyNewImage(PhotoDTO photo) {
        int notificationId = new Random().nextInt(999999999);

        Log.d(TAG, "notificationId: " + notificationId);

        Intent serviceIntent = new Intent(this, getClass());
        serviceIntent.setAction(ACTION_SCHEDULE_PHOTO_EXPIRY);
        serviceIntent.putExtra("NOTIFICATION_ID", notificationId);
        serviceIntent.putExtra("SCHEDULED_PHOTO", new ScheduledPhoto(
                photo.getUri(),
                photo.getFilePath(),
                ScheduledPhoto.State.SCHEDULED
        ));

        Log.d(TAG, "uri: "+ photo.getUri());

        PendingIntent deleteIntent = PendingIntent.getService(
                this,
                0,
                serviceIntent,
                0
        );

        Notification.Action[] deleteActions = new Notification.Action[]{
                new Notification.Action
                        .Builder(Icon.createWithResource(this, R.mipmap.ic_launcher), "in 2 weeks", deleteIntent)
                        .build()
        };

        Notification photoNotification =
                new Notification.Builder(this, PHOTO_NOTIFICATION_ID)
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentTitle("New photo!")
                        .setContentText("delete this photo?")
                        .setOngoing(false)
                        .setTimeoutAfter(NOTIFICATION_TIMEOUT_MS)
                        .setUsesChronometer(true)
                        .setChronometerCountDown(true)
                        .setWhen(System.currentTimeMillis() + NOTIFICATION_TIMEOUT_MS)
                        .setActions(deleteActions)
                        .setLargeIcon(photo.getImageBitmap())
                        .setStyle(
                                new Notification.BigPictureStyle()
                                        .bigPicture(photo.getImageBitmap())
                        )
                        .build();

        notificationManager.notify(notificationId, photoNotification);
    }

    private void handleUri(Uri changeUri) {
        Cursor cursor;
        try {
            cursor = getContentResolver().query(changeUri, new String[]{"is_pending", MediaColumns._ID, "orientation", "relative_path", "_display_name"}, MediaColumns._ID + " > ?", new String[]{String.valueOf(lastImageID)}, "date_added");
            // cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, MediaColumns._ID + " > ?", new String[]{String.valueOf(lastImageID)}, "date_added");
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return;
        }
        if (cursor == null) {
            return;
        }
        if (cursor.getCount() == 0) {
            cursor.close();
            return;
        }

        PhotoDTO[] files = new PhotoDTO[cursor.getCount()];

        int index = 0;
        while (cursor.moveToNext()) {
            boolean pending = cursor.getInt(cursor.getColumnIndex("is_pending")) == 1;

            Log.d(getClass().getName(), "file is pending: " + pending);
            if (pending) {
                cursor.close();
                return;
            }
            int id = cursor.getInt(cursor.getColumnIndex(MediaColumns._ID));
            if (id > lastImageID) {
                lastImageID = id;
            }
            String uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI + "/" + id;
            String filePath = new File(
                    cursor.getString(
                            cursor.getColumnIndex("relative_path")
                    ),
                    cursor.getString(
                            cursor.getColumnIndex("_display_name")
                    )
            ).getAbsolutePath();
            filePath = new File(
                    Environment.getExternalStorageDirectory(),
                    filePath
            ).getAbsolutePath();

            Bitmap preview = null;

            try {
                InputStream stream = getContentResolver().openInputStream(Uri.parse(uri));
                preview = BitmapFactory.decodeStream(stream);
                stream.close();

                int orientation = cursor.getInt(cursor.getColumnIndex("orientation"));
                Matrix matrix = new Matrix();
                matrix.postRotate(orientation);
                preview = Bitmap.createBitmap(preview, 0, 0, preview.getWidth(), preview.getHeight(), matrix, true);
            } catch (IOException e) {
                e.printStackTrace();
            }

            files[index++] = new PhotoDTO(
                    uri,
                    filePath,
                    preview
            );
        }
        cursor.close();

        for (PhotoDTO photoUri : files) {
            notifyNewImage(photoUri);
        }
    }

    private class PhotoDTO {
        private final String uri;
        private final String filePath;
        private final Bitmap imageBitmap;

        public PhotoDTO(String uri, String filePath, Bitmap imageBitmap) {
            this.uri = uri;
            this.filePath = filePath;
            this.imageBitmap = imageBitmap;
        }

        public String getUri() {
            return uri;
        }

        public String getFilePath() {
            return filePath;
        }

        public Bitmap getImageBitmap() {
            return imageBitmap;
        }
    }

}