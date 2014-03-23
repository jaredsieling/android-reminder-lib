
package org.ohmage.reminders.base;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import org.ohmage.reminders.base.AdminDialogFragment.AdminCodeListener;
import org.ohmage.reminders.R;

/**
 * <p>
 * This activity marshals the admin pin code dialog for activities which can't
 * start fragments on their own.
 * </p>
 * <p>
 * Activities can start this with
 * {@link #startActivityForResult(android.content.Intent, int)} and will receive
 * {@link Activity#RESULT_OK} if the pin is entered correctly, and
 * {@link Activity#RESULT_CANCELED} if it is not
 * </p>
 * 
 * @author cketcham
 */
public class AdminPincodeActivity extends FragmentActivity implements AdminCodeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {

            // If we have admin mode set, we don't need to show the pincode
            if (getResources().getBoolean(R.bool.admin_mode)) {
                setResult(RESULT_OK);
                finish();
            }

            AdminDialogFragment newFragment = new AdminDialogFragment();
            newFragment.show(getSupportFragmentManager(), "dialog");
        }
    }

    @Override
    public void onAdminCodeEntered(boolean success) {
        setResult(success ? RESULT_OK : RESULT_CANCELED);
        finish();
    }
}
