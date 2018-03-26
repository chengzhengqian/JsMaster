package com.serendipity.chengzhengqian.jsmaster;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.constraint.ConstraintLayout;
import android.text.Spannable;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends Activity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
    LinearLayout mainLayout;
    public final String jsEditorTag="edit";
    public final String jsLogTag="log";
    public final String serverTag="web";
    public void addLog(String s){
        jsLog.append(s);
    }
    public void addServerLog(String s){
        serverLog.append(s);
    }
    public void addLogWithColor( String text, int color) {

        int start = jsLog.getText().length();
        jsLog.append(text);
        int end = jsLog.getText().length();

        Spannable spannableText = (Spannable) jsLog.getText();
        spannableText.setSpan(new ForegroundColorSpan(color), start, end, 0);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    public int maxLineNumbers=1000;
    private void setTextViewScrollable(TextView tv){
        tv.setMaxLines(maxLineNumbers);
        tv.setMovementMethod(new ScrollingMovementMethod());
    }
    public TextView jsLog;
    private void setMatchParentLinearLayout(View v){
        LinearLayout.LayoutParams p=
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,0);
        v.setLayoutParams(p);
    }
    private void setVerticalWeightLinearLayout(View v,float f){
        LinearLayout.LayoutParams p=
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        0,f);
        v.setLayoutParams(p);
    }
    private void setHorizontalWeightLinearLayout(View v,float f){
        LinearLayout.LayoutParams p=
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT,
                        f);
        v.setLayoutParams(p);
    }
    private void setVerticalWrapLinearLayout(View v){
        LinearLayout.LayoutParams p=
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,0);
        v.setLayoutParams(p);
    }
    private Button createTransparentBtn(String text){
        Button b=new Button(this);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setTextColor(Color.BLUE);
        b.setText(text);
        return b;
    }
    private final String btnRunText="run";
    private final String btnClearText="clr";
    private void clearJsLog(){
        jsLog.setText("");
    }
    private LinearLayout generateJavascriptLogView(){
        LinearLayout jsLogLL=new LinearLayout(this);
        jsLogLL.setOrientation(LinearLayout.VERTICAL);
        jsLog =new TextView(this);
        jsLog.setGravity(Gravity.BOTTOM);
        setVerticalWeightLinearLayout(jsLog,1.0f);
        setTextViewScrollable(jsLog);
        jsLogLL.addView(jsLog);

        LinearLayout logCtrLL=new LinearLayout(this);
        logCtrLL.setOrientation(LinearLayout.HORIZONTAL);
        setVerticalWrapLinearLayout(logCtrLL);

        Button run=createTransparentBtn(btnRunText);
        setHorizontalWeightLinearLayout(run,1.0f);
        run.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showToast(JsEngine.runJavaScript(jsEditor.getText().toString(), JsEngine.DefaultEngineId));
            }
        });
        Button clr=createTransparentBtn(btnClearText);
        setHorizontalWeightLinearLayout(clr,1.0f);
        clr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearJsLog();
            }
        });
        logCtrLL.addView(run);
        logCtrLL.addView(clr);

        jsLogLL.addView(logCtrLL);


        return jsLogLL;
    }
    public EditText jsEditor;

    private LinearLayout generateJavascriptEditorView(){
        LinearLayout jsEditorLL=new LinearLayout(this);
        jsEditor =new EditText(this);
        jsEditor.setGravity(Gravity.LEFT);
        jsEditor.setBackgroundColor(Color.TRANSPARENT);
        jsEditor.setTextColor(Color.GREEN);
        setMatchParentLinearLayout(jsEditor);
        setTextViewScrollable(jsEditor);
        jsEditorLL.addView(jsEditor);
        return jsEditorLL;
    }
    public TextView serverLog;
    public void setToggleServerStartBtn(View v){
        Button toggleStart= (Button) v;
        if(GlobalState.isServerRunning){
            toggleStart.setText(btnTexttoggleServer[1]);
        }
        else
            toggleStart.setText(btnTexttoggleServer[0]);
    }
    public String[] btnTexttoggleServer=new String[]{"start","stop"};
    private LinearLayout generateServerControlView(){
        LinearLayout serverCtlLL=new LinearLayout(this);
        serverCtlLL.setOrientation(LinearLayout.VERTICAL);
        serverLog=new TextView(this);
        serverLog.setGravity(Gravity.BOTTOM);
        serverLog.setBackgroundColor(Color.TRANSPARENT);
        serverLog.setTextColor(Color.BLUE);
        setVerticalWeightLinearLayout(serverLog,1.0f);
        setTextViewScrollable(serverLog);
        serverCtlLL.addView(serverLog);

        final LinearLayout serverBtnLL=new LinearLayout(this);
        setVerticalWrapLinearLayout(serverBtnLL);
        serverBtnLL.setOrientation(LinearLayout.HORIZONTAL);

        final Button toggleStart=createTransparentBtn("");
        setToggleServerStartBtn(toggleStart);
        setHorizontalWeightLinearLayout(toggleStart,1.0f);
        serverBtnLL.addView(toggleStart);
        toggleStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleServer();
                setToggleServerStartBtn(toggleStart);

            }
        });

        serverCtlLL.addView(serverBtnLL);

        return serverCtlLL;
    }
    public HashMap<String,LinearLayout> views=new HashMap<>();

    private void generateViews(){
        views.clear();
        views.put(jsLogTag,generateJavascriptLogView());
        views.put(jsEditorTag,generateJavascriptEditorView());
        views.put(serverTag,generateServerControlView());
        for(LinearLayout l:views.values()){
            mainLayout.addView(l);
        }
    }
    private void showToast(String msg){
        Toast.makeText(this,msg,Toast.LENGTH_LONG).show();
    }
    private final String setCurrentViewNotFound="set view must be one of ";
    private void setCurrrentView(String tag){
        if(views.get(tag)!=null){
            LinearLayout.LayoutParams noShow
                    =new LinearLayout.LayoutParams
                    (0, LinearLayout.LayoutParams.MATCH_PARENT,0);
            LinearLayout.LayoutParams show
                    =new LinearLayout.LayoutParams
                    (LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT,1);
            for(LinearLayout l:views.values()){
                l.setLayoutParams(noShow);
            }
            views.get(tag).setLayoutParams(show);

        }
        else {
            showToast(setCurrentViewNotFound+views.keySet().toString());
        }
    }
    ConstraintLayout root;

    private void findKeyUIs(){
        mainLayout=findViewById(R.id.mainLayout);
        mainLayout.removeAllViews();
        root=findViewById(R.id.Root);
    }
    private void registerState(){
        GlobalState.uiReceiver =new UIReceiver(null);
        GlobalState.isUIRunning=true;
    }
    private void unRegisterState(){
        GlobalState.isUIRunning=false;
    }
    @Override
    protected void onResume(){
        super.onResume();
        findKeyUIs();
        generateViews();
        generateMenu();
        setCurrrentView(jsLogTag);
        startEngine();
        registerState();
        enSureServerStart();
    }
    public void enSureServerStart(){
        if(!GlobalState.isServerRunning){
            toggleServer();
        }
    }

    public void startEngine(){
        JsEngine.init(getRawResource(R.raw.init));
    }

    public void stopEngine(){
        JsEngine.close();
    }

    @Override
    protected void onPause(){
        super.onPause();
        stopEngine();
        unRegisterState();
    }


    public int menuSize=250;
    private boolean isMenuShow=false;
    ListView menuList;
    public void generateMenu(){
        menuList = new ListView(this);
        ArrayList<String> s = new ArrayList<>(views.keySet());
        ArrayAdapter<String> viewTags =
                new ArrayAdapter<String>(this,
                        android.R.layout.simple_list_item_1,
                        s
                );
        ConstraintLayout.LayoutParams lp =
                new ConstraintLayout.LayoutParams
                        (menuSize, ConstraintLayout.LayoutParams.WRAP_CONTENT);
        lp.topToBottom = R.id.control;
        lp.rightToRight = R.id.Root;
        menuList.setAdapter(viewTags);
        menuList.setLayoutParams(lp);


        menuList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                toggleMenus(view);
                setCurrrentView((String) ((TextView) view).getText());
            };});

    }
    public void toggleMenus(View view) {
        if(!isMenuShow) {
            root.addView(menuList);
            isMenuShow=true;
        }else {
            root.removeView(menuList);
            isMenuShow=false;
        }


    }
    private String readFromInputStream(InputStream input) {
        BufferedReader reader=new BufferedReader(new InputStreamReader(input));
        String content="";
        StringBuilder builder=new StringBuilder();
        try {
            while ((content = reader.readLine()) != null) {
                builder.append(content + "\n");
            }
        }
        catch (Exception e)
        {
            addServerLog(e.toString());
        }
        return builder.toString();
    }
    public String getRawResource(int id){
        InputStream input= getResources().openRawResource(id);
        String content=(readFromInputStream(input));
        return content;
    }
    public void toggleServer() {
        Intent intent=new Intent(getBaseContext(),JsService.class);
        if(GlobalState.isServerRunning){
            stopService(intent);
        }
        else{
            if(!GlobalState.isFileInited){
                GlobalState.serverIndexHtml=getRawResource(R.raw.index);
            }
            startService(intent);
        }
        GlobalState.isServerRunning=!GlobalState.isServerRunning;

    }

    class UIReceiver extends ResultReceiver{

        public UIReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            runOnUiThread(
                    new UpdateUI(resultCode,
                            resultData.getString(GlobalState.ContentTag)
                            )
            );}
    }
    class UpdateUI implements Runnable {
        String content;
        int type;
        public UpdateUI(int type,String content) {
            this.content= content;
            this.type=type;
        }
        @Override
        public void run() {
            if(type==GlobalState.ADDLOG){
                addLog(content);
            }
            else if(type==GlobalState.ADDLOGERROR){
                addLogWithColor(content,Color.RED);
            }
            else if(type==GlobalState.ADDLOGINFO){
                addLogWithColor(content,Color.BLUE);
            }
            else if(type==GlobalState.ADDSEVERLOG){
                addServerLog(content);
            }
            else if(type==GlobalState.SHOWTOAST){
                showToast(content);
            }
            else if(type==GlobalState.RUNCODE){
                addLog("\n"+JsEngine.runJavaScript(content, JsEngine.DefaultEngineId));
            }

            else if(type>=GlobalState.RESERVEINDEPENDENTCTXIDMIN &&type<=GlobalState.RESERVEINDEPENDENTCTXIDMAX){
                addLog("\n"+JsEngine.runJavaScript(content,type));
            }
            else {
                showToast("UpdateUI receive unknown code "+type);
            }

        }
    }
}
