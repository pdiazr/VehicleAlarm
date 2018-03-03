package es.gpsou.vehiclealarm;

/**
 * Created by Pedro on 13/05/2017.
 */

public class VehicleDeviceStatus {
    public boolean deadThreadFlag=true;
    public double latitude=0;
    public double longitude=0;
    public long locationTs=0;
    public boolean sensorAlarm=false;
    public long sensorAlarmTs=0;
    public boolean geofenceAlarm=false;
    public long geofenceAlarmTs=0;
    public int batteryLevel=-1;
    public double latitude_park=0;
    public double longitude_park=0;
    public int parkingStatus=0;
    public boolean trackingActive=false;
    public boolean audioOn=false;
    public boolean monitoringActivated=false;
}
