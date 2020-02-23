package es.gpsou.vehiclealarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.functions.FirebaseFunctions;

import org.json.JSONObject;

/**
 * Created by Pedro on 10/11/2017.
 */

public class PairingThread extends Thread {

    final long RETRY_INTERVAL=10000;
//    public static Account account=null;
    public static boolean active=false;
    Context mContext=null;
//    String groupId=null;

    public PairingThread(Context mContext) {
        this.mContext = mContext;

        active=true;
    }


    @Override
    public void run() {

        if(!active) {
            if(Looper.myLooper()!=null) {
                Looper.myLooper().quit();
                Log.d(Globals.TAG, "Looper detenido");
            }
            return;
        }

        SharedPreferences settings=mContext.getSharedPreferences(Globals.CONFIGURACION, 0);
        String remoteFirebaseId=settings.getString(Globals.REMOTE_FB_REGISTRATION_ID, null);
        String localFirebaseId=settings.getString(Globals.FB_REGISTRATION_ID, null);
//        String localFirebaseId=FirebaseInstanceId.getInstance().getToken();
        Log.d(Globals.TAG,"remoteFirebaseId:"+remoteFirebaseId);
        Log.d(Globals.TAG,"localFirebaseId:"+localFirebaseId);
        if(localFirebaseId==null) {
            Handler mainHandler = new Handler(mContext.getMainLooper());

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext.getApplicationContext(), R.string.FirebaseId_error, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }


//        String idToken = null;
        try {

/*            if(groupId==null) {
                idToken = GoogleAuthUtil.getToken(mContext, account, Globals.CLIENT_ID);

                groupId = FBGroupManager.addNotificationKey(Globals.SENDER_ID, account.name, localFirebaseId, idToken);
                FBGroupManager.addNotificationKey(Globals.SENDER_ID, account.name, remoteFirebaseId, idToken);
            }

            if(groupId==null) {
                groupId = FBGroupManager.createGroup(mContext, localFirebaseId, remoteFirebaseId);

                SharedPreferences.Editor editor=settings.edit();
                editor.putString(Globals.FB_GROUP_ID, groupId);
                editor.putString(Globals.FB_REGISTRATION_ID, localFirebaseId);
                editor.apply();
            }


            Log.d(Globals.TAG, "GroupId: "+groupId);
*/
            Log.d(Globals.TAG, "LocalFirebaseId: "+localFirebaseId);
            Log.d(Globals.TAG, "RemoteFirebaseId: "+remoteFirebaseId);

/*            FirebaseMessaging fm = FirebaseMessaging.getInstance();
            String to = groupId;
            String id = Integer.toString(Globals.msgId.incrementAndGet());
            fm.send(new RemoteMessage.Builder(to)
                    .setMessageId(id)
                    .addData(Globals.P2P_DEST, Globals.P2P_DEST_IN_VEHICLE)
                    .addData(Globals.P2P_OP, Globals.P2P_OP_PAIRING_COMMIT)
                    .addData(Globals.P2P_GROUP_ID, groupId)
                    .build());
*/
            FirebaseFunctions mFunctions = FirebaseFunctions.getInstance("europe-west1");
            JSONObject data=new JSONObject();
            data.put(Globals.P2P_TO, remoteFirebaseId);
            data.put(Globals.P2P_DEST, Globals.P2P_DEST_IN_VEHICLE);
            data.put(Globals.P2P_OP, Globals.P2P_OP_PAIRING_COMMIT);
            data.put(Globals.P2P_COMMIT_REGISTRATION_ID, localFirebaseId);
            mFunctions.getHttpsCallable("sendMessage").call(data);

            if(Looper.myLooper()==null) {
                Looper.prepare();
                new Handler().postDelayed(this, RETRY_INTERVAL);
                Looper.loop();
            } else {
                new Handler().postDelayed(this, RETRY_INTERVAL);
            }

            Log.d(Globals.TAG, "Looper iniciado");

/*            editor.putString(Globals.APP_MODE, Globals.MONITORING_MODE);
            editor.apply();

            Intent intent = new Intent(this, MonitorActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent); */
        } catch (Exception e) {
            e.printStackTrace();
            Handler mainHandler = new Handler(mContext.getMainLooper());

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext.getApplicationContext(), R.string.Pairing_error, Toast.LENGTH_SHORT).show();
                }
            });
        }

    }

/*    public String addNotificationKey(
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
        Log.d(Globals.TAG, senderId);
        Log.d(Globals.TAG, idToken);
        Log.d(Globals.TAG, data.toString());

        Log.d(Globals.TAG, "#####################");

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
