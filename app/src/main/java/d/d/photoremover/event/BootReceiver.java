package d.d.photoremover.event;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import d.d.photoremover.schedule.service.ScheduleService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startForegroundService(new Intent(context, EventService.class));
        context.startForegroundService(new Intent(context, ScheduleService.class));
    }
}
