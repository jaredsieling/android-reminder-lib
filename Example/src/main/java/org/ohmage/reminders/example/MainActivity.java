package org.ohmage.reminders.example;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.ohmage.reminders.glue.TriggerFramework;
import org.ohmage.reminders.ui.TriggerListActivity;

import java.util.Arrays;


public class MainActivity extends ActionBarActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.show_all_reminders).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), TriggerListActivity.class);
                startActivity(intent);
                String[] active = TriggerFramework.getActiveSurveys(getBaseContext(), null);
                Log.d(TAG, "active surveys:" + Arrays.toString(active));
            }
        });

        findViewById(R.id.show_specific_reminders).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), TriggerListActivity.class);
                String[] actions = new String[2];
                actions[0] = "0";
                actions[1] = "1";
                intent.putExtra(TriggerListActivity.EXTRA_ACTIONS, actions);
                startActivity(intent);
            }
        });

        findViewById(R.id.show_group_reminders).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), TriggerListActivity.class);
                intent.putExtra(TriggerListActivity.EXTRA_CAMPAIGN_URN, "group_id");
                intent.putExtra(TriggerListActivity.EXTRA_NAME, "Group Name");
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
