package ru.sgk.cachewebbrowser.AndroidLib;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.text.format.Formatter;
import android.util.Base64;
import android.webkit.WebView;
import android.widget.Spinner;

import java.io.File;
import java.util.HashMap;

import ru.sgk.cachewebbrowser.Lib.Sys;
import ru.sgk.cachewebbrowser.MainActivity;
import ru.sgk.cachewebbrowser.services.ServiceExample;

/**
 * Created by MyasnikovIA on 13.02.19.
 */
public class WebServerAndroid {

    private HashMap<String, String> Setup = new HashMap<String, String>(10, (float) 0.5);
    public static String ApplicationName;
    private String StartPathFile;

    private MainActivity parentActivity;
    private WebView webView;
    public WebServerAndroid (MainActivity activity, WebView webViewPar)  {
        webView=webViewPar;
        parentActivity = activity;
        ApplicationName=parentActivity.getApplicationInfo().loadLabel(parentActivity.getPackageManager()).toString();
        StartPathFile= Environment.getExternalStorageDirectory()+ File.separator+ApplicationName+File.separator+"html";
        if (!new File(StartPathFile).exists()){
            new File(StartPathFile).mkdirs();
        }

        Setup = Sys.readFile(parentActivity, "conf.ini");
        if (Setup.size() == 0) {
            String Author = new String(Base64.encode(("user" + ":" + "user").getBytes(), Base64.DEFAULT));
            Setup.put("UserPort", "8080");
            Setup.put("Author", Author);
            Setup.put("Interval", "3000");
            Setup.put("DefaultHost", "index.html");
            Setup.put("CharSet", "cp1251");
            Setup.put("IsAutorization", "0");
            Setup.put("StartPath", StartPathFile);
        }
    }


    public void Start(){
        Setup.put("run", "1");
        Sys.writeFile( parentActivity, "conf.ini", Setup);
        parentActivity.startService(new Intent( parentActivity.getBaseContext(), ServiceExample.class));
    }

    public void Stop(){
        Setup.put("run", "0");
        Sys.writeFile( parentActivity, "conf.ini", Setup);
        parentActivity.stopService(new Intent(parentActivity.getBaseContext(), ServiceExample.class));
    }

    public void SetHost(String HostName){
        Setup.put("DefaultHost", HostName);
        Sys.writeFile( parentActivity, "conf.ini", Setup);
    }
    public void SetPort(String HostPort){
        Setup.put("UserPort", HostPort);
        Sys.writeFile( parentActivity, "conf.ini", Setup);
    }
    public void SetChar(String CharSet){
        Setup.put("CharSet", CharSet);
        Sys.writeFile( parentActivity, "conf.ini", Setup);
    }
    public void SetAuthor(String UserName,String UserPass){
        String Author = new String(Base64.encode((UserName + ":" + UserPass).getBytes(), Base64.DEFAULT));
        Setup.put("Author", Author);
        Sys.writeFile(parentActivity, "conf.ini", Setup);
    }

    public void SetWebAuthor(Boolean isAuthor){
        if (isAuthor==true){
           Setup.put("IsAutorization", "1");
        }else{
            Setup.put("IsAutorization", "0");
        }
        Sys.writeFile( parentActivity, "conf.ini", Setup);
    }

    public void SetInterval(String Interval){
        Setup.put("Interval", Interval);
        Sys.writeFile(parentActivity, "conf.ini", Setup);
    }

    public void StartBrowser(){
        WifiManager wifiMgr1 = (WifiManager) parentActivity.getSystemService(parentActivity.WIFI_SERVICE);
        WifiInfo wifiInfo1 = wifiMgr1.getConnectionInfo();
        int ip = wifiInfo1.getIpAddress();
        String ipAddress = Formatter.formatIpAddress(ip);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + ipAddress + ":" +  Setup.get("UserPort")));
        parentActivity.startActivity(browserIntent);
    }
    public void SetStartPath(String StartPathFile){
      Setup.put("StartPath", StartPathFile);
      Sys.writeFile(parentActivity, "conf.ini", Setup);
    }

}
