package info.dprktech.androidtoolbox;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.content.Intent;
import android.content.ComponentName;
import android.widget.Toast;
import android.media.AudioManager;
import android.os.UserManager;
import android.content.SharedPreferences;
import android.provider.Settings.Global;
import java.lang.Process;
import java.lang.ProcessBuilder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    //private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            //mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
            //        | View.SYSTEM_UI_FLAG_FULLSCREEN
            //        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            //        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            //        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            //        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    //private View mControlsView;
/*    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };*/

    private boolean isUsbTetheringActive = false;

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            //if (AUTO_HIDE) {
            //    delayedHide(AUTO_HIDE_DELAY_MILLIS);
            //}
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        //mVisible = true;
        //mControlsView = findViewById(R.id.fullscreen_content_controls);
        //mContentView = findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        //mContentView.setOnClickListener(new View.OnClickListener() {
        //    @Override
        //    public void onClick(View view) {
        //        toggle();
        //    }
        //});

        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction(AudioManager.ACTION_HDMI_AUDIO_PLUG);

        BroadcastReceiver hdmiautioreceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (action.equals(AudioManager.ACTION_HDMI_AUDIO_PLUG)) {
                    int state = intent.getIntExtra("state", 0);

                    if (state == 1) {
                        //Log.d("HDMIListner", "BroadcastReceiver.onReceive() : Connected HDMI-TV");
                        Toast.makeText(context, "HDMI Audio Connected", Toast.LENGTH_LONG).show();
                    } else {
                        //Log.d("HDMIListner", "HDMI >>: Disconnected HDMI-TV");
                        Toast.makeText(context, "HDMI Audio Disconnected", Toast.LENGTH_LONG).show();
                    }
                }
            }
        };

        registerReceiver(hdmiautioreceiver, intentfilter);


    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        //delayedHide(100);
    }

/*
    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }
*/

/*    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }*/

/*    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        //mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        //        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    *//**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     *//*
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }*/

    public void devtoolsMessage(View view) {
        try {
            startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (Exception e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
    }

    public void audioplugMessage(View view) {
        //Intent intent = new Intent("android.media.action.HDMI_AUDIO_PLUG");
        //Intent intent = new Intent("android.intent.action.HDMI_AUDIO_PLUG");
        Intent intent = new Intent(AudioManager.ACTION_HDMI_AUDIO_PLUG);
        intent.putExtra("state", 1);
        try {
            //startActivity(intent);
            sendBroadcast(intent);
        } catch (Exception e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
    }

    public void audiounplugMessage(View view) {
        Intent intent = new Intent(AudioManager.ACTION_HDMI_AUDIO_PLUG);
        intent.putExtra("state", 0);
        try {
            //startActivity(intent);
            sendBroadcast(intent);
        } catch (Exception e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
    }

    public void usbsettingsMessage(View view) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.UsbSettings"));
        try {
            startActivity(intent);
        } catch (Exception e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
    }

    public void getuseridMessage(View view){
        String username = "";
        try {
            Context c = getApplicationContext();
            UserManager u = (UserManager) c.getSystemService(Context.USER_SERVICE);
            username = u.getUserName();
        } catch (NullPointerException e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"Failed to retrieve username",Toast.LENGTH_LONG).show();
        } catch (Exception e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
        Toast.makeText(getApplicationContext(), username, Toast.LENGTH_LONG).show();
    }

    public void deviceinfoasrootMessage(View view){
        try {
            Process p = new ProcessBuilder()
                    .command("/system/xbin/su")
                    .redirectErrorStream(true).start();
            OutputStream out = p.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            //am start -a android.settings.SETTINGS
            //am start -a com.android.settings.APPLICATION_DEVELOPMENT_SETTINGS
            //am start -a com.android.settings.DEVICE_INFO_SETTINGS
            String cmd = "am start -a com.android.settings.DEVICE_INFO_SETTINGS";
            out.write(cmd.getBytes());
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ( (line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
            String result = builder.toString();
            Toast.makeText(getApplicationContext(),result,Toast.LENGTH_LONG).show();
            //startActivity(new Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS));
        } catch (Exception e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
    }

    public void showdevoptionsMessage(View view){
        try {
            Context settings_ctx = createPackageContext(
                    "com.android.settings",
                    CONTEXT_IGNORE_SECURITY);
            SharedPreferences settings_prefs;
            settings_prefs = settings_ctx.getSharedPreferences(
                    "development", 0);
            settings_prefs.edit().putBoolean("show", true).apply();
        } catch (Exception e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),
                           e.toString(),
                           Toast.LENGTH_LONG).show();
        }
    }

    public void globalenableadbMessage(View view){
        try {
            Global.putInt(getContentResolver(), Global.ADB_ENABLED, 1);
        } catch (Exception e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
    }

    public void globaldisableadbMessage(View view){
        try {
            Global.putInt(getContentResolver(), Global.ADB_ENABLED, 0);
        } catch (Exception e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
    }

    public void toggleUsbTethering(View view){
        Object obj = getSystemService(Context.CONNECTIVITY_SERVICE);
        for (Method m : obj.getClass().getDeclaredMethods()) {
            if (m.getName().equals("tether")) {
                try {
                    m.invoke(obj, "usb0");
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public void toggleUsbTethering2(View view) {
        isUsbTetheringActive = !isUsbTetheringActive;
        Object obj = getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Method m = obj.getClass().getMethod("setUsbTethering", Boolean.TYPE);
            m.invoke(obj, Boolean.valueOf(isUsbTetheringActive));
        } catch (NoSuchMethodException e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
    }
}
