package com.hackz.facebrio.activity;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.hackz.facebrio.R;
import com.hackz.facebrio.fragment.SettingsFragment;

import org.wordpress.passcodelock.AppLockManager;
import org.wordpress.passcodelock.PasscodePreferenceFragment;


public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = SettingsActivity.class.getSimpleName();
    private static final int REQUEST_STORAGE = 1;

    private static final String KEY_PASSCODE_FRAGMENT = "passcode-fragment";
    private static final String KEY_PREFERENCE_FRAGMENT = "preference-fragment";

    private PasscodePreferenceFragment mPasscodePreferenceFragment;
    private SettingsFragment mSamplePreferenceFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // set a toolbar to replace the action bar
        Toolbar toolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }



        FragmentManager fragmentManager = getFragmentManager();
        mSamplePreferenceFragment = (SettingsFragment) fragmentManager.findFragmentByTag(KEY_PREFERENCE_FRAGMENT);
        mPasscodePreferenceFragment = (PasscodePreferenceFragment) fragmentManager.findFragmentByTag(KEY_PASSCODE_FRAGMENT);

        if (mSamplePreferenceFragment == null || mPasscodePreferenceFragment == null) {
            Bundle passcodeArgs = new Bundle();
            passcodeArgs.putBoolean(PasscodePreferenceFragment.KEY_SHOULD_INFLATE, false);
            mSamplePreferenceFragment = new SettingsFragment();
            mPasscodePreferenceFragment = new PasscodePreferenceFragment();
            mPasscodePreferenceFragment.setArguments(passcodeArgs);

            fragmentManager.beginTransaction()
                    .replace(android.R.id.content, mPasscodePreferenceFragment, KEY_PASSCODE_FRAGMENT)
                    .add(android.R.id.content, mSamplePreferenceFragment, KEY_PREFERENCE_FRAGMENT)
                    .commit();
        }



    }

    @Override
    public void onBackPressed() {
        int count = getFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            //super.onBackPressed();
        } else
            getFragmentManager().popBackStack();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Storage permission granted");
                } else {
                    Log.e(TAG, "Storage permission denied");
                    Toast.makeText(this, getString(R.string.no_storage_permission), Toast.LENGTH_SHORT).show();
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onStart() {
        super.onStart();

        Preference togglePreference = mSamplePreferenceFragment.findPreference(
                getString(org.wordpress.passcodelock.R.string.pref_key_passcode_toggle));
        Preference changePreference = mSamplePreferenceFragment.findPreference(
                getString(org.wordpress.passcodelock.R.string.pref_key_change_passcode));

        if (togglePreference != null && changePreference != null) {
            mPasscodePreferenceFragment.setPreferences(togglePreference, changePreference);
            ((SwitchPreference) togglePreference).setChecked(
                    AppLockManager.getInstance().getAppLock().isPasswordLocked());
        }
    }
}
