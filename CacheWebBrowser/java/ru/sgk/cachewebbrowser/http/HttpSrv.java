package ru.sgk.cachewebbrowser.http;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.sgk.cachewebbrowser.MainActivity;
import ru.sgk.cachewebbrowser.R;


/**
 * Created by myasnikov on 14.01.16.
 */
public class HttpSrv {

    //-------------------------------------------------------------

    //public Android(MainActivity activity, WebView webViewPar)  {
    public HttpSrv(MainActivity activity, WebView webViewPar) {
        this.activity = activity;
        this.webView  = webViewPar;
    }
    private static String ApplicationName;
    private static MainActivity activity;
    private WebView webView;
   // private static Context context;
    private static int numComp = 0;
    private static String IPmac = "";
    // private static String rootPath = "mnt/sdcard/ApacheAndroid/demo";
    private static String startFile = "index.htm";
    public static int port = 9090;
    public static int TimeOut = 300;
    private boolean process = false;
    /**
     * список IP адресов которым ограничен доступ
     */
    public static Hashtable<String, String> BlackList = new Hashtable<String, String>(10, (float) 0.5);
    /**
     * списко текстовых фраз, для фильтрации
     */
    public static List<String> BlockText = new ArrayList<String>();
    public static Hashtable<String, String> ClientList = new Hashtable<String, String>(10, (float) 0.5);

    SharedPreferences settings;

    //            String authString = "user" + ":" + "123";
    //            code = new String(Base64.encode(authString.getBytes()));

