package org.ohmage.reminders.base;

import android.content.Context;
import android.database.Cursor;

import java.util.HashMap;

/**
 * Created by cketcham on 4/16/14.
 */
public class Actions {
    private String[] mActionNames;
    private String[] mActionIds;
    private HashMap<String, String> mActions = new HashMap<String, String>();

    public Actions(Context context, String[] allowedActionIds) {
        String select = null;
        if (allowedActionIds != null) {
            StringBuilder selectBuilder = null;
            for (int i = 0; i < allowedActionIds.length; i++) {
                if (selectBuilder == null) {
                    selectBuilder = new StringBuilder();
                } else {
                    selectBuilder.append(" OR ");
                }
                selectBuilder.append(ReminderContract.Reminders._ID + "=?");
            }
            select = selectBuilder.toString();
        }

        mActions.clear();
        Cursor c = context.getContentResolver()
                .query(ReminderContract.Reminders.buildRemindersUri(), new String[]{ReminderContract.Reminders._ID, ReminderContract.Reminders.REMINDER_NAME}, select,
                        allowedActionIds, null);
        mActionIds = new String[c.getCount()];
        mActionNames = new String[c.getCount()];
        int i = 0;
        while (c.moveToNext()) {
            mActionIds[i] = c.getString(0);
            mActionNames[i] = c.getString(1);
            mActions.put(mActionIds[i], mActionNames[i]);
            i++;
        }
    }

    public String getName(String id) {
        return mActions.get(id);
    }

    public int size() {
        return mActionIds.length;
    }

    public String getId(int i) {
        return mActionIds[i];
    }

    public String[] getNames() {
        return mActionNames;
    }

    public String[] getIds() {
        return mActionIds;
    }
}
