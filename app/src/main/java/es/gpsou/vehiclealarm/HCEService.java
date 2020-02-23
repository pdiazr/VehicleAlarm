package es.gpsou.vehiclealarm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.cardemulation.HostApduService;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;


@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class HCEService extends HostApduService {
    private static final long RETRY_INTERVAL=1000;

    public static boolean active=false;
    public static String groupId=null;

    @Override
    public byte[] processCommandApdu(byte[] apdu, Bundle extras) {
        if(!active)
            return(null);

        if(selectAidApdu(apdu)) {
            return(Globals.NFC_SYNC_STRING.getBytes(Charset.forName("UTF-8")));
        } else {
            SharedPreferences settings=getSharedPreferences(Globals.CONFIGURACION, 0);
            String remoteBtMac=null;
            String remoteFirebaseId=null;

            try {
                JSONObject receivedData = new JSONObject(new String(apdu));
                remoteBtMac=receivedData.getString(Globals.NFCBT_BT_MAC);
                remoteFirebaseId=receivedData.getString(Globals.NFCBT_FIREBASE_ID);
            } catch(JSONException e) {
                return(Globals.NFCBT_ERROR_STRING.getBytes(Charset.forName("UTF-8")));
            }

            String btMac=settings.getString(Globals.BT_MAC, "NULL");
            String firebaseId = settings.getString(Globals.FB_REGISTRATION_ID, "NULL");
//            String firebaseId = FirebaseInstanceId.getInstance().getToken();

            SharedPreferences.Editor editor=settings.edit();
            editor.putString(Globals.REMOTE_BT_MAC, remoteBtMac);
            editor.putString(Globals.REMOTE_FB_REGISTRATION_ID, remoteFirebaseId);
            editor.putString(Globals.FB_REGISTRATION_ID, firebaseId);
            editor.apply();

            JSONObject data=new JSONObject();
            try {
                data.put(Globals.NFCBT_BT_MAC, btMac);
                data.put(Globals.NFCBT_FIREBASE_ID, firebaseId);
            } catch(JSONException e) {
                throw new RuntimeException(e);
            }

            Intent intent=new Intent(this, NfcMonitorActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            new PairingThread(this).start();

            return(data.toString().getBytes(Charset.forName("UTF-8")));
        }
    }

    @Override
    public void onDeactivated(int reason) {

    }

    private boolean selectAidApdu(byte[] apdu) {
        return apdu.length >= 2 && apdu[0] == (byte)0 && apdu[1] == (byte)0xa4;
    }

/*    @Override
    public void run() {
        if(!active) {
            if(Looper.myLooper()!=null) {
                Looper.myLooper().quit();
                Log.d(Globals.TAG, "Looper detenido");
            }
            return;
        }

        Log.d(Globals.TAG, getSharedPreferences(Globals.CONFIGURACION, 0).toString());
        SharedPreferences settings=getSharedPreferences(Globals.CONFIGURACION, 0);
        String remoteFirebaseId=settings.getString(Globals.REMOTE_FB_REGISTRATION_ID, null);
//        String localFirebaseId=settings.getString(Globals.FB_REGISTRATION_ID, null);
        String localFirebaseId=FirebaseInstanceId.getInstance().getToken();
Log.d("DEBUGGING","remoteFirebaseId:"+remoteFirebaseId);
        Log.d("DEBUGGING","localFirebaseId:"+localFirebaseId);
        if(localFirebaseId==null) {
            Handler mainHandler = new Handler(getMainLooper());

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), R.string.FirebaseId_error, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        String idToken = null;
        try {
            if(groupId==null) {
                idToken = GoogleAuthUtil.getToken(this, account, Globals.CLIENT_ID);

                groupId = addNotificationKey(Globals.SENDER_ID, account.name, localFirebaseId, idToken);
                addNotificationKey(Globals.SENDER_ID, account.name, remoteFirebaseId, idToken);
            }

            Log.d(Globals.TAG, "GroupId: "+groupId);
            Log.d(Globals.TAG, "LocalFirebaseId: "+localFirebaseId);
            Log.d(Globals.TAG, "RemoteFirebaseId: "+remoteFirebaseId);

            FirebaseMessaging fm = FirebaseMessaging.getInstance();
            String to = groupId;
            String id = Integer.toString(Globals.msgId.incrementAndGet());
            fm.send(new RemoteMessage.Builder(to)
                    .setMessageId(id)
                    .addData(Globals.P2P_DEST, Globals.P2P_DEST_IN_VEHICLE)
                    .addData(Globals.P2P_OP, Globals.P2P_OP_PAIRING_COMMIT)
                    .addData(Globals.P2P_GROUP_ID, groupId)
                    .build());


            SharedPreferences.Editor editor=settings.edit();
            editor.putString(Globals.FB_GROUP_ID, groupId);
            editor.putString(Globals.FB_REGISTRATION_ID, localFirebaseId);
            editor.apply();

            if(Looper.myLooper()==null) {
                Looper.prepare();
                new Handler().postDelayed(this, RETRY_INTERVAL);
                Looper.loop();
            } else {
                new Handler().postDelayed(this, RETRY_INTERVAL);
            }

            Log.d(Globals.TAG, "Looper iniciado"); */

/*            editor.putString(Globals.APP_MODE, Globals.MONITORING_MODE);
            editor.apply();

            Intent intent = new Intent(this, MonitorActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent); */
/*        } catch (Exception e) {
            e.printStackTrace();
            Handler mainHandler = new Handler(getMainLooper());

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), R.string.HCEService_error, Toast.LENGTH_SHORT).show();
                }
            });
        }

    }

    public String addNotificationKey(
            String senderId, String name, String registrationId, String idToken)
            throws IOException, JSONException {
        URL url = new URL("https://android.googleapis.com/gcm/googlenotification");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);

        // HTTP request header
        con.setRequestProperty("project_id", senderId);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestMethod("POST");
        con.bind();

        // HTTP request
        JSONObject data = new JSONObject();
        data.put("operation", "add");
        data.put("notification_key_name", name);
        data.put("registration_ids", new JSONArray(Arrays.asList(registrationId)));
        data.put("id_token", idToken);
Log.d("DEBUGGING", senderId);
        Log.d("DEBUGGING", idToken);
        Log.d("DEBUGGING", data.toString());

        Log.d("DEBUGGING", "#####################");

        OutputStream os = con.getOutputStream();
        os.write(data.toString().getBytes("UTF-8"));
        os.close();

        // Read the response into a string
        InputStream is = con.getInputStream();
        String responseString = new Scanner(is, "UTF-8").useDelimiter("\\A").next();
        is.close();

        // Parse the JSON string and return the notification key
        JSONObject response = new JSONObject(responseString);
        return response.getString("notification_key");

    } */
}
