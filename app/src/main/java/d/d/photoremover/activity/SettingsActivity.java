package d.d.photoremover.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import d.d.photoremover.R;
import d.d.photoremover.event.EventService;

public class SettingsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment(this))
                    .commit();

        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.settings_title_settings);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        startService(
                new Intent(
                        EventService.ACTION_PREFERENCE_CHANGE,
                        null,
                        this,
                        EventService.class
                )
        );
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;

        public SettingsFragment(SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
            this.onSharedPreferenceChangeListener = onSharedPreferenceChangeListener;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            getPreferenceManager()
                    .getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this.onSharedPreferenceChangeListener);
        }
    }
}