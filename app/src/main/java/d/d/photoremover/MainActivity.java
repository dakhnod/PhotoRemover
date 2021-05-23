package d.d.photoremover;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import d.d.photoremover.event.EventService;
import d.d.photoremover.schedule.service.ScheduleService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startServices();
    }

    private void startServices(){
        Intent eventServiceIntent = getIntent();
        eventServiceIntent.setClass(this, EventService.class);
        startForegroundService(eventServiceIntent);

        Intent scheduleServiceIntent = getIntent();
        scheduleServiceIntent.setClass(this, ScheduleService.class);
        startForegroundService(scheduleServiceIntent);
    }
}