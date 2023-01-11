package info.dprktech.androidtoolbox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class HdmiAudioBroadcastReceiver extends BroadcastReceiver {

    private static String HDMIINTENT = "android.intent.action.HDMI_AUDIO_PLUG";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals(HDMIINTENT)) {
            boolean state = intent.getBooleanExtra("state", false);

            if (state) {
                //Log.d("HDMIListner", "BroadcastReceiver.onReceive() : Connected HDMI-TV");
                Toast.makeText(context, "HDMI Audio Connected", Toast.LENGTH_LONG).show();
            } else {
                //Log.d("HDMIListner", "HDMI >>: Disconnected HDMI-TV");
                Toast.makeText(context, "HDMI Audio Disconnected", Toast.LENGTH_LONG).show();
            }
        }
    }
}