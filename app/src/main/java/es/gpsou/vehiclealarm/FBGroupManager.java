package es.gpsou.vehiclealarm;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created by Pedro on 08/12/2017.
 */

public class FBGroupManager {
    public static Account account=null;

    public static String createGroup(Context mContext, String localFirebaseId, String remoteFirebaseId) throws IOException, GoogleAuthException, JSONException {
        String groupId;
        String idToken;

        if (account == null)
            account = getAccount(mContext);
        idToken = GoogleAuthUtil.getToken(mContext, account, Globals.CLIENT_ID);

        Log.d(Globals.TAG, "Creamos nuevo Id de grupo de Firebase");

        groupId = addNotificationKeys(Globals.SENDER_ID, account.name, localFirebaseId, remoteFirebaseId, idToken);

//        groupId = addNotificationKey(Globals.SENDER_ID, account.name, localFirebaseId, idToken);
//        addNotificationKey(Globals.SENDER_ID, account.name, remoteFirebaseId, idToken);

        return (groupId);
    }

    public static void replaceFirebaseId (Context mContext, String oldFirebaseId, String newFirebaseId) throws GoogleAuthException, IOException, JSONException {
        if(account==null)
            account=getAccount(mContext);
        String idToken = GoogleAuthUtil.getToken(mContext, account, Globals.CLIENT_ID);

        removeNotificationKey(Globals.SENDER_ID, account.name, oldFirebaseId, idToken);
        addNotificationKey(Globals.SENDER_ID, account.name, newFirebaseId, idToken);
    }

    public static void removeFirebaseId (Context mContext, String firebaseId) throws GoogleAuthException, IOException, JSONException {
        if(account==null)
            account=getAccount(mContext);
        String idToken = GoogleAuthUtil.getToken(mContext, account, Globals.CLIENT_ID);

        removeNotificationKey(Globals.SENDER_ID, account.name, firebaseId, idToken);
    }

    static String addNotificationKey(
            String senderId, String name, String registrationId, String idToken)
            throws IOException, JSONException {
        URL url = new URL("https://fcm.googleapis.com/fcm/googlenotification");
//        URL url = new URL("https://android.googleapis.com/gcm/googlenotification");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);

        // HTTP request header
        con.setRequestProperty("project_id", senderId);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestMethod("POST");
        con.connect();

        // HTTP request
        JSONObject data = new JSONObject();
        data.put("operation", "add");
        data.put("notification_key_name", name);
        data.put("registration_ids", new JSONArray(Arrays.asList(registrationId)));
        data.put("id_token", idToken);
        Log.d(Globals.TAG, "Add Notification KEY");
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

    }

    static String addNotificationKeys(
            String senderId, String name, String localId, String remoteId, String idToken)
            throws IOException, JSONException {
        URL url = new URL("https://android.googleapis.com/gcm/googlenotification");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);

        // HTTP request header
        con.setRequestProperty("project_id", senderId);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestMethod("POST");
        con.connect();

        // HTTP request
        JSONObject data = new JSONObject();
        data.put("operation", "add");
        data.put("notification_key_name", name);
        data.put("registration_ids", new JSONArray(Arrays.asList(localId, remoteId)));
        data.put("id_token", idToken);
        Log.d(Globals.TAG, "Add Notification KEY");
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

    }

    static void removeNotificationKey(
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
        con.connect();

        // HTTP request
        JSONObject data = new JSONObject();
        data.put("operation", "remove");
        data.put("notification_key_name", name);
        data.put("registration_ids", new JSONArray(Arrays.asList(registrationId)));
        data.put("id_token", idToken);
        Log.d(Globals.TAG, "Remove Notification KEY");
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
    }

    public static Account getAccount(Context mContext) {
        if(account!=null)
            return(account);

        try {
            Account[] accounts = AccountManager.get(mContext).
                    getAccountsByType("com.google");
            if (accounts.length == 0) {
                return null;
            }

            SharedPreferences settings=mContext.getSharedPreferences(Globals.CONFIGURACION, 0);
            String name = settings.getString(Globals.GOOGLE_ACCOUNT, null);

            if(name!=null) {
                for(int i=0; i<accounts.length; i++) {
                    if(name.compareTo(accounts[i].name)==0)
                        account=accounts[i];
                }
            }

            if(account==null)
                account=accounts[0];

            return account;
        } catch(SecurityException e) {
            return null;
        }
    }

}
