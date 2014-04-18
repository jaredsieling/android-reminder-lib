package org.ohmage.reminders.example;

import android.app.Application;

import org.ohmage.reminders.glue.TriggerFramework;

/**
 * Created by cketcham on 4/16/14.
 */
public class ReminderExampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        TriggerFramework.setAuthority("org.ohmage.reminders");
    }
}
