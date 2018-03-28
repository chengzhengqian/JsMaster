package com.serendipity.chengzhengqian.jsmaster;


import android.os.Bundle;
import android.os.ResultReceiver;

/*
class to hold global variables
 */
public class GlobalState {

    public static int ADDLOG=0;
    public static int ADDSEVERLOG=1;
    public static int SHOWTOAST=2;
    public static int RUNCODE=3;
    public static int ADDLOGERROR = 5;
    public static int ADDLOGINFO = 6;
    public static int RESERVEINDEPENDENTCTXIDMIN=1000;
    public static int RESERVEINDEPENDENTCTXIDMAX=2000;
    public static int ADDWINDOW=7;
    public static final int RUNCURRENTWINDOW = 8;
    public static boolean isUIRunning;
    public static ResultReceiver uiReceiver;
    public static String ContentTag="content";
    public static boolean isServerRunning=false;
    public static boolean isFileInited=false;
    public static String  serverIndexHtml;
    public static int CurrentWindowsHeapId=0;
    public static void sendToMain(int type, String content){
        if(isUIRunning)
        {
            Bundle bundle = new Bundle();
            bundle.putString(ContentTag, content);
            uiReceiver.send(type, bundle);
        }
    }

    public static void printToJSLog(String s){
        sendToMain(ADDLOG,s);
    }
}
