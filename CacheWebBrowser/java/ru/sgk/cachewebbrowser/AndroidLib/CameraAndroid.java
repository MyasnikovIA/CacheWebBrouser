package ru.sgk.cachewebbrowser.AndroidLib;

import android.content.Intent;
import android.os.Environment;
import android.webkit.WebView;

import java.io.File;

import ru.sgk.cachewebbrowser.MainActivity;

/**
 * Created by MyasnikovIA on 06.02.19.
 * https://examples.javacodegeeks.com/android/core/hardware/camera-hardware/android-camera-example/
 * http://adblogcat.com/camera-api-simple-way-to-take-pictures-and-save-them-on-sd-card/
 *
 *  <script>
 *  function ResultPic(picSrc){
 *      document.getElementById("pic").src=picSrc;
 *  }
 *  function Getpic(){
 *      // V1
 *      // var picSrc=Camera.GetPhoto('pic');
 *      // document.getElementById("content").innerHTML= picSrc;
 *
 *      /// V2
 *      // Camera.GetPhoto('ResultPic');
 *
 *      /// V3
 *      Camera.GetPhoto('logPic');
 *  }
 *  </script>
 *  <br> <Button onclick="Getpic();">Getpic()</Button>
 *  <br> <div id="logPic" >-----<br />-----</div>
 *  <br> <img src="" id="pic"/>
 *  <br> <img src="" id="pic2"/>
 *  <br> <Button onclick="document.getElementById('pic2').src=Camera.GetPhotoaImageBase64(); ">GetpicBase64()</Button>
 *
 *
 */
public class CameraAndroid {


    public String ImageCameraId=""; // ID HTML тэга img
    public String CameraImageBase64=""; // ID HTML тэга img
    public static final int CAMERA_PIC_REQUEST = 1111;
    private String ApplicationName;
    public String StartPathFile="";
    public String PicturePath="";

    private MainActivity parentActivity;
    private WebView webView;
    public CameraAndroid(MainActivity activity, WebView webViewPar)  {
        webView=webViewPar;
        parentActivity = activity;
        StartPathFile="file://"+ Environment.getExternalStorageDirectory()+ File.separator+ApplicationName+File.separator;
        PicturePath=Environment.getExternalStorageDirectory()+File.separator+ApplicationName+File.separator+"Picture"+File.separator;
        File dir = new File(PicturePath);
        if (!dir.exists()){  dir.mkdirs(); }
        PicturePath=PicturePath+"TmpCamPic.jpg";
    }
    public String GetPhoto(String ImageId){
        CameraImageBase64="";
        ImageCameraId=ImageId;
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        parentActivity.startActivityForResult(intent, CAMERA_PIC_REQUEST);
        return PicturePath;
    }
    public String GetPhotoaImageBase64(){
        return  CameraImageBase64;
    }

}
