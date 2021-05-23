package d.d.photoremover.schedule.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import d.d.photoremover.schedule.ScheduledPhoto;

public class ScheduleDatabaseHelper extends SQLiteOpenHelper {
    private final static String DATABASE_NAME = "schedule";
    private final static String SCHEDULED_PHOTOS_TABLE_NAME = "scheduled_photos";
    private final static String COLUMN_ID = "id";
    private final static String COLUMN_URI = "uri";
    private final static String COLUMN_FILE_PATH = "file_path";
    private final static String COLUMN_EXPIRY_DATE = "expiry_date";
    private final static int DATABASE_VERSION = 1;

    private final SQLiteDatabase database;

    public ScheduleDatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        database = getWritableDatabase();
    }

    public void close() {
        this.database.close();
    }

    public void schedulePhotoExpiry(ScheduledPhoto photo) {
        ContentValues values = new ContentValues();

        values.put(COLUMN_URI, photo.getUri());
        values.put(COLUMN_FILE_PATH, photo.getFilePath());
        values.put(COLUMN_EXPIRY_DATE, photo.getExpiryDate());

        if (photo.hasId()) {
            this.database.update(
                    SCHEDULED_PHOTOS_TABLE_NAME,
                    values,
                    COLUMN_ID + " = ?",
                    new String[]{
                            String.valueOf(photo.getId())
                    }
            );
        } else {
            try{
                this.database.insertOrThrow(
                        SCHEDULED_PHOTOS_TABLE_NAME,
                        null,
                        values
                );
            }catch (SQLException e){
                Log.d(getClass().getCanonicalName(), "sql exception");
            }
        }
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + SCHEDULED_PHOTOS_TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY," +
                COLUMN_URI + " TEXT NOT NULL UNIQUE," +
                COLUMN_FILE_PATH + " TEXT NOT NULL," +
                COLUMN_EXPIRY_DATE + " INTEGER NOT NULL" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
