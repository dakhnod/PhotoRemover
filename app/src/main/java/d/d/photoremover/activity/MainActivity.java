package d.d.photoremover.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLEngineResult;

import d.d.photoremover.R;
import d.d.photoremover.event.EventService;
import d.d.photoremover.schedule.ScheduledPhoto;
import d.d.photoremover.schedule.service.ScheduleService;

public class MainActivity extends AppCompatActivity implements MenuItem.OnMenuItemClickListener {
    private List<ScheduledPhoto> scheduledPhotos = new ArrayList<>();
    private ScheduledPhotoAdapter adapter;

    final String TAG = getClass().getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        startServices();

        loadScheduledPhotos();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        for(int i = 0; i < menu.size(); i++){
            menu.getItem(i).setOnMenuItemClickListener(this);
        }
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if(item.getItemId() == R.id.menu_item_settings){
            startActivity(new Intent(this, SettingsActivity.class));
        }
        return false;
    }

    private void initViews(){
        setSupportActionBar(findViewById(R.id.my_toolbar));

        ListView scheduledPhotosList = findViewById(R.id.list_scheduled_photos);
        adapter = new ScheduledPhotoAdapter(this, this.scheduledPhotos);
        scheduledPhotosList.setAdapter(adapter);
    }

    private void refreshList(){
        this.adapter.notifyDataSetChanged();
    }

    private void loadScheduledPhotos(){
        bindService(
                new Intent(this, ScheduleService.class),
                new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        Log.d(TAG, "onServiceConnected: service connected, loading photos");
                        ScheduleService.ScheduleServiceBinder binder = (ScheduleService.ScheduleServiceBinder) service;
                        MainActivity.this.scheduledPhotos.clear();
                        MainActivity.this.scheduledPhotos.addAll(binder.getScheduledPhotos());
                        MainActivity.this.unbindService(this);

                        MainActivity.this.refreshList();
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        Log.d(TAG, "onServiceDisconnected: service disconnected");
                    }
                },
                0
        );
    }

    private void startServices(){
        Intent eventServiceIntent = getIntent();
        eventServiceIntent.setClass(this, EventService.class);
        startForegroundService(eventServiceIntent);

        Intent scheduleServiceIntent = getIntent();
        scheduleServiceIntent.setClass(this, ScheduleService.class);
        startForegroundService(scheduleServiceIntent);
    }

    class ScheduledPhotoAdapter extends ArrayAdapter<ScheduledPhoto>{
        public ScheduledPhotoAdapter(Context context, List<ScheduledPhoto> scheduledPhotos) {
            super(context, R.layout.list_item_scheduled_photo, scheduledPhotos);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if(convertView == null){
                convertView = getLayoutInflater().inflate(R.layout.list_item_scheduled_photo, null);
            }
            TextView expiryDurationText = convertView.findViewById(R.id.list_item_scheduled_photo_expiry_duration);
            ImageView previewImage = convertView.findViewById(R.id.list_item_scheduled_photo_preview);

            ScheduledPhoto currentPhoto = getItem(position);

            try {
                // TODO: photo orientation, maybe save in schedule table
                InputStream previewInputStream = getContentResolver().openInputStream(Uri.parse(currentPhoto.getUri()));
                Bitmap previewBitmap = BitmapFactory.decodeStream(previewInputStream);
                previewInputStream.close();

                previewImage.setImageBitmap(previewBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ScheduledPhoto.State state = currentPhoto.getState();
            if(state.isError()){
                TextView issueView = convertView.findViewById(R.id.list_item_scheduled_photo_issue);
                issueView.setText(state.getStateDescriptionResource());
                issueView.setVisibility(View.VISIBLE);
            }else{
                long expiryDuration = currentPhoto.getExpiryDurationFromNow();
                // TODO: change to something human readable
                expiryDurationText.setText(String.valueOf(expiryDuration));
                expiryDurationText.setVisibility(View.VISIBLE);
            }

            return convertView;
        }
    }


}