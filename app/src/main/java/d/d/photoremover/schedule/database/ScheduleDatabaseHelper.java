package d.d.photoremover.schedule.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import d.d.photoremover.schedule.ScheduledPhoto;

public class ScheduleDatabaseHelper extends SQLiteOpenHelper {
    private final static String DATABASE_NAME = "schedule";
    private final static String TABLE_NAME_SCHEDULED_PHOTOS = "scheduled_photos";
    private final static String COLUMN_ID = "id";
    private final static String COLUMN_URI = "uri";
    private final static String COLUMN_FILE_PATH = "file_path";
    private final static String COLUMN_EXPIRY_DATE = "expiry_date";
    private final static String COLUMN_STATE = "state";
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
        values.put(COLUMN_STATE, photo.getState().toString());

        if (photo.hasId()) {
            this.database.update(
                    TABLE_NAME_SCHEDULED_PHOTOS,
                    values,
                    COLUMN_ID + " = ?",
                    new String[]{
                            String.valueOf(photo.getId())
                    }
            );
        } else {
            try {
                this.database.insertOrThrow(
                        TABLE_NAME_SCHEDULED_PHOTOS,
                        null,
                        values
                );
            } catch (SQLException e) {
                Log.d(getClass().getName(), "sql exception");
                Log.d(getClass().getName(), photo.getUri());
                e.printStackTrace();
            }
        }
    }

    // TODO: maybe helper should just return List<>, not arrayList
    public ArrayList<ScheduledPhoto> getScheduledPhotos() {
        Cursor cursor = this.database.query(
                TABLE_NAME_SCHEDULED_PHOTOS,
                new String[]{
                        COLUMN_ID,
                        COLUMN_URI,
                        COLUMN_FILE_PATH,
                        COLUMN_EXPIRY_DATE,
                        COLUMN_STATE
                },
                null,
                null,
                null,
                null,
                COLUMN_EXPIRY_DATE
        );

        ArrayList<ScheduledPhoto> scheduledPhotos = new ArrayList<>(cursor.getCount());

        if (!cursor.moveToFirst()) {
            cursor.close();
            return scheduledPhotos;
        }

        int idIndex = cursor.getColumnIndex(COLUMN_ID);
        int uriIndex = cursor.getColumnIndex(COLUMN_URI);
        int pathIndex = cursor.getColumnIndex(COLUMN_FILE_PATH);
        int expiryIndex = cursor.getColumnIndex(COLUMN_EXPIRY_DATE);
        int stateIndex = cursor.getColumnIndex(COLUMN_STATE);

        do {
            ScheduledPhoto scheduledPhoto = new ScheduledPhoto(
                    cursor.getString(uriIndex),
                    cursor.getString(pathIndex),
                    ScheduledPhoto.State.fromString(cursor.getString(stateIndex)),
                    cursor.getLong(expiryIndex)
            );
            scheduledPhoto.setId(
                    cursor.getInt(idIndex)
            );
            scheduledPhotos.add(scheduledPhoto);
        } while (cursor.moveToNext());

        cursor.close();
        return scheduledPhotos;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + TABLE_NAME_SCHEDULED_PHOTOS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY," +
                COLUMN_URI + " TEXT NOT NULL UNIQUE," +
                COLUMN_FILE_PATH + " TEXT NOT NULL," +
                COLUMN_EXPIRY_DATE + " INTEGER NOT NULL," +
                COLUMN_STATE + " TEXT NOT NULL" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
