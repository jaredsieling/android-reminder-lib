package org.ohmage.reminders.base;

import android.net.Uri;
import android.provider.BaseColumns;

import org.ohmage.reminders.glue.TriggerFramework;

/**
 * Clients should create a provider to handle these columns for a content authority they specify
 * using {@link org.ohmage.reminders.glue.TriggerFramework#setAuthority(String)}. The Uri will look
 * something like content://{authority}/reminders.
 */
public abstract class ReminderContract {

    interface ReminderColumns {
        /**
         * The name of this reminder
         */
        String REMINDER_NAME = "reminder_name";

        /**
         * The group of this reminder if it is part of a group
         */
        String REMINDER_GROUP = "reminder_group";

        /**
         * The time that the reminder went off
         */
        String REMINDER_PENDING_TIME = "reminder_pending_time";

        /**
         * The timezone of the time where the reminder went off
         */
        String REMINDER_PENDING_TIMEZONE = "reminder_pending_timezone";
    }

    private static final String PATH_REMINDERS = "reminders";

    /**
     * Represents an reminder.
     */
    public static final class Reminders implements BaseColumns, ReminderColumns {

        public static final long NOT_PENDING = -1;

        public static Uri buildRemindersUri() {
            return buildBaseContentUri().buildUpon().appendPath(PATH_REMINDERS).build();
        }
    }

    private static Uri buildBaseContentUri() {
        if (TriggerFramework.getAuthority() == null)
            throw new RuntimeException("The Authority for the reminder ContentProvider must be specified");
        return Uri.parse("content://" + TriggerFramework.getAuthority());
    }
}
