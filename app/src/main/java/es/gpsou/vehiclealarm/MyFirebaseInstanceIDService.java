package es.gpsou.vehiclealarm;

import android.content.SharedPreferences;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {
    public MyFirebaseInstanceIDService() {
    }

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();

        SharedPreferences settings=getSharedPreferences(Globals.CONFIGURACION, 0);
        String mode=settings.getString(Globals.APP_MODE, Globals.NULL);
        String currentToken=settings.getString(Globals.FB_REGISTRATION_ID, null);

        SharedPreferences.Editor editor=settings.edit();
        editor.putString(Globals.FB_REGISTRATION_ID, refreshedToken);
        editor.apply();

        if(mode.compareTo(Globals.MONITORING_MODE)==0) {
            try {
                FBGroupManager.replaceFirebaseId(getApplicationContext(), currentToken, refreshedToken);
            } catch(Exception e) {
                e.printStackTrace();
            }
        } else if(mode.compareTo(Globals.IN_VEHICLE_MODE)==0) {
            String groupId=settings.getString(Globals.P2P_GROUP_ID, null);

            FirebaseMessaging fm = FirebaseMessaging.getInstance();
            String operation=Globals.P2P_OP_REPLACE_REGISTRATION_ID;
            String id = Integer.toString(Globals.msgId.incrementAndGet());
            RemoteMessage.Builder mRemoteMessage=new RemoteMessage.Builder(groupId);
            fm.send(mRemoteMessage.setMessageId(id)
                    .addData(Globals.P2P_DEST, Globals.P2P_DEST_MONITOR)
                    .addData(Globals.P2P_OP, operation)
                    .addData(Globals.P2P_OLD_FB_REGISTRATION_ID, currentToken)
                    .addData(Globals.P2P_NEW_FB_REGISTRATION_ID, refreshedToken)
                    .setTtl(3600)
                    .build());
        }
    }
}