    /**
     * Запуск вэб сервера<br>
     * Пример: new HTTP().start("C:\\AppServ\\www",
     * "9090","C:\\AppServ\\www\\lib", true);
     *
     */
    public void Start() {

        settings = activity.getSharedPreferences("HttpSrv", 0);
        ApplicationName=activity.getApplicationInfo().loadLabel(activity.getPackageManager()).toString();
        this.port=settings.getInt("UserPort", 9090);
        this.startFile=settings.getString("DefaultHost", "index.html");
        SharedPreferences.Editor ed = settings.edit();
        ed.putInt("WebServerRun", 1);
        ed.commit();
        process = true;
        this.TimeOut = this.TimeOut * 1000;
        // File filesLibPath = new File((new File(HttpSrv.class.getProtectionDomain().getCodeSource().getLocation().getPath())).getParent() + "\\lib");
        Thread myThready;
        myThready = new Thread(new Runnable() {
            public void run() {
                try {
                    ServerSocket ss = new ServerSocket(HttpSrv.port);
                    while (process == true) {
                        numComp++;
                        Socket socket = ss.accept();
                        new Thread(new SocketProcessor(socket)).start();
                    }
                } catch (Exception ex) {
                    Logger.getLogger(HttpSrv.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Throwable ex) {
                    Logger.getLogger(HttpSrv.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        });
        myThready.start();    //Запуск потока

    }


    /**
     * Остановить сервер
     */
    public void Stop() {
        process = false;
        settings = activity.getSharedPreferences("HttpSrv", 0);
        SharedPreferences.Editor ed = settings.edit();
        ed.putInt("WebServerRun", 0);
        ed.commit();
    }

    private static class SocketProcessor implements Runnable {
        File sdcard;
        private Hashtable<String, Object> Json = new Hashtable<String, Object>(10, (float) 0.5);
        private Hashtable<String, Object> JsonParam = new Hashtable<String, Object>(10, (float) 0.5);
        private static Object StandardLog;
        private Socket socket;
        private InputStream is;
        private OutputStream os;
        private String contentZapros = "";

        private SocketProcessor(Socket socket) throws Throwable {
            this.socket = socket;
            this.socket.setSoTimeout(TimeOut);
            this.is = socket.getInputStream();
            this.os = socket.getOutputStream();
            Json.clear();
            JsonParam.clear();

            String Adress = socket.getRemoteSocketAddress().toString();
            Json.put("RemoteIPAdress", Adress);
            Adress = Adress.split(":")[0];
            Adress = Adress.replace("/", "");
            InetAddress address = InetAddress.getByName(Adress);
            sdcard = Environment.getExternalStorageDirectory();
            String mediaState = Environment.getExternalStorageState();
            if (mediaState.equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
                sdcard = new File(Environment.getExternalStorageDirectory() + "/"+ApplicationName+"/html/");
            } else {
                //  /data/data/"your app package name "
                sdcard = new File("/data/data/" + HttpSrv.class.getPackage().toString().replace("package ", "") + "/html/");
            }
            if (!sdcard.exists()) {
                sdcard.mkdirs();
            }


        }

        public void run() {
            try {
                PrintStream out = new PrintStream(os);
                System.setOut(out);
                System.setErr(out);

                readInputHeaders();
            } catch (Throwable t) {
            } finally {
                try {
                    socket.close();
                } catch (Throwable t) {
                    /*do nothing*/
                }
            }
        }

        private List<String> param = new ArrayList<String>();
        private byte[] POST = new byte[0];
        private String Koderovka = "";
        //  private String getCommand = "";
        private String getCmd = "";

        /**
         * Чтение входных данных от клиента
         *
         * @throws java.io.IOException
         */
        private void readInputHeaders() throws IOException {
            //  FileWriter outLog = new FileWriter(rootPath + "\\log.txt", true); //the true will append the new data
            //  outLog.write("add a line\n");//appends the string to the file
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            contentZapros = "";
            StringBuffer sbInData = new StringBuffer();
            int numLin = 0;
            InputStreamReader isr = new InputStreamReader(is);
            int charInt;
            char[] charArray = new char[1024];
            // Читаем заголовок
            StringBuffer sb = new StringBuffer();
            StringBuffer sbTmp = new StringBuffer();
            while ((charInt = isr.read()) > 0) {
                if (socket.isConnected() == false) {
                    return;
                }
                //    os.write((char) charInt);
                //    outLog.write((char) charInt);
                sbTmp.append((char) charInt);
                if (sbTmp.toString().indexOf("\n") != -1) {
                    // если в первой строке невстречается слово GET или POST, тогда отключаем соединение
                    if (sb.toString().split("\n").length == 1) {
                        int res = sb.toString().indexOf("GET");
                        if (res == -1) {
                            res = sb.toString().indexOf("POST");
                            if (res == -1) {
                                is.close();
                                os.close();
                                socket.close();
                                return;
                            }
                        }
                    }
                    if (sbTmp.toString().length() == 2) {
                        break; // чтение заголовка окончено
                    }
                    sbTmp.setLength(0);
                }
                sb.append((char) charInt);
            }

            // FileWriter outLog = new FileWriter("D:\\HtmlServer_012\\LOG!!!!.txt", true); //the true will append the new data
            // outLog.write(sb.toString() + "\r\n-------------\r\n");
            if (sb.toString().indexOf("Content-Length: ") != -1) {
                String sbTmp2 = sb.toString().substring(sb.toString().indexOf("Content-Length: ") + "Content-Length: ".length(), sb.toString().length());
                String lengPostStr = sbTmp2.substring(0, sbTmp2.indexOf("\n")).replace("\r", "");
                int LengPOstBody = Integer.valueOf(lengPostStr);
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                while ((charInt = isr.read()) > 0) {
                    if (socket.isConnected() == false) {
                        return;
                    }
                    // outLog.write((char) charInt);
                    buffer.write((char) charInt);
                    LengPOstBody--;
                    //  outLog.write(LengPOstBody+"\r\n");
                    if (LengPOstBody == 0) {
                        break;
                    }
                }
                buffer.flush();
                POST = buffer.toByteArray();
                Json.put("PostBodyText", new String(buffer.toByteArray()));
                Json.put("PostBodyByte", buffer.toByteArray());
            }
            // outLog.close();
            int indLine = 0;
            for (String TitleLine : sb.toString().split("\r\n")) {
                indLine++;
                if (indLine == 1) {
                    TitleLine = TitleLine.replaceAll("GET /", "");
                    TitleLine = TitleLine.replaceAll("POST /", "");
                    TitleLine = TitleLine.replaceAll(" HTTP/1.1", "");
                    TitleLine = TitleLine.replaceAll(" HTTP/1.0", "");
                    contentZapros = java.net.URLDecoder.decode(TitleLine, "UTF-8");
                    Json.put("ContentZapros", contentZapros);
                    if (contentZapros.indexOf("?") != -1) {
                        String tmp = contentZapros.substring(0, contentZapros.indexOf("?") + 1);
                        String param = contentZapros.replace(tmp, "");
                        getCmd = param;
                        Json.put("ParamAll", param);
                        int indParam = 0;
                        for (String par : param.split("&")) {
                            String[] val = par.split("=");
                            if (val.length == 2) {
                                val[0] = java.net.URLDecoder.decode(val[0], "UTF-8");
                                val[1] = java.net.URLDecoder.decode(val[1], "UTF-8");
                                Json.put(val[0], val[1]);
                                val[0] = val[0].replace(" ", "_");
                                Json.put(val[0], val[1]);
                                JsonParam.put(val[0], val[1]);
                                if (!val[0].equals("method")) {
                                    this.param.add(val[1]);
                                }
                            } else {
                                indParam++;
                                val[0] = java.net.URLDecoder.decode(val[0], "UTF-8");
                                Json.put("Param" + String.valueOf(indParam), val[0]);
                                this.param.add(val[0]);
                                JsonParam.put("Param" + String.valueOf(indParam), val[0]);
                            }
                        }
                        contentZapros = tmp.substring(0, tmp.length() - 1);//.toLowerCase()
                    }
                    Json.put("Zapros", contentZapros);
                    Json.put("RootPath", sdcard.getAbsolutePath());
                    Json.put("AbsalutZapros", sdcard.getAbsolutePath() + "/" + contentZapros);
                } else {
                    if (TitleLine == null || TitleLine.trim().length() == 0) {
                        break;
                    }
                    if (TitleLine.split(":").length > 0) {
                        String val = TitleLine.split(":")[0];
                        val = val.replace(" ", "_");
                        Json.put(val, TitleLine.replace(TitleLine.split(":")[0] + ":", ""));
                    }
                    if (TitleLine.indexOf("Authorization:") == 0) {
                        //Authorization: Basic dXNlcjoxMjM=
                        String coderead = TitleLine.replaceAll("Authorization: Basic ", "");
                        Json.put("Author", TitleLine.replaceAll("Authorization: Basic ", ""));
                    }
                }
            }
            //
            // кодировка входных данных
            if (Json.containsKey("Content-Type") == true) {
                // Content-Type: text/html; charset=windows-1251
                if (Json.get("Content-Type").toString().split("charset=").length == 2) {
                    Json.put("Charset", Json.get("Content-Type").toString().split("charset=")[1]);
                }
            }
            // Парсим Cookie если он есть
            if (Json.containsKey("Cookie") == true) {
                String Cookie = Json.get("Cookie").toString();
                Cookie = Cookie.substring(1, Cookie.length());// убираем лишний пробел сначала строки
                for (String elem : Cookie.split("; ")) {
                    String[] val = elem.split("=");
                    Json.put(val[0], val[1]);
                    val[0] = val[0].replace(" ", "_");
                    Json.put(val[0], val[1]);
                    JsonParam.put(val[0], val[1]);
                }
            }
            //  PrintWriter pw = new PrintWriter(new FileWriter("C:\\Intel\\srvLogInData.xml"));
            //  pw.write(sb.toString());
            //  pw.close();
            sb.setLength(0);
            if (Json.containsKey("RemoteIPAdress") == false) {
                PrintStream out = new PrintStream(os);
                System.setOut(out);
                System.setErr(out);
                System.out.println("Your IP- address is not defined, in this regard, there is a suspicion that you are the attacker .\n"
                        + "You have not yet added to the blacklist , but not for long .\n"
                        + "Good luck .\r\n");
                System.out.println("Ваш IP адрес не определен, в связи с этим есть подозрение, что вы злоумышленник.\n"
                        + "Вы еще не добавлены в черный список, но это ненадолго.\n"
                        + "Желаю удачи.\r\n");
                is.close();
                os.close();
                socket.close();
                return;
            }
            if (Json.get("Zapros").toString().length() < 2) {
                Json.put("Zapros", startFile);
                Json.put("AbsalutZapros", sdcard.getAbsolutePath() + "/" + startFile);
            }

            // Отправить иконку приложения
            if (Json.get("Zapros").toString().equals("favicon.ico")) {
                Bitmap bmp = BitmapFactory.decodeResource(activity.getResources(), R.drawable.ic_launcher);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                os.write(("HTTP/1.1 200 OK\r\n"
                        + "Content-Type: image/png; charset=utf-8\r\n"
                        + "Connection: close\r\n"
                        + "Server: HTMLserver\r\n\r\n").getBytes());
                os.flush();
                os.write(byteArray, 0, byteArray.length);
                os.flush();
                return;
            }

            CreateCompId();


            File pageFile = new File(sdcard.getAbsolutePath() + "/" + Json.get("Zapros"));
            if (pageFile.exists() && !pageFile.isDirectory()) {
                sendRawFile(pageFile);
            } else {
                if (sendContentProvidr(Json.get("Zapros").toString()) == false) {
                    sendInfo();
                }
            }
            os.close();
            is.close();

        }


        /**
         * Найти провайдера по имени, и если он есть, тогда запустить его
         *
         * @param ProvierName
         * @return
         */
        private boolean sendContentProvidr(String ProvierName) {
            boolean isContentOk = false;
            for (PackageInfo pack : activity.getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS)) {
                ProviderInfo[] providers = pack.providers;
                if (providers != null) {
                    for (ProviderInfo provider : providers) {
                        String providerString = provider.authority;
                        if (providerString != null) {
                            String providerLow = providerString.toLowerCase();
                            String zapr = ProvierName.toLowerCase();
                            if (providerLow.equals(zapr)) {
                                ContentResolver cr = activity.getContentResolver();
                                Uri CONTACT_URI = Uri.parse("content://" + providerString);
                                StringBuffer headClient = new StringBuffer();
                                Bundle Head = new Bundle();
                                String cur = "\n";
                                String curRavno = "=";
                                for (String key : Json.keySet()) {
                                    Head.putString(key, Json.get(key).toString());
                                    headClient.append(key + curRavno + Json.get(key).toString() + cur);
                                }
                                if (Json.containsKey("PostBodyByte")) {
                                    Head.putByteArray("PostBodyByte", POST);
                                }
                                if (Json.containsKey("CharSet")) {
                                    Head.putString("CharSet", (String) Json.get("CharSet"));
                                } else {
                                    Head.putString("CharSet", "utf-8");
                                }
                                Bundle callRes = cr.call(CONTACT_URI, providerString, headClient.toString(), Head);
                                if (callRes != null) {
                                    try {

                                        byte[] res = callRes.getByteArray("return");
                                        if (res != null) {
                                            os.write(res);
                                            os.flush();
                                            return true;
                                        } else {
                                            os.write(callRes.toString().getBytes());
                                            os.flush();
                                        }
                                    } catch (IOException e) {
                                        System.err.print("Error:" + e.toString());
                                    }
                                }
                                isContentOk = true;
                                // System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
                            }
                        }
                    }
                }
            }
            return isContentOk;
        }

        private void sendInfo() {
            try {
                os.write("HTTP/1.1 200 OK\r\n".getBytes());
                os.write("Content-Type: text/html; charset=utf-8\r\n".getBytes());
                os.write("Connection: close\r\n".getBytes());
                os.write("Server: HTMLserver\r\n".getBytes());
                os.write("\r\n\r\n".getBytes());
                os.flush();

                File pageFile = new File(sdcard.getAbsolutePath() + "/" + Json.get("Zapros"));
                os.write((pageFile.getAbsolutePath()).getBytes());

                os.write(("<br>").getBytes());
                os.write(("<br>").getBytes());
                String ProvierName = Json.get("Zapros").toString();
                os.write(("ProvierName - " + ProvierName + "<br>").getBytes());
                os.write(("<br>").getBytes());
                os.write(("<br>===================").getBytes());
                for (String key:Json.keySet()){
                    os.write(("<br>"+key+" = "+Json.get(key)).getBytes());
                }
                os.write(("<br>===================").getBytes());
                os.write(("<br>").getBytes());
                Properties p = System.getProperties();
                os.write((Json.toString() + "<br>").getBytes());
                os.write(("<br>").getBytes());
                os.write(("<br>").getBytes());
                os.write(("<br>").getBytes());
                os.write(("<br>").getBytes());
                os.write(("Java Runtime Environment version: " + p.getProperty("java.version") + "<br>").getBytes());
                os.write(("Java Runtime Environment vendor: " + p.getProperty("java.vendor") + "<br>").getBytes());
                os.write(("Java vendor URL: " + p.getProperty("java.vendor.url") + "<br>").getBytes());
                os.write(("Java installation directory: " + p.getProperty("java.home") + "<br>").getBytes());
                os.write(("Java Virtual Machine specification version: " + p.getProperty("java.vm.specification.version") + "<br>").getBytes());
                os.write(("Java Virtual Machine specification vendor: " + p.getProperty("java.vm.specification.vendor") + "<br>").getBytes());
                os.write(("Java Virtual Machine specification name: " + p.getProperty("java.vm.specification.name") + "<br>").getBytes());
                os.write(("Java Virtual Machine implementation version: " + p.getProperty("java.vm.version") + "<br>").getBytes());
                os.write(("Java Virtual Machine implementation vendor: " + p.getProperty("java.vm.vendor") + "<br>").getBytes());
                os.write(("Java Virtual Machine implementation name: " + p.getProperty("java.vm.name") + "<br>").getBytes());
                os.write(("Java Runtime Environment specification version: " + p.getProperty("java.specification.version") + "<br>").getBytes());
                os.write(("Java Runtime Environment specification vendor: " + p.getProperty("java.specification.vendor") + "<br>").getBytes());
                os.write(("Java Runtime Environment specification name: " + p.getProperty("java.specification.name") + "<br>").getBytes());
                os.write(("Java class format version number: " + p.getProperty("java.class.version") + "<br>").getBytes());
                os.write(("Java class path: " + p.getProperty("java.class.path") + "<br>").getBytes());
                os.write(("List of paths to search when loading libraries: " + p.getProperty("java.library.path") + "<br>").getBytes());
                os.write(("Default temp file path: " + p.getProperty("java.io.tmpdir") + "<br>").getBytes());
                os.write(("Name of JIT compiler to use: " + p.getProperty("java.compiler") + "<br>").getBytes());
                os.write(("Path of extension directory or directories: " + p.getProperty("java.ext.dirs") + "<br>").getBytes());
                os.write(("Operating system name: " + p.getProperty("os.name") + "<br>").getBytes());
                os.write(("Operating system architecture: " + p.getProperty("os.arch") + "<br>").getBytes());
                os.write(("Operating system version: " + p.getProperty("os.version") + "<br>").getBytes());
                os.write(("File separator (\"/\" on UNIX): " + p.getProperty("file.separator") + "<br>").getBytes());
                os.write(("Path separator (\":\" on UNIX): " + p.getProperty("path.separator") + "<br>").getBytes());
                os.write(("Line separator (\"\\n\" on UNIX): " + p.getProperty("line.separator") + "<br>").getBytes());
                os.write(("User's account name: " + p.getProperty("user.name") + "<br>").getBytes());
                os.write(("User's home directory: " + p.getProperty("user.home") + "<br>").getBytes());
                os.write(("User's current working directory: " + p.getProperty("user.dir") + "<br>").getBytes());
                os.write(("----------------------------------------------<br>").getBytes());
            } catch (Exception ex) {
                System.err.println("Error send info:" + ex.toString());
            }
        }

        public static String getMimeType(File pageFile) {
            String type = null;
            String extension = null;
            try {
                extension = MimeTypeMap.getFileExtensionFromUrl(pageFile.toURI().toURL().toString());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            if (extension != null) {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
            return type;
        }

        /**
         * Отправка бинарного файла клиенту
         *
         * @param pageFile
         */
        private void sendRawFile(File pageFile) {
            try {
                // OutputStream os= socket.getOutputStream();
                // String TypeCont = ContentType(pageFile);
                String TypeCont = getMimeType(pageFile);
                // Первая строка ответа
                os.write("HTTP/1.1 200 OK\r\n".getBytes());
                // дата создания в GMT
                DateFormat df = DateFormat.getTimeInstance();
                df.setTimeZone(TimeZone.getTimeZone("GMT"));
                // Время последней модификации файла в GMT
                os.write(("Last-Modified: " + df.format(new Date(pageFile.lastModified())) + "\r\n").getBytes());
                // Длина файла
                os.write(("Content-Length: " + pageFile.length() + "\r\n").getBytes());
                os.write(("Content-Type: " + TypeCont + "; ").getBytes());
                os.write(("charset=" + Json.get("Charset") + "\r\n").getBytes());
                // Остальные заголовки
                os.write("Connection: close\r\n".getBytes());
                os.write("Server: HTMLserver\r\n\r\n".getBytes());
                // Сам файл:
                FileInputStream fis = new FileInputStream(pageFile.getAbsolutePath());
                int lengRead = 1;
                byte buf[] = new byte[1024];
                while ((lengRead = fis.read(buf)) != -1) {
                    os.write(buf, 0, lengRead);
                    os.flush();
                }
                // закрыть файл
                fis.close();
                // завершаем соединение
                os.close();
                is.close();
                //   System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
            } catch (IOException ex) {
                Logger.getLogger(HttpSrv.class.getName()).log(Level.SEVERE, null, ex);
            }
        }


        /**
         * Закодировать строку кодировкой MD5
         *
         * @param input
         * @return
         */
        private static String getMD5(String input) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] messageDigest = md.digest(input.getBytes());
                BigInteger number = new BigInteger(1, messageDigest);
                String hashtext = number.toString(16);
                while (hashtext.length() < 32) {
                    hashtext = "0" + hashtext;
                }
                return hashtext;
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }


        /**
         * Создание иденификатора подключаемого компьютера  и сохранение его в куках клиенской машины
         */
        public void CreateCompId() {
            //
            // создаем идентификатор компьютера , сохраняем его в Кукисах и перезагружаем страницу
            if (Json.containsKey("WORCSTATIONID") == false) {
                try {
                    Date currentDate = new Date();
                    Long time = currentDate.getTime();
                    String IDcomp = getMD5(numComp + IPmac + time);
                    String initWORCSTation = ""
                            + "<script>"
                            + "    function setCookie(cname, cvalue, exdays) { var d = new Date(); d.setTime(d.getTime() + (exdays)); var expires = 'expires='+d.toUTCString();   		document.cookie = cname + '=' + cvalue + '; ' + expires;} \n"
                            + "    setCookie('WORCSTATIONID', '" + IDcomp + "', 157680000); "
                            + "    window.location.href=window.location.toString();"
                            + "</script>"; //31536000
                    os.write(("HTTP/1.1 200 OK\r\n").getBytes());
                    os.write(("Content-Type: text/html; ").getBytes());
                    os.write(("Content-Length: " + initWORCSTation.length() + "\r\n").getBytes());
                    os.write(("charset=utf-8\r\n").getBytes());
                    os.write("Connection: close\r\n".getBytes());
                    os.write("Server: HTMLserver\r\n\r\n".getBytes());
                    os.write(initWORCSTation.getBytes());
                    return;
                } catch (Exception ex) {
                    System.err.println("Error create ID comp:" + ex.toString());
                    return;
                }
            }
        }

        /**
         * сздать идентификатор компьютера
         */
        private void createIdComp() {
            //
            // создаем идентификатор компьютера , сохраняем его в Кукисах и перезагружаем страницу
            if (Json.containsKey("WORCSTATIONID") == false) {
                try {
                    Date currentDate = new Date();
                    Long time = currentDate.getTime();
                    String IDcomp = getMD5(numComp + IPmac + time);
                    String initWORCSTation = ""
                            + "<script>"
                            + "    function setCookie(cname, cvalue, exdays) { var d = new Date(); d.setTime(d.getTime() + (exdays)); var expires = 'expires='+d.toUTCString();   		document.cookie = cname + '=' + cvalue + '; ' + expires;} \n"
                            + "    setCookie('WORCSTATIONID', '" + IDcomp + "', 157680000); "
                            + "    window.location.href=window.location.toString();"
                            + "</script>"; //31536000
                    os.write(("HTTP/1.1 200 OK\r\n").getBytes());
                    os.write(("Content-Type: text/html; ").getBytes());
                    os.write(("Content-Length: " + initWORCSTation.length() + "\r\n").getBytes());
                    os.write(("charset=utf-8\r\n").getBytes());
                    ;
                    os.write("Connection: close\r\n".getBytes());
                    os.write("Server: HTMLserver\r\n\r\n".getBytes());
                    os.write(initWORCSTation.getBytes());
                    is.close();
                    os.close();
                    socket.close();
                    return;
                } catch (Exception ex) {
                    System.err.println("Error create ID comp:" + ex.toString());
                    return;
                }
            }

        }
    }


    /**
     * Created by myasnikov on 14.01.16.
     */
    public abstract static class Html extends ContentProvider {
        public abstract Bundle call(String method, String arg, Bundle extras) ;

        @Override
        public boolean onCreate() {
            return false;
        }

        @Override
        public Cursor query(Uri uri, String[] strings, String s, String[] strings2, String s2) {
            return null;
        }

        @Override
        public String getType(Uri uri) {
            return null;
        }

        @Override
        public Uri insert(Uri uri, ContentValues contentValues) {
            return null;
        }

        @Override
        public int delete(Uri uri, String s, String[] strings) {
            return 0;
        }

        @Override
        public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
            return 0;
        }
    }


}