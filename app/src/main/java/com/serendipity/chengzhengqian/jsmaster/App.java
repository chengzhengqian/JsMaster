package com.serendipity.chengzhengqian.jsmaster;

import android.app.Activity;
import android.widget.LinearLayout;

public class App extends LinearLayout {
    public Activity ctx;
    public int id;
    public String name;
    public App(Activity context, int id, String windowsName) {
        super(context);
        ctx= context;
        this.id =id;
        this.name=windowsName;
        JsEngine.runJavaScript(JsEngine.WINDOWSNAME+"=\""+windowsName+"\"", this.id);
    }
}
