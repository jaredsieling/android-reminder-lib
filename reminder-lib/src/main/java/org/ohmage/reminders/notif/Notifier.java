/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.reminders.notif;


import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.ohmage.reminders.R;
import org.ohmage.reminders.base.ReminderContract.Reminders;
import org.ohmage.reminders.base.TriggerBase;
import org.ohmage.reminders.base.TriggerDB;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/*
 * The trigger notification manager. The logic which displays, repeats and
 * removes the home screen notifications are implemented here. Whenever a 
 * trigger goes off, the base calls the notifyNewTrigger provided by this 
 * class and passes the notification description as argument. This class 
 * displays the notification, sets up alarms to expire and repeat the 
 * notification. 
 * 
 * The notifications from all the triggers are summarized into one item
 * on the home screen. At any point in time, this notification item will 
 * display the list of all surveys which are active at that moment. 
 * Whenever a trigger expires, the list of surveys associated with that 
 * trigger are removed from the notification item and the surveys list 
 * in the item is updated with the rest of active surveys if any.  
 */
public class Notifier {

    private static final String TAG = "Notifier";

    //TODO - This needs to be defined in a common place in order
    //make sure that it does not collide with any other notification
    //id in Ohmage
    private static final int NOIF_ID = 100;

    //Action of the intent which is broadcasted when the user
    //clicks on the notification
    private static final String ACTION_TRIGGER_NOTIFICATION =
            "org.ohmage.reminders.TRIGGER_NOTIFICATION";
    //Action of the intent which is broadcasted when the notification
    //is updated by adding or removing surveys
    private static final String ACTION_ACTIVE_SURVEY_LIST_CHANGED =
            "org.ohmage.reminders.SURVEY_LIST_CHANGED";

    private static final String ACTION_NOTIF_CLICKED =
            "edu.ucla.cens.triggers.notif.Notifier.notification_clicked";
    private static final String ACTION_NOTIF_DELETED =
            "edu.ucla.cens.triggers.notif.Notifier.notification_deleted";
    private static final String ACTION_NOTIF_IGNORED =
            "edu.ucla.cens.triggers.notif.Notifier.notification_ignored";
    private static final String ACTION_NOTIF_SNOOZED =
            "edu.ucla.cens.triggers.notif.Notifier.notification_snoozed";
    private static final String ACTION_NOTIF_RESHOW =
            "edu.ucla.cens.triggers.notif.Notifier.notification_end_snooze";
    private static final String ACTION_EXPIRE_ALM =
            "edu.ucla.cens.triggers.notif.Notifier.expire_notif";
    private static final String ACTION_REPEAT_ALM =
            "edu.ucla.cens.triggers.notif.Notifier.repeat_notif";

    public static final String EXTRA_SURVEYS = Notifier.class.getName() + ".extra_surveys";

    private static final String KEY_TRIGGER_ID =
            Notifier.class.getName() + ".trigger_id";
    private static final String KEY_REPEAT_LIST =
            Notifier.class.getName() + ".repeat_list";

    private static final String KEY_NOTIF_VISIBILITY_PREF =
            "notif_visibility";

    /*
     * Utility function to save the status of the notification when it
     * is cleared from the home screen. The status is persistently stored
     * using shared preferences. This status is used while the notification
     * needs to be refreshed 'quietly'. If the notification is already hidden
     * and it needs to be refreshed quietly (without alerting the user), no
     * action is required.
     */
    private static void hideNotification(Context context) {
        NotificationManager notifMan = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);

