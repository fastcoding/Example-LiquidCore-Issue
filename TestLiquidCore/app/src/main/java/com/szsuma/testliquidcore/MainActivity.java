package com.szsuma.testliquidcore;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;

import org.liquidplayer.javascript.JSFunction;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    WebServer webServer = null;
    WebView webView = null;
    static String TAG = "mainact";
    JSFunction jsf_get_pic = null;
    ArrayList<String> picfiles=new ArrayList<String>();
    int idx=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = (WebView) findViewById(R.id.webview);
        webServer = new WebServer(getApplicationContext());

        webServer.setOnServerReady(new Runnable() {
            @Override
            public void run() {
                for (File file : new File(webServer.getWebDir()+"/public/images").listFiles()) {
                    picfiles.add(file.getPath());
                    Log.i(TAG,"added pic:"+file.getPath());
                }
                String url=webServer.getUri();
                setTitle(url);
                Log.i(TAG,"start webview:"+url);
                webView.loadUrl(url);
                webServer.getJSContext().property("android_fetch_pic",new JSFunction(webServer.getJSContext(),"fetch_pic") {
                    public String fetch_pic() {
                        if (idx<picfiles.size()){
                            try {
                                File f=new File(picfiles.get(idx));
                                FileInputStream fi=new FileInputStream(f);
                                byte[] bf = new byte[(int)f.length()];
                                if (f.length()!=fi.read(bf)){
                                    Log.e(TAG,"failed to read:"+f.getPath());
                                }
                                fi.close();
                                idx++;
                                return Base64.encodeToString(bf,Base64.DEFAULT);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }else{
                            idx=0;
                        }
                        return null;
                    }
                });

            }
        });



        webServer.start();
        Button btn=(Button)findViewById(R.id.refresh);
        btn.setClickable(true);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                webView.reload();
            }
        });
    }
}
