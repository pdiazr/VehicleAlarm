package es.gpsou.vehiclealarm;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Pedro on 08/01/2017.
 */

public class Globals {
    public static VehicleDeviceStatus vehicleDeviceStatus=new VehicleDeviceStatus();

    public static final String TAG = "VehicleAlarm";
    public static String PACKAGE_NAME = null;

    public static final String ACTIVATE_SENSOR="ACTIVATE_SENSOR";

    public static final String UPDATE_UI_INTENT_ACTION="UpdateUI";

    public static final String BT_UUID = "f05a4570-ad18-44ef-805a-1b6e4ea1e536";

    public static final String SENDER_ID = "1019104744841";
    public static final String CLIENT_ID = "audience:server:client_id:" + "1019104744841-s8q2330eijmkutv2f24k5lliuf5elqi9.apps.googleusercontent.com";
//    public static final String CLIENT_ID = "audience:server:client_id:" + "1019104744841-2baahjgjcf6a8nnv05stgtb6jrfrmuac.apps.googleusercontent.com";

    public static final String SAVED_STATUS = "SavedStatus";

    public static final String CONFIGURACION = "AppConfig";
    public static final String GOOGLE_ACCOUNT = "GoogleAccount";
    public static final String APP_MODE = "AppMode";
    public static final String BT_MAC = "BtMac";
    public static final String BT_CONNECT_TIMEOUT = "BtConnectTimeout";
    public static final String REMOTE_BT_MAC = "RemoteBtMac";
    public static final String FB_REGISTRATION_ID = "FirebaseId";
    public static final String REMOTE_FB_REGISTRATION_ID = "RemoteFirebaseId";

//    public static final String FB_GROUP_ID = "GroupId";

//    public static final String P2P_GROUP_ID = "GroupId";
    public static final String P2P_COMMIT_REGISTRATION_ID = "CommitRegistrationId";
    public static final String P2P_TO = "to";
    public static final String P2P_TTL = "ttl";
    public static final String P2P_OP = "Operation";
    public static final String P2P_OP_PAIRING_COMMIT = "PairingCommit";
    public static final String P2P_OP_PAiRING_COMMIT_RESULT = "PairingCommitResult";
    public static final String P2P_OP_TEST = "Test";
    public static final String P2P_OP_SENSOR_ALARM = "SensorAlarm";
    public static final String P2P_OP_GET_LOCATION = "GetLocation";
    public static final String P2P_OP_GET_LOCATION_RESULT = "GetLocationResult";
    public static final String P2P_OP_LOCATION_UPDATE = "LocationUpdate";
    public static final String P2P_OP_PARK = "Park";
    public static final String P2P_OP_PARK_RESET = "ParkReset";
    public static final String P2P_OP_ACTIVATE_TRACKING = "ActivateTracking";
    public static final String P2P_OP_DEACTIVATE_TRACKING = "DeactivateTracking";
    public static final String P2P_OP_GEOFENCING_ALERT = "GeofencingAlert";
    public static final String P2P_OP_REPLACE_REGISTRATION_ID = "ReplaceRegistrationId";
    public static final String P2P_OP_REMOVE_REGISTRATION_ID = "RemoveRegistrationId";
    public static final String P2P_OP_AUDIO_REQ = "AudioReq";
    public static final String P2P_OP_AUDIO_RESP = "AudioResp";
    public static final String P2P_OP_STOP_AUDIO = "StopAudio";
    public static final String P2P_OP_SWITCH_MONITORING = "SwitchMonitoring";
    public static final String P2P_LATITUDE = "Latitude";
    public static final String P2P_LONGITUDE = "Longitude";
    public static final String P2P_BATTERY = "Battery";
    public static final String P2P_PARKING = "Parking";
    public static final String P2P_LATITUDE_PARK = "LatitudePark";
    public static final String P2P_LONGITUDE_PARK = "LongitudePark";
    public static final String P2P_TIMESTAMP = "Timestamp";
    public static final String P2P_RESULT = "Result";
    public static final String P2P_DEST = "Destination";
    public static final String P2P_DEST_IN_VEHICLE = "InVehicle";
    public static final String P2P_DEST_MONITOR = "Monitor";
    public static final String P2P_TXT = "TEST";
    public static final String P2P_FB_REGISTRATION_ID = "FbRegistrationId";
    public static final String P2P_OLD_FB_REGISTRATION_ID = "OldFbRegistrationId";
    public static final String P2P_NEW_FB_REGISTRATION_ID = "NewFbRegistrationId";
/*    public static final String P2P_IPADDR = "IpAddr";
    public static final String P2P_UDP_PORT = "UdpPort"; */
    public static final String P2P_SDP = "SDP";
    public static final String P2P_STUN_SERVER = "StunServer";
    public static final String P2P_MONITORING_ACTIVATED = "MonitoringActivated";
    public static final String NULL = "NULL";
    public static final String TRUE = "TRUE";
    public static final String FALSE = "FALSE";

    public static final String IN_VEHICLE_MODE = "InVehicle";
    public static final String MONITORING_MODE = "Monitoring";

    public static final String NFC_SYNC_STRING = "OK";
    public static final String NFCBT_ERROR_STRING = "ERROR";
    public static final String NFCBT_BT_MAC = "NfcBtBtMac";
    public static final String NFCBT_FIREBASE_ID = "NfcBtFirebaseId";

    public static final long SENSOR_UPDATE_TIME=2000;

    public static final String GEOFENCE_ID="GeofenceId";
    public static final float PARK_RADIUS=100.0f;

    public static final int GEOFENCE_INTENT_REQUEST_CODE=1;
    public static final int LOCATION_UPDATE_INTENT_REQUEST_CODE=2;
    public static final String RESOLUTION_REQUIRED="ResolutionRequired";
    public static final String SWITCH_MONITORING="SwitchMonitoring";

    public static AtomicInteger msgId = new AtomicInteger();

    public static final String BUNDLE_KEY_LOCATION="BundleLocation";
    public static final String BUNDLE_LATITUDE="Latitude";
    public static final String BUNDLE_LONGITUDE="Longitude";
    public static final String BUNDLE_TIMESTAMP="Timestamp";
    public static final String BUNDLE_BATTERY="Battery";

    public static final int SENSOR_ALARM=1;
    public static final int GEOFENCE_TRANSITION=2;
    public static final int GET_LOCATION_RESULT=3;
    public static final int LOCATION_UPDATE=4;
    public static final int HIDE_LOCATION_UPDATE=5;
    public static final int MONITORING_ACTIVATED=6;
    public static final int PARK=7;
    public static final int AUDIO=8;
    public static final int AUDIO_ACTIVATED=9;
    public static final int MONITORING=10;

    public static final long LOCATION_UPDATE_INTERVAL=5000;
}