        notifMan.cancel(NOIF_ID);
        saveNotifVisibility(context, false);
    }

    private static void displayNotification(Context context, ArrayList<String> surveys, boolean quiet) {

        // If the notification is to be refreshed quietly, and if it is hidden, do nothing.
        if (quiet && !getNotifVisibility(context)) {
            return;
        }

        // Cancel any alarms to reshow the notification because we are showing it now
        cancelAlarm(context, ACTION_NOTIF_RESHOW);

        NotificationManager notifMan = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);

        //Watch for notification cleared events
        Intent deleteIntent = new Intent(context, NotifReceiver.class)
                .setAction(ACTION_NOTIF_DELETED);
        PendingIntent piDelete = PendingIntent.getBroadcast(context, 0, deleteIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        //Watch for notification clicked events
        Intent intent = new Intent(context, NotifReceiver.class).setAction(ACTION_NOTIF_CLICKED)
                .putExtra(EXTRA_SURVEYS, new ArrayList<String>(surveys));
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        // Constructs the Builder object.
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.survey_notification)
                        .setContentText(context.getResources().getQuantityString(R.plurals.survey_notification_message, surveys.size()))
                        .setAutoCancel(true)
                        .setContentTitle(context.getString(R.string.notifications_multi_title,
                                surveys.size()))
                        .setContentIntent(pi)
                        .setDeleteIntent(piDelete);

        if(!quiet) {
            builder.setDefaults(Notification.DEFAULT_ALL);
        }

        if (surveys.size() == 1) {
            Intent ignoreIntent = new Intent(context, NotifReceiver.class)
                    .setAction(ACTION_NOTIF_IGNORED).putExtra(EXTRA_SURVEYS, surveys);
            PendingIntent piIgnore = PendingIntent.getBroadcast(context, 0, ignoreIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent snoozeIntent = new Intent(context, NotifReceiver.class)
                    .setAction(ACTION_NOTIF_SNOOZED);
            PendingIntent piSnooze = PendingIntent.getBroadcast(context, 0, snoozeIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            builder.addAction(R.drawable.stat_notify_alarm, context.getString(R.string.snooze), piSnooze)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.ignore), piIgnore);

            // Get the survey name
            Cursor cursor = context.getContentResolver().query(Reminders.buildRemindersUri(),
                    new String[]{Reminders.REMINDER_NAME}, Reminders._ID + "=?",
                    new String[]{surveys.get(0)}, null);
            if (cursor.moveToFirst()) {
                builder.setContentTitle(cursor.getString(0));
            }
        }
        notifMan.notify(NOIF_ID, builder.build());

        //Save the current visibility
        saveNotifVisibility(context, true);
    }

    /*
     * Utility function to prepare a string of surveys from a list
     * of surveys. This function merely adds commas in between.
     */
    private static String getSurveyDisplayList(Set<String> surveys) {
        String ret = "";

        int i = 0;
        for (String survey : surveys) {
            ret += survey;

            i++;
            if (i < surveys.size()) {
                ret += ", ";
            }
        }

        return ret;
    }

    /*
     * Refreshes the notification. The caller can specify if the user needs
     * to be alerted or the refresh needs to be done quietly. The user
     * can be alerted when there is a new trigger or when there is a repeat
     * reminder. The notification can be refreshed quietly when a trigger
     * expires.
     */
    public static void refreshNotification(Context context, boolean quiet) {

        Log.v(TAG, "Notifier: Refreshing notification, quiet = " + quiet);

        //Get the list of all the surveys active at the moment
        Set<String> actSurveys = NotifSurveyAdaptor.getAllActiveSurveys(context, null);

        //Remove the notification if there are no active surveys
        if (actSurveys.size() == 0) {
            Log.v(TAG, "Notifier: No active surveys");
            hideNotification(context);
        } else {
            //Prepare the message and display the notification
            displayNotification(context, new ArrayList<String>(actSurveys), quiet);
        }
    }

    private static void cancelAllAlarms(Context context, int trigId) {
        AlarmManager alarmMan = (AlarmManager) context.getSystemService(
                Context.ALARM_SERVICE);

        Intent i = new Intent(context, NotifReceiver.class).setAction(ACTION_EXPIRE_ALM);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i,
                PendingIntent.FLAG_NO_CREATE);

        if (pi != null) {
            alarmMan.cancel(pi);
            pi.cancel();
        }

        i = new Intent(context, NotifReceiver.class).setAction(ACTION_REPEAT_ALM);
        pi = PendingIntent.getBroadcast(context, 0, i,
                PendingIntent.FLAG_NO_CREATE);

        if (pi != null) {
            alarmMan.cancel(pi);
            pi.cancel();
        }
    }

    private static void cancelAlarm(Context context, String action) {
        AlarmManager alarmMan = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent i = new Intent(context, NotifReceiver.class).setAction(action);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i,
                PendingIntent.FLAG_NO_CREATE);

        if (pi != null) {
            alarmMan.cancel(pi);
            pi.cancel();
        }
    }

    private static void setAlarm(Context context,
                                 String action,
                                 int mins,
                                 Bundle extras) {

        Log.v(TAG, "Notifier: Setting alarm(" + mins + ", " + action + ")");

        AlarmManager alarmMan = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);

        Intent i = new Intent(context, NotifReceiver.class).setAction(action);
        if (extras != null) {
            i.putExtras(extras);
        }

        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i,
                PendingIntent.FLAG_CANCEL_CURRENT);

        long elapsed = mins * 60 * 1000;

        alarmMan.set(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + elapsed, pi);
    }

    /*
     * Set the alarm for a first item in the repeat reminder list
     * of a trigger. The remaining list is bundled with the alarm
     * intent. When alarm fires, this function is called again
     * with the repeat list obtained from the bundle until there
     * are no items remaining in the list.
     *
     * In order make this algorithm easier, this function accepts
     * the repeats as a list of their diffs.
     */
    private static void setRepeatAlarm(Context context, int trigId,
                                       int[] repeatDiffs) {

        if (repeatDiffs.length == 0) {
            //No more repeats in the list
            return;
        }

        //Set a repeat reminder for the first item in the list
        //and prepare the new list by removing this item

        int[] newRepeats = new int[repeatDiffs.length - 1];
        System.arraycopy(repeatDiffs, 1, newRepeats, 0, repeatDiffs.length - 1);

        Bundle repeatBundle = new Bundle();
        repeatBundle.putIntArray(KEY_REPEAT_LIST, newRepeats);
        //Set the alarm for the first repeat item and attach the remaining list
        setAlarm(context, ACTION_REPEAT_ALM, repeatDiffs[0], repeatBundle);
    }

    /*
     * Restores the state of a notification such as repeat reminders and
     * expiration timer for a specific trigger. This can be called at
     * bootup to restore the notification if it is still valid after
     * bootup.
     *
     * The expiration alarm is restored for the rest of the interval
     * calculated using the saved trigger time stamp.
     *
     * In the case of repeat reminder, only the remaining valid
     * reminders are set.
     */
    public static void restorePastNotificationStates(Context context,
                                                     int trigId,
                                                     String notifDesc,
                                                     long timeStamp) {

        NotifDesc desc = new NotifDesc();
        if (!desc.loadString(notifDesc)) {
            return;
        }

        //Cancel all the current alarms
        cancelAllAlarms(context, trigId);

        //if it hasn't expired yet, create a notif for the remaining time
        long now = System.currentTimeMillis();

        if (timeStamp > now || timeStamp < 0) {
            //TODO log
            return;
        }

        //Calculate the elapsed number of minutes for this trigger
        int elapsedMins = (int) (((now - timeStamp) / 1000) / 60);
        //Calculate the remaining duration for this trigger
        int remDuration = desc.getDuration() - elapsedMins;

        if (remDuration <= 0) {
            //The trigger expired
            return;
        }

        //Set an expire alarm for the remaining duration
        setAlarm(context, ACTION_EXPIRE_ALM, remDuration, null);

        //Set an alarm for the remaining repeats, if any
        List<Integer> repeats = desc.getSortedRepeats();
        //Check if there is any repeat after the current time
        //Older repeats are to be discarded
        int i = 0;
        for (int repeat : repeats) {

            if (repeat > elapsedMins) {

                //There are repeats after the current time

                //Discard the older ones from the list
                int[] repeatDiffs = getRepeatDiffs(repeats.subList(i, repeats.size()));

                /* Subtract the elapsed time from the first repeat
                 * For instance, let the original repeat list be [5, 10, 15].
                 * Let's assume 7 minutes have elapsed.
                 * So, the remaining list would be [10, 15] and the diff list
                 * of this remaining list would be [10, 5]. Now, since 7 minutes
                 * have already elapsed, the first alarm should be set to fire
                 * after 3 minutes (10 - 7)
                 */

                repeatDiffs[0] -= elapsedMins;

                setRepeatAlarm(context, trigId, repeatDiffs);
                break;
            }

            i++;
        }
    }

    /*
     * Prepare the array of the differences of the repeats from
     * the repeat reminders list. The given list must be sorted.
     */
    private static int[] getRepeatDiffs(List<Integer> repeatList) {

        int[] ret = new int[repeatList.size()];

        int i = 0;
        for (int repeat : repeatList) {

            if (i > 0) {
                ret[i] = (repeat - ret[i - 1]);
            } else {
                ret[i] = repeat;
            }

            i++;
        }

        return ret;
    }

    /*
     * Utility function to handle a repeat reminder alarm. Refreshes the
     * notification and resets the repeat alarm if required.
     */
    private static void repeatReminder(Context context, int trigId, Intent intent) {

        TriggerDB db = new TriggerDB(context);
        db.open();
        TriggerDB.Campaign campaign = db.getCampaignInfo(trigId);
        db.close();

        Set<String> actSurveys = NotifSurveyAdaptor.getActiveSurveysForTrigger(context,
                trigId);

        //Check if this trigger is still active. If not cancel all the alarms
        if (actSurveys.size() == 0) {
            cancelAllAlarms(context, trigId);
            return;
        }

        //Trigger is still active, alert the user
        refreshNotification(context, false);
        //Continue the remaining repeat reminders
        int[] repeatDiffs = intent.getIntArrayExtra(KEY_REPEAT_LIST);
        setRepeatAlarm(context, trigId, repeatDiffs);
    }

    private static void handleNotifClicked(Context context, ArrayList<String> surveys) {
        //Hide the notification window when the user clicks on it
        hideNotification(context);

        //Broadcast to Ohmage
        Intent i = new Intent(ACTION_TRIGGER_NOTIFICATION);
        i.putExtra(EXTRA_SURVEYS, surveys);
        i.setPackage(context.getPackageName());
        context.sendBroadcast(i);
    }

    /*
     * Save the visibility of the notification to preferences
     */
    private static void saveNotifVisibility(Context context, boolean visible) {
        SharedPreferences pref = context.getSharedPreferences(
                Notifier.class.getName(),
                Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(KEY_NOTIF_VISIBILITY_PREF, visible);
        editor.commit();
    }

    /*
     * Get the current visibility of the notification
     */
    private static boolean getNotifVisibility(Context context) {
        SharedPreferences pref = context.getSharedPreferences(
                Notifier.class.getName(),
                Context.MODE_PRIVATE);

        return pref.getBoolean(KEY_NOTIF_VISIBILITY_PREF, false);
    }


    private static void handleNotifDeleted(Context context) {
        saveNotifVisibility(context, false);
    }

    private static void handleTriggerExpired(Context context, int trigId) {

        Log.v(TAG, "Notifier: Handling expiration alarm for: "
                + trigId);

        //Log information related to expired triggers.
        NotifSurveyAdaptor.handleExpiredTrigger(context, trigId);

        TriggerDB db = new TriggerDB(context);
        db.open();
        TriggerDB.Campaign campaign = db.getCampaignInfo(trigId);
        db.close();

        //Quietly refresh the notification
        Notifier.refreshNotification(context, true);
    }

    /*
     * Displays a new trigger notification. If the notification is
     * already being displayed, the survey list is updated and the user
     * is alerted.
     */
    public static void notifyNewTrigger(Context context,
                                        int trigId,
                                        String notifDesc) {

        //Clear all existing alarms for this trigger if required
        cancelAllAlarms(context, trigId);
        //Update the notification with quite = false
        refreshNotification(context, false);

        NotifDesc desc = new NotifDesc();
        if (!desc.loadString(notifDesc)) {
            Log.e(TAG, "Notifier: Error parsing notif desc in " +
                    "notifyNewTrigger()");
            return;
        }

        //Set an alarm to expire this trigger notif
        setAlarm(context, ACTION_EXPIRE_ALM, desc.getDuration(), null);

        //Set an alarm for repeat reminder
        int[] repeatDiffs = getRepeatDiffs(desc.getSortedRepeats());
        setRepeatAlarm(context, trigId, repeatDiffs);
    }

    public static void removeTriggerNotification(Context context, int trigId) {
        //Clear all existing alarms for this trigger if required
        cancelAllAlarms(context, trigId);
        refreshNotification(context, true);
    }

    /* Receiver for all alarms */
    public static class NotifReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(ACTION_NOTIF_CLICKED)) {
                ArrayList<String> surveys = intent.getStringArrayListExtra(EXTRA_SURVEYS);
                Notifier.handleNotifClicked(context, surveys);
            } else if (intent.getAction().equals(ACTION_NOTIF_IGNORED)) {
                hideNotification(context);
                // Set the surveys for this trigger to be ignored
                ArrayList<String> surveys = intent.getStringArrayListExtra(EXTRA_SURVEYS);
                for (String survey : surveys) {
                    NotifSurveyAdaptor.recordSurveyIgnored(context, survey);
                }
                TriggerBase.updatePendingStateForSurveys(context, Reminders.NOT_PENDING,
                        surveys.toArray(new String[]{}));
            } else if (intent.getAction().equals(ACTION_NOTIF_SNOOZED)) {
                Toast.makeText(context, R.string.notification_snoozed, Toast.LENGTH_SHORT).show();
                hideNotification(context);
                setAlarm(context, ACTION_NOTIF_RESHOW, 10, null);
            } else if (intent.getAction().equals(ACTION_NOTIF_RESHOW)) {
                refreshNotification(context, false);
            } else if (intent.getAction().equals(ACTION_NOTIF_DELETED)) {
                Notifier.handleNotifDeleted(context);
            } else if (intent.getAction().equals(ACTION_EXPIRE_ALM)) {

                Notifier.handleTriggerExpired(context,
                        intent.getIntExtra(KEY_TRIGGER_ID, -1));
            } else if (intent.getAction().equals(ACTION_REPEAT_ALM)) {

                if (!intent.hasExtra(KEY_TRIGGER_ID)) {
                    return;
                }

                int trigId = intent.getIntExtra(KEY_TRIGGER_ID, -1);

                Notifier.repeatReminder(context, trigId, intent);
            }
        }

    }
}
