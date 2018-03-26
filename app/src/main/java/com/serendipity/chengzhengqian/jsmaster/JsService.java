package com.serendipity.chengzhengqian.jsmaster;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.time.LocalDate;
import java.util.Calendar;

import static java.time.LocalDate.*;

public class JsService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int ONGOING_NOTIFICATION_ID = 1;
    private static int WEBSERVER_PORT = 9000;
    private JsServer server;
    public static String wrapServiceInfo(String s){
        return Utils.getCurrentTime()+": "+s+"\n";
    }
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            server=new JsServer(WEBSERVER_PORT);
            server.start();
            GlobalState.sendToMain(GlobalState.ADDSEVERLOG,
                    wrapServiceInfo("server is started"));
        }
        catch (Exception e){
            GlobalState.sendToMain(GlobalState.ADDSEVERLOG,
                    wrapServiceInfo(e.toString()));
        }
        return START_STICKY;

    }
    @TargetApi(26)
    public void onDestroy() {
        super.onDestroy();
        server.stop();
        GlobalState.sendToMain(GlobalState.ADDSEVERLOG,
                wrapServiceInfo("server is stopped!"));
    }

}