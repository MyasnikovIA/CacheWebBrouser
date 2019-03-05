package ru.sgk.cachewebbrowser.AndroidLib;

import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.webkit.WebView;

import net.arnx.jsonic.JSON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ru.sgk.cachewebbrowser.MainActivity;

/**
 * Created by MyasnikovIA on 12.02.19.
 */
public class WifiAndroid {

    private  WifiManager manager;
    private static HashMap<String, Object> WifiActive = new HashMap<String, Object>();
    private static List<Object> WifiPoints = new ArrayList<Object>();

    private MainActivity parentActivity;
    private WebView webView;
    public WifiAndroid(MainActivity activity, WebView webViewPar)  {
        webView=webViewPar;
        parentActivity = activity;
        manager = (WifiManager) parentActivity.getSystemService(parentActivity.WIFI_SERVICE); // для включения WIFI
    }

    public String getIp() {
       WifiManager wifiMgr = (WifiManager) parentActivity.getSystemService(parentActivity.WIFI_SERVICE);
       WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
       int ip = wifiInfo.getIpAddress();
        return   Formatter.formatIpAddress(ip);
    }
    public String getWifiInfo() {
        try {
            android.net.wifi.WifiInfo wifiInfo = manager.getConnectionInfo();
            DhcpInfo dhcpInfo = manager.getDhcpInfo();
            WifiActive.put("AccessMAC", wifiInfo.getMacAddress());
            WifiActive.put("SSID", wifiInfo.getSSID());
            WifiActive.put("tBSSID", wifiInfo.getBSSID());
            WifiActive.put("RSSI", wifiInfo.getRssi());// уровень сигнала
            WifiActive.put("IP", convertIpAddress(dhcpInfo.ipAddress));
        } catch (Exception e) {

        }
        return JSON.encode(WifiPoints, false);
    }


    private String getWifiList() {
        HashMap<String, Object> oneWifi = new HashMap<String, Object>();
        try {
            // manager.setWifiEnabled(true);  //Включение WIFI
            for (ScanResult results : manager.getScanResults()) {
                oneWifi.put("SSID", results.SSID);
                oneWifi.put("BSSID", results.BSSID);
                oneWifi.put("Lavel", results.level);
                oneWifi.put("TypSec", results.capabilities);
                WifiPoints.add(oneWifi);
                //  WifiActive.put(results.BSSID,oneWifi);
            }
        } catch (Exception e) {

        }
        return JSON.encode(WifiPoints, false);
    }

    private String convertIpAddress(int ipAddress) {
        int ip0, ip1, ip2, ip3, tmp;
        ip3 = ipAddress / 0x1000000;
        tmp = ipAddress % 0x1000000;
        ip2 = tmp / 0x10000;
        tmp %= 0x10000;
        ip1 = tmp / 0x100;
        ip0 = tmp % 0x100;
        return String.format("%d.%d.%d.%d", ip0, ip1, ip2, ip3);
    }

public void on(){
    manager.setWifiEnabled(true);  //Включение WIFI
}
    public void off(){
        manager.setWifiEnabled(false);  //Включение WIFI
    }



}
