package ru.sgk.cachewebbrowser.AndroidLib;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.webkit.WebView;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;



import net.arnx.jsonic.*;

import ru.sgk.cachewebbrowser.MainActivity;

import static ru.sgk.cachewebbrowser.AndroidLib.Android.Alert;

//класс для работы с сенсорами
// https://startandroid.ru/ru/uroki/vse-uroki-spiskom/287-urok-137-sensory-uskorenie-orientatsija.html
//

/**
 *    <pre>
 *        <div id="content" >-----</div>
 *        <div id="log" >-----</div>
 *      </pre>
 *    <script>
 *
 *   /// v1
 *   /// Получить список устройств в строковом виде
 *   document.getElementById("content").innerHTML=Sensor.list();
 *
 *   /// V2
 *   var SensList=null;
 *   Sensor.list('SensList');
 *   /// В другой функции можно обратится к 'SensList' как к объекту
 *
 *   /// V3
 *   function ViewSensorList(SensListObj){
 *       document.getElementById("log").innerHTML+="\r\n<br>"+JSON.stringify(SensListObj);
 *   }
 *   Sensor.list('ViewSensorList'); // Запустить асинхронно получение списка сенсоров на устройстве
 *
 *   /// ------------------------------------------------ \\\
 *   ///   Запустить слушателя устройства (Асинхронно)    \\\
 *   /// ------------------------------------------------ \\\
 *
 *   // Обработчик изменения состояния сенсора
 *    var TestFunCalBackSensor=function(obj)
 *     {
 *        document.getElementById("content").innerHTML=obj;
 *        document.getElementById("content").innerHTML+='\r\n<br>'+JSON.stringify(obj);
 *     }
 *   /// V1 запустить все устройства
 *    Sensor.Start('','TestFunCalBackSensor');
 *
 *   /// V2 запустить одно устройство
 *   // Sensor.Start('TYPE_ACCELEROMETER','TestFunCalBackSensor');
 *
 *   /// Получить значение по запросу пользователя
 *   function GetSensor(){
 *      document.getElementById("content").innerHTML= Sensor.GetSensorValue();
 *   }
 *   /// ------------------------------------------------ \\\
 *   ///   Остановить слушателя устройства (Асинхронно)    \\\
 *   /// ------------------------------------------------ \\\
 *   // Sensor.Stop();                    /// остановить все устройства
 *   // Sensor.Stop('TYPE_ACCELEROMETER') /// остановить одно устройство
 *
 *  </script>
 *  <br> <Button onclick="GetSensor(); ">GetSensor()</Button>
 *
 */
public class SensorAndroid {

	private Integer x = 0;

    private String SensorAndroidCalBackFunctionFun="";
    private SensorManager sensorManager;
    private List<Sensor> sensors=null;

    private Map<String,Sensor>SensorTab=new HashMap<String,Sensor>();
    private Map<String,float[]>SensorValue=new HashMap<String,float[]>();

    private Map<String,String>FunctionCalBackValue=new HashMap<String,String>();

    private StringBuilder sb = new StringBuilder();
    private WebView webView=null;
    private MainActivity activity;

    public SensorAndroid(MainActivity activityPar, WebView webViewPar)  {
        webView=webViewPar;
        activity=activityPar;
        sensorManager = (SensorManager)activity.getSystemService(activity.SENSOR_SERVICE);
        sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensors) {
            String Name=GetSensorName(sensor);
            if (Name.length()==0){continue;}
            SensorTab.put( Name , sensorManager.getDefaultSensor(sensor.getType()));
        }
    }


    private String GetSensorName(Sensor sensor){
        String Name;
        if (sensor.getType()==1){
            Name="TYPE_ACCELEROMETER";
        }else if(sensor.getType()==2){
            Name="TYPE_MAGNETIC_FIELD";
        }else if(sensor.getType()==3){
            Name="TYPE_ORIENTATION";
        }else if(sensor.getType()==4){
            Name="TYPE_GYROSCOPE";
        }else if(sensor.getType()==5){
            Name="TYPE_LIGHT";
        }else if(sensor.getType()==6){
            Name="TYPE_PRESSURE";
        }else if(sensor.getType()==7){
            Name="TYPE_TEMPERATURE";
        }else if(sensor.getType()==8){
            Name="TYPE_PROXIMITY";
        }else if(sensor.getType()==9){
            Name="TYPE_GRAVITY";
        }else if(sensor.getType()==10){
            Name="TYPE_LINEAR_ACCELERATION";
        }else if(sensor.getType()==11){
            Name="TYPE_ROTATION_VECTOR";
        }else if(sensor.getType()==12){
            Name="TYPE_RELATIVE_HUMIDITY";
        }else if(sensor.getType()==13){
            Name="TYPE_AMBIENT_TEMPERATURE";
        }else{
           // Name=sensor.getName();
            Name="";
        }
        return  Name;
    }

        public Boolean Start(){
           return Start("","");
        }
        public Boolean Start(String SensorName){
           return Start(SensorName,"");
        }

        public Boolean Start(String SensorName,String SensorAndroidCalBackFunction){
            if (SensorAndroidCalBackFunction==null){SensorAndroidCalBackFunction="";}
            boolean res=false;
            for(Map.Entry<String, Sensor> entry : SensorTab.entrySet()) {
                String key = entry.getKey();
                if (SensorName.length()>0){
                  if (key.indexOf(SensorName)<0){
                      continue;
                  }
                }
                res=true;
                Sensor value = entry.getValue();
                sensorManager.registerListener(listenerLight, value    , SensorManager.SENSOR_DELAY_NORMAL);
                String FunJSName= (SensorAndroidCalBackFunction+key).replaceAll("_", "");
                FunctionCalBackValue.put(key ,FunJSName);
                if (SensorAndroidCalBackFunction.length()>0) {
                    SensorAndroidCalBackFunctionFun=SensorAndroidCalBackFunction;
                    webView.loadUrl("javascript: "+ FunJSName+" = "+ SensorAndroidCalBackFunctionFun+"");
                }else{
                    SensorAndroidCalBackFunctionFun="";
                    webView.loadUrl("javascript: "+ FunJSName+" = function(txt){} ");
                }
            }
            return res;
        }


    protected boolean Stop() {
        return Stop("");
    }

    protected boolean Stop(String SensorName) {
        boolean res=false;
        for(Map.Entry<String, Sensor> entry : SensorTab.entrySet()) {
            String key = entry.getKey();
            if (SensorName.length()>0){
                if (key.indexOf(SensorName)<0){
                    continue;
                }
            }
            Sensor value = entry.getValue();
            sensorManager.unregisterListener(listenerLight, value);
            String FunNameTmp=(String) FunctionCalBackValue.get(key);
            if(FunNameTmp.length()>0){
               webView.loadUrl("javascript: " + FunNameTmp + "=null;");
            }
            FunctionCalBackValue.remove(key);
            res=true;
        }
        return res;
    }

    public String list(String FunctionCalBack){
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        int ind=0;
        for (Sensor sensor : sensors) {
            String Name=GetSensorName(sensor);
            if (Name.length()==0){continue;}
            ind++;
            if (ind>1){sb.append(",");}
            sb.append("{")
                    .append("\"Name\":\"" + Name + "\"")
                    .append(",\"SystemName\":\""+sensor.getName()+"\"")
                    .append(",\"Type\":").append(sensor.getType()+"")
                    .append(",\"Nendor\":\"").append(sensor.getVendor()+"\"")
                    .append(",\"Version\":\"").append(sensor.getVersion()+"\"")
                    .append(",\"Max\":").append(sensor.getMaximumRange()+"")
                    .append(",\"Power\":").append(sensor.getPower()+"")
                    .append(",\"MinDelay\":\"").append(sensor.getMinDelay() + "\"")
                    .append(",\"Resolution\":\"").append(sensor.getResolution() + "\"");
            sb.append("}");
        }
        sb.append("]");
        if (FunctionCalBack.length()>0){
            // Alert("javascript: "+FunctionCalBack+"=" + sb.toString(),activity);
            webView.loadUrl("javascript: if (typeof "+FunctionCalBack+" === 'function'){ "+FunctionCalBack+"(" + sb.toString() +");  }else{   "+FunctionCalBack+"=" + sb.toString() +" };"  );
        }
        return sb.toString();
    }

    public String list(){
        return list("");
    }



     String format(float values[]) {
        //  return String.format("[%1$.1f,%2$.1f,%3$.1f]", values[0], values[1], values[2]);
        return "["+Float.toString(values[0])+","+Float.toString(values[1])+","+Float.toString(values[0])+"]";
    }
    public String GetSensorValue() {
        return  JSON.encode(SensorValue, false);
    }


    SensorEventListener listenerLight = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            x++;
            if (x%1 == 0) {
                String Name=GetSensorName(sensorEvent.sensor);
                if (Name.length()>0){
                   StringBuilder paramsStr = new StringBuilder();
                   float[] values = new float[3];
                   for (int i = 0; i < 3; i++) {values[i] = sensorEvent.values[i];}
                   SensorValue.put(Name,values);
                }
                String FunNameTmp=(String) FunctionCalBackValue.get(Name);
                webView.loadUrl("javascript: if (typeof "+FunNameTmp+" === 'function'){ "+FunNameTmp+"(" + JSON.encode(SensorValue, false) +");  }else{   "+FunNameTmp+"=" + JSON.encode(SensorValue, false) +" };"  );
                x = 0;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor arg0, int arg1) {
            // TODO Auto-generated method stub
        }
    };

    private long lastUpdate2 = 0;
    public static float Speed = 0;
    private float last_x, last_y, last_z;
    public void getSpeed(SensorEvent sensorEvent) {
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];
        if (lastUpdate2 != 0) {
            long diffTime = (System.currentTimeMillis() - lastUpdate2);
            Speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime;// * 10000;;
            last_x = x;
            last_y = y;
            last_z = z;
        }
        lastUpdate2 = System.currentTimeMillis();
    }




}
