package ru.sgk.cachewebbrowser;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.app.Activity;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;

import net.arnx.jsonic.JSON;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ru.sgk.cachewebbrowser.AndroidLib.Android;
import ru.sgk.cachewebbrowser.AndroidLib.CameraAndroid;
import ru.sgk.cachewebbrowser.AndroidLib.GPSAndroid;
import ru.sgk.cachewebbrowser.AndroidLib.SensorAndroid;
import ru.sgk.cachewebbrowser.AndroidLib.WebServerAndroid;
import ru.sgk.cachewebbrowser.AndroidLib.WifiAndroid;
import ru.sgk.cachewebbrowser.Lib.ImageLib;

import static ru.sgk.cachewebbrowser.Lib.ImageLib.disableWebviewZoomControls;




/*
    Получиьт снимок с камеры
    <script>
        function Getpic(){
            var picSrc=Android.GetPicture('pic');
            document.getElementById("content").innerHTML= picSrc;
        }
    </script>
    <br> <Button onclick="Getpic();">Getpic()</Button>
    <br><img src="" id="pic"/>

    <br><img src="" id="pic2"/>
    <br> <Button onclick="document.getElementById('pic2').src=Android.GetCameraImageBase64(); ">GetpicBase64()</Button>



 */

public class MainActivity extends Activity   implements SurfaceHolder.Callback{

    public PowerManager.WakeLock wl= null;
    public WebView webView ;
    private Android andoid;
    private CameraAndroid webcamera;
    private GPSAndroid WebGps;
    private WifiAndroid WebWifi;
    private SensorAndroid WebSensor;

    private WebServerAndroid webServerAndroid;


    /// Сервис сьемка камеры
    /// https://stackoverflow.com/questions/10121660/how-to-record-video-from-background-of-application-android
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // webView = new WebView(this);
        // setContentView(webView);
        setContentView(R.layout.activity_main);
        webView= (WebView) findViewById(R.id.webView);
        webView.getSettings().setUserAgentString("Desktop");

        new LockOrientation(this).lock();

        webView.getSettings().setJavaScriptEnabled(true);

        andoid=new Android(this,webView);
        webView.addJavascriptInterface(andoid, "Android");

        webcamera=new CameraAndroid(this,webView);
        webView.addJavascriptInterface(webcamera, "Camera");

        WebSensor=new SensorAndroid(this,webView);
        webView.addJavascriptInterface(WebSensor, "Sensor");

        WebGps=new GPSAndroid(this,webView);
        webView.addJavascriptInterface(WebGps, "Gps");

        WebWifi=new WifiAndroid(this,webView);
        webView.addJavascriptInterface(WebWifi, "Wifi");

        webServerAndroid = new WebServerAndroid(this,webView);
        webView.addJavascriptInterface(webServerAndroid, "WebServer");


        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setSupportZoom(true);

        disableWebviewZoomControls(webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setAllowFileAccess(true);
        webView.loadUrl(andoid.StartPathFile+"html/index.html");
        // webView.loadUrl("javascript: var AppPhotoPouse=false;");
        // webView.loadUrl("file:///android_asset/html/index.htm");
        PowerManager pm = (PowerManager)getSystemService( Context.POWER_SERVICE);
        wl = pm.newWakeLock( PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE,"UMSE PowerTest");
        if (wl != null) {
            wl.acquire();
        }

    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == webcamera.CAMERA_PIC_REQUEST) {
             if (resultCode != RESULT_OK) {
                if (webcamera.ImageCameraId.length()>0 ){
                    webView.loadUrl("javascript: document.getElementById('"+webcamera.ImageCameraId+"').src=''; ");
                }
                return;
             }
                //2
                Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
                // <IMG src="data:image/jpeg;base64,/9j/4AAQSkZJR...IQd6QknrS54460h60BY//9k=">
               webcamera.CameraImageBase64= ImageLib.encodeImage(thumbnail);
               // mImage.setImageBitmap(thumbnail);
               ByteArrayOutputStream bytes = new ByteArrayOutputStream();
               thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
               // File file = new File(Environment.getExternalStorageDirectory()+File.separator + "image.jpg");
               File file = new File(webcamera.PicturePath);
               try {
                   file.createNewFile();
                   FileOutputStream fo = new FileOutputStream(file);
                   fo.write(bytes.toByteArray());
                   fo.close();
               } catch (IOException e) {
                  e.printStackTrace();
               }
            // String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            // String img=""+webcamera.PicturePath;
            if (webcamera.ImageCameraId.length()>0 ){

                StringBuffer sb=new StringBuffer("javascript: ");
                sb.append(" if (typeof "+webcamera.ImageCameraId+" === 'function'){ ");
                sb.append("   "+webcamera.ImageCameraId+"('"+webcamera.CameraImageBase64+"' ) ");
                //Input
                sb.append(" }else if ( (''+"+webcamera.ImageCameraId+")=='[object HTMLImageElement]'){  ");
                sb.append("    document.getElementById('"+webcamera.ImageCameraId+"').src=''; document.getElementById('"+webcamera.ImageCameraId+"').src='"+webcamera.CameraImageBase64+"'; ");
                //TextArea
                sb.append(" }else if ( (''+"+webcamera.ImageCameraId+")=='[object HTMLTextAreaElement]'){  ");
                sb.append("    document.getElementById('"+webcamera.ImageCameraId+"').value='"+webcamera.CameraImageBase64+"'; ");
                //!!!! div (переписать на вывод изображения задний фон в стиле) !!!!!
                sb.append(" }else if ( (''+"+webcamera.ImageCameraId+")=='[object HTMLDivElement]'){  ");
                // sb.append("    document.getElementById('"+webcamera.ImageCameraId+"').innerHTML='"+webcamera.CameraImageBase64+"'; ");
                sb.append(" document.getElementById('"+webcamera.ImageCameraId+"').style.backgroundImage = 'url(\""+webcamera.CameraImageBase64+"\")'; ");

                // VAR
                sb.append(" }else{");
                sb.append("   "+webcamera.ImageCameraId+"='"+webcamera.CameraImageBase64+"' ; ");
                sb.append(" }");
                webView.loadUrl(sb.toString());
            }
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) &&webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
      /*
        webView.evaluateJavascript("(function() { window.dispatchEvent(appResumeEvent); })();", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {

            }
        });
        */
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub

    }


}
