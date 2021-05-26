package d.d.photoremover.date;

import android.content.Context;

import d.d.photoremover.R;

public class DateFormatter {
    private Context context;

    public DateFormatter(Context context) {
        this.context = context;
    }

    public String formatRemainingTime(long remainingMillis){
        long seconds = (remainingMillis / 1000);
        long remainingDays = seconds / 86400;
        long remainingHours = seconds / 3600 % 24;
        long remainingMinutes = seconds / 60 % 60;
        long remainingSeconds = seconds % 60;

        if(remainingDays >= 2){
            return context.getString(R.string.date_format_days, remainingDays);
        }
        
        if(remainingDays >= 1){
            return context.getString(R.string.date_format_days_hours, remainingDays, remainingHours);
        }
        
        if(remainingHours >= 10){
            return context.getString(R.string.date_format_hours, remainingHours);
        }
        
        if(remainingHours >= 1){
            return context.getString(R.string.date_format_hours_minutes, remainingHours, remainingMinutes);
        }
        
        if(remainingMinutes >= 1){
            return context.getString(R.string.date_format_minutes, remainingMinutes);
        }
        
        return context.getString(R.string.date_format_seconds, remainingSeconds);
    }
}
