package d.d.photoremover.schedule.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import d.d.photoremover.R;
import d.d.photoremover.event.EventService;
import d.d.photoremover.schedule.ScheduledPhoto;
import d.d.photoremover.schedule.database.ScheduleDatabaseHelper;

public class ScheduleService extends Service {
    private final String SERVICE_NOTIFICATION_ID = "Schedule service";

    private ArrayList<ScheduledPhoto> scheduledPhotos;

    private NotificationManager notificationManager;
    private ScheduleDatabaseHelper scheduleHelper;

    private Handler handler;

    private static final String TAG = "ScheduleService";


    @Override
    public void onCreate() {
        super.onCreate();

        this.handler = new Handler();

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

        List<ScheduledPhoto> scheduledPhotos = scheduleHelper.getScheduledPhotos();

        this.scheduledPhotos = new ArrayList<>(scheduledPhotos.size());
        this.scheduledPhotos.addAll(scheduledPhotos);

        this.parseScheduledList();
    }

    private void insertScheduledPhoto(ScheduledPhoto photo){
        for(int i = 0; i < this.scheduledPhotos.size(); i++){
            ScheduledPhoto compared = this.scheduledPhotos.get(i);
            if(compared.getExpiryDate() > photo.getExpiryDate()){
                this.scheduledPhotos.add(i, photo);
                return;
            }
        }

        this.scheduledPhotos.add(photo);
    }

    private void parseScheduledList(){
        cancelTimer();

        ArrayList<ScheduledPhoto> markedForDelete = new ArrayList<>();

        long now = System.currentTimeMillis();
        long nextSchedule = -1;

        for(int i = 0; i < this.scheduledPhotos.size(); i++){
            ScheduledPhoto compared = this.scheduledPhotos.get(i);

            if(compared.getState().isError()) continue;

            if(compared.getExpiryDate() <= now){
                markedForDelete.add(compared);
            }else{
                nextSchedule = compared.getExpiryDate();
                break;
            }
        }

        for(ScheduledPhoto marked : markedForDelete){
            // TODO: fix post deletion
            this.deleteScheduled(marked);
        }

        if(nextSchedule != -1){
            this.startTimer(nextSchedule);
        }
    }

    Runnable timerRunnable = this::parseScheduledList;

    private void cancelTimer(){
        this.handler.removeCallbacks(timerRunnable);
    }

    private void startTimer(long expiryDate){
        long delay = expiryDate - System.currentTimeMillis();

        Log.d(TAG, "startTimer: starting timer for " + delay);

        this.handler.postDelayed(timerRunnable, delay);
    }

    private void deleteScheduled(ScheduledPhoto photoToDelete){
        String filePath = photoToDelete.getFilePath();

        File file = new File(filePath);

        if(!file.exists()){
            photoToDelete.setState(ScheduledPhoto.State.ERROR_FILE_NOT_FOUND);
            return;
        }

        if(!file.canWrite()){
            photoToDelete.setState(ScheduledPhoto.State.ERROR_FILE_NO_ACCESS);
            return;
        }

        // TODO: delete file properly
        boolean deletionSuccess = file.delete();

        if(!deletionSuccess){
            photoToDelete.setState(ScheduledPhoto.State.ERROR_FILE_DELETE_FAILED);
            return;
        }

        Log.d(getClass().getName(), "deleting photo " + filePath);

        scheduledPhotos.remove(photoToDelete);
        getContentResolver().notifyChange(Uri.parse(photoToDelete.getUri()), null, ContentResolver.NOTIFY_SYNC_TO_NETWORK);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null) return START_STICKY;

        if(EventService.ACTION_SCHEDULE_PHOTO_EXPIRY.equals(intent.getAction())){
            ScheduledPhoto scheduledPhoto = (ScheduledPhoto) intent.getSerializableExtra("SCHEDULED_PHOTO");

            this.scheduleHelper.schedulePhotoExpiry(scheduledPhoto);

            this.insertScheduledPhoto(scheduledPhoto);

            this.parseScheduledList();
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
        return new ScheduleServiceBinder();
    }

    public class ScheduleServiceBinder extends Binder{
        public List<ScheduledPhoto> getScheduledPhotos(){
            return ScheduleService.this.scheduledPhotos;
        }
    }
}