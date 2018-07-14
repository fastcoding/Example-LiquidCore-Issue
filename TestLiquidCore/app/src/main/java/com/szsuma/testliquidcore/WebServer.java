package com.szsuma.testliquidcore;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.liquidplayer.javascript.JSContext;
import org.liquidplayer.javascript.JSFunction;
import org.liquidplayer.node.Process;
import org.liquidplayer.service.MicroService;
import org.liquidplayer.service.Synchronizer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class WebServer {
    static String TAG="webserver";

    private MicroService mService=null;
    private String serviceId=null;
    private Context mContext=null;
    private JSContext mJSContext=null;
    private Runnable OnServerReady=null;
    private String sWebDir=null;
    public String getWebDir(){
        return sWebDir;
    }
    public String getUri() {
        return "http://"+ipAddress+":"+port;
    }
    public JSContext getJSContext(){
        return mJSContext;
    }
    private String ipAddress="";
    private int port=0;
    private Thread webroot_thread=null;
    public Runnable getOnServerReady() {
        return OnServerReady;
    }

    public void setOnServerReady(Runnable onServerReady) {
        if (webroot_thread!=null){
            try {
                webroot_thread.join();
                webroot_thread=null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        OnServerReady = onServerReady;
    }

    static final int unzipBUFFER = 2048;
    private void fireServerStarted(){

        mService.emit("startServer",true);
        if (OnServerReady!=null){
            new android.os.Handler(Looper.getMainLooper()).post(OnServerReady);
        }
    }
    public static boolean unzipToDir(InputStream zipstream, String outBasedir) {
        try {
            BufferedOutputStream dest = null;
            BufferedInputStream is = null;
            ZipEntry entry;
            ZipInputStream zipfile = new ZipInputStream(zipstream);
            entry = zipfile.getNextEntry();
            while(entry!=null) {
                if (entry.isDirectory()){
                    Log.d(TAG,"Extracting Directory: " + entry.getName());
                    File f=new File(outBasedir+"/"+entry.getName());
                    f.mkdirs();
                }else {
                    Log.d(TAG,"Extracting File: " + entry.getName());
                    is = new BufferedInputStream(zipfile);
                    int count;
                    byte data[] = new byte[unzipBUFFER];
                    FileOutputStream fos = new
                            FileOutputStream(outBasedir + "/" + entry.getName());
                    dest = new
                            BufferedOutputStream(fos, unzipBUFFER);
                    while ((count = is.read(data, 0, unzipBUFFER))
                            != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                    dest.close();
                    //is.close();
                }
                entry = zipfile.getNextEntry();
            }
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private final MicroService.EventListener readyListener = new MicroService.EventListener() {

        @Override
        public void onEvent(MicroService service, String event, JSONObject payload) {
            prepareWebroot(service);
            try {
                ipAddress=payload.getString("wlan0");
                port=payload.getInt("port");
                Log.i(TAG,"uri="+ipAddress+":"+port);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };
    // Our start listener will set up our event listeners once express server is up and running
    private final MicroService.ServiceStartListener startListener = new MicroService.ServiceStartListener() {
        @Override
        public void onStart(MicroService service, final Synchronizer synchronizer) {
            Log.i(TAG, "service started");

            // mWakeLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP, "DoNotDimScreen");
            //mWakeLock.acquire()

            service.getProcess().addEventListener(new Process.EventListener() {
                @Override
                public void onProcessStart(Process process, JSContext context) {
                    mJSContext=context;
                    context.property("device_log",new JSFunction(context,"android_log"){
                        public void android_log(Object... msgs){
                            synchronizer.enter();
                            String ss="";
                            for(Object o:msgs){
                                ss+=o.toString();
                            }
                            Log.i(TAG,ss);
                            synchronizer.exit();
                        }
                    });
                    //forward console.log to android logcat
                    context.evaluateScript("console={};console.log=console.info=console.debug=function(){device_log.apply(this,arguments)};");

                }

                @Override
                public void onProcessAboutToExit(Process process, int exitCode) {

                }

                @Override
                public void onProcessExit(Process process, int exitCode) {
                    mJSContext=null;
                }

                @Override
                public void onProcessFailed(Process process, Exception error) {
                    Log.d(TAG,"process failed: "+error.toString());

                }
            });
            service.addEventListener("ready", readyListener);
        }
    };

    private void prepareWebroot(final MicroService service){
        if (serviceId==null){
            Log.e(TAG,"unable to get service Id");
            service.emit("startServer",false);
            return;
        }
        //copied from liquid core internals, we have to install our web resources into javascript filesystem space.
        final String suffix = "/__org.liquidplayer.node__/_" + serviceId;
        sWebDir = mContext.getApplicationContext().getFilesDir().getAbsolutePath() + suffix + "/local/web";
        File webDir=new File(sWebDir);
        if (webDir.exists()) {
            File flocalweb = new File(sWebDir + "/.webroot_done");

            //install static page resources once.
            if (flocalweb.exists()) {
                fireServerStarted();
                return;
            }
        }else{
            webDir.mkdir();
        }
        webroot_thread=new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream zipstream = null;
                boolean ok=true;
                try {
                    zipstream = mContext.getApplicationContext().getAssets().open("web.zip");
                    if (zipstream!=null) {
                        ok = unzipToDir(zipstream, sWebDir);
                        zipstream.close();
                        File fdone=new File(sWebDir+"/.webroot_done");
                        fdone.createNewFile();
                        if (!ok) {
                            Log.e(TAG, "unable to extract web resources!");
                            service.emit("startServer",false);
                            return;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    service.emit("startServer",false);
                    return;
                }
                fireServerStarted();

            }
        });
        webroot_thread.start();
        return;
    }


    public WebServer(Context ctx){
        mContext=ctx;
    }


    public void start(){
        try {
            URI uri = new URI("android.resource://" + mContext.getPackageName() + "/raw/server" );
            try {
                serviceId = URLEncoder.encode(uri.toString().substring(0,uri.toString().lastIndexOf('/')), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            //JNICall.NDKSetEnv("NODE_OPTIONS","--max-old-space-size=64",true); //
            mService = new MicroService(mContext.getApplicationContext(), uri, startListener);
            mService.start( /*arguments to js */);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void stop(){

        if (mService!=null){
            mService.emit("stopServer");
        }
    }

}
