package d.d.photoremover.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import d.d.photoremover.R;
import d.d.photoremover.date.DateFormatter;
import d.d.photoremover.event.EventService;
import d.d.photoremover.schedule.ScheduledPhoto;
import d.d.photoremover.schedule.service.ScheduleService;

public class MainActivity extends AppCompatActivity implements MenuItem.OnMenuItemClickListener {
    private List<ScheduledPhoto> scheduledPhotos = new ArrayList<>();
    private ScheduledPhotoAdapter adapter;
    private ScheduleService.ScheduleServiceBinder scheduleServiceBinder = null;

    final String TAG = getClass().getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        verifyStoragePermissions();

        startServices();

        connectTOSchedulerService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setOnMenuItemClickListener(this);
        }
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.menu_item_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(this.scheduleServiceBinder != null){
            this.refreshList();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unbindService(schedulerConnection);
    }

    private void initViews() {
        setSupportActionBar(findViewById(R.id.my_toolbar));

        ListView scheduledPhotosList = findViewById(R.id.list_scheduled_photos);
        adapter = new ScheduledPhotoAdapter(this, this.scheduledPhotos);
        scheduledPhotosList.setAdapter(adapter);
        // TODO add popup menu
        scheduledPhotosList.setOnItemClickListener((adapterView, view, itemPosition, l) -> {
            PopupMenu menu = new PopupMenu(MainActivity.this, view,0);
            menu.inflate(R.menu.photo_popup_menu);

            menu.setOnMenuItemClickListener(menuItem -> {
                if(menuItem.getItemId() == R.id.menu_item_save_from_deletion){
                    // TODO handle that
                }else if(menuItem.getItemId() == R.id.menu_item_delete_now){
                    // TODO: add delete code
                }
                return true;
            });

            menu.show();
        });
    }

    private void verifyStoragePermissions() {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    0
            );
        }
    }

    private void refreshList() {
        this.scheduledPhotos.clear();
        this.scheduledPhotos.addAll(this.scheduleServiceBinder.getScheduledPhotos());
        this.adapter.notifyDataSetChanged();
    }

    private ServiceConnection schedulerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected: service connected, loading photos");
            MainActivity.this.scheduleServiceBinder = (ScheduleService.ScheduleServiceBinder) service;
            MainActivity.this.refreshList();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected: service disconnected");
        }
    };

    private void connectTOSchedulerService() {
        bindService(
                new Intent(this, ScheduleService.class),
                schedulerConnection,
                0
        );
    }

    private void startServices() {
        Intent eventServiceIntent = getIntent();
        eventServiceIntent.setClass(this, EventService.class);
        startForegroundService(eventServiceIntent);

        Intent scheduleServiceIntent = getIntent();
        scheduleServiceIntent.setClass(this, ScheduleService.class);
        startForegroundService(scheduleServiceIntent);
    }

    class ScheduledPhotoAdapter extends ArrayAdapter<ScheduledPhoto> {
        private DateFormatter dateFormatter;

        public ScheduledPhotoAdapter(Context context, List<ScheduledPhoto> scheduledPhotos) {
            super(context, R.layout.list_item_scheduled_photo, scheduledPhotos);
            this.dateFormatter = new DateFormatter(getContext());
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_item_scheduled_photo, null);
            }
            TextView expiryDurationText = convertView.findViewById(R.id.list_item_scheduled_photo_expiry_duration);
            ImageView previewImage = convertView.findViewById(R.id.list_item_scheduled_photo_preview);

            ScheduledPhoto currentPhoto = getItem(position);

            try {
                InputStream previewInputStream = getContentResolver().openInputStream(Uri.parse(currentPhoto.getUri()));
                Bitmap previewBitmap = BitmapFactory.decodeStream(previewInputStream);
                previewInputStream.close();

                Matrix rotationMatrix = new Matrix();
                rotationMatrix.postRotate(currentPhoto.getMetaData().getOrientation(), 0, 0);

                previewImage.setImageBitmap(
                        Bitmap.createBitmap(
                                previewBitmap,
                                0,
                                0,
                                previewBitmap.getWidth(),
                                previewBitmap.getHeight(),
                                rotationMatrix,
                                false
                                )
                );
            } catch (IOException | SecurityException e) {
                e.printStackTrace();
            }

            ScheduledPhoto.State state = currentPhoto.getState();
            if (state.isError()) {
                TextView issueView = convertView.findViewById(R.id.list_item_scheduled_photo_issue);
                issueView.setText(state.getStateDescriptionResource());
                issueView.setVisibility(View.VISIBLE);
            } else {
                long expiryDuration = currentPhoto.getExpiryDurationFromNow();
                expiryDurationText.setText(this.dateFormatter.formatRemainingTime(expiryDuration));
                expiryDurationText.setVisibility(View.VISIBLE);
            }

            return convertView;
        }
    }


}