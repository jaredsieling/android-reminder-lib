package org.ohmage.reminders.example;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.ohmage.reminders.base.ReminderContract.Reminders;

import java.util.Arrays;
import java.util.List;

public class ReminderProvider extends ContentProvider {
    private static final String TAG = ReminderProvider.class.getSimpleName();

    public ReminderProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO: Implement this to handle requests to insert a new row.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean onCreate() {
        // TODO: Implement this to initialize your content provider on startup.
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        MatrixCursor cursor = new MatrixCursor(projection);

        List<String> selectionList = null;
        if (selectionArgs != null)
            selectionList = Arrays.asList(selectionArgs);

        // Add four fake surveys
        for (int i = 0; i < 4; i++) {
            if (selectionList == null || selectionList.contains(String.valueOf(i))) {
                cursor.newRow()
                        .add(Reminders.REMINDER_NAME, "Reminder #" + i)
                        .add(Reminders._ID, String.valueOf(i));
            }
        }
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        long time = values.getAsLong(Reminders.REMINDER_PENDING_TIME);
        String timezone = values.getAsString(Reminders.REMINDER_PENDING_TIMEZONE);
        String t = DateTimeFormat.fullTime().withZone(DateTimeZone.forID(timezone)).print(time);
        if(time == Reminders.NOT_PENDING) {
            Log.d(TAG, "The surveys: " + Arrays.toString(selectionArgs) + "are no longer pending");
        } else {
            Log.d(TAG, "The surveys: " + Arrays.toString(selectionArgs) + "are pending at " + t);
        }
        return selectionArgs.length;
    }
}
