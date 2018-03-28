package com.serendipity.chengzhengqian.jsmaster;

import fi.iki.elonen.NanoHTTPD;

import java.util.HashMap;
import java.util.Map;

public class JsServer extends NanoHTTPD {

    public JsServer(int port) {
        super(port);
    }

    private String parseBodyForPostOrGet(IHTTPSession session){
        Map<String, String> files = new HashMap<>();
        Method method = session.getMethod();
        if (Method.PUT.equals(method) || Method.POST.equals(method)) {
            try {
                session.parseBody(files);
            } catch (Exception e) {
                return (e.toString());
            }
        }
        return "";
    }
    @Override
    public Response serve(IHTTPSession session) {
        String url=session.getUri();
        GlobalState.sendToMain(GlobalState.ADDSEVERLOG,JsService.wrapServiceInfo(url));
        if(url.equals("/index.html")||
                session.getUri().equals("/")
                )return indexHtml();

        else if(session.getUri().equals("/runCode"))return runCode(session);
        else if(session.getUri().equals("/runCodeUI"))return runCodeUI(session);

        else if(session.getUri().equals("/createWindows"))return createWindows(session);

        return newFixedLengthResponse("Not Found!");
    }

    private Response createWindows(IHTTPSession session) {
        String result=parseBodyForPostOrGet(session);
        String response="";
        if(result.equals("")) {
            String name= session.getParms().get("name");
            if (GlobalState.isUIRunning) {
                response = SUCCESS;
                GlobalState.sendToMain(GlobalState.ADDWINDOW, name);
            } else {
                response = NOTRUNNING;
            }
        }
        return newFixedLengthResponse(response);
    }

    public static String SUCCESS="success";
    public static String NOTRUNNING="not running";
    private Response runCode(IHTTPSession session) {
        String result=parseBodyForPostOrGet(session);
        String response="";
        if(result.equals("")) {
            String content = session.getParms().get("code");
            String id= session.getParms().get("id");
            if (GlobalState.isUIRunning) {
                int i=Integer.valueOf(id);
                response = SUCCESS;
                if(i==JsEngine.DefaultEngineId)
                    GlobalState.sendToMain(GlobalState.RUNCODE, content);
                else if(i>=GlobalState.RESERVEINDEPENDENTCTXIDMIN&&i<=GlobalState.RESERVEINDEPENDENTCTXIDMAX){
                    GlobalState.sendToMain(i,content);
                }
                else {
                    response= " id should be within "+GlobalState.RESERVEINDEPENDENTCTXIDMIN
                            + " and " +GlobalState.RESERVEINDEPENDENTCTXIDMAX;
                }

            } else {
                response = NOTRUNNING;
            }
        }

        return newFixedLengthResponse(response);
    }
    private Response runCodeUI(IHTTPSession session) {
        String result=parseBodyForPostOrGet(session);
        String response="";
        if(result.equals("")) {
            String content = session.getParms().get("code");
            if (GlobalState.isUIRunning) {

                response = SUCCESS;
                GlobalState.sendToMain(GlobalState.RUNCURRENTWINDOW,content);

            } else {
                response = NOTRUNNING;
            }
        }

        return newFixedLengthResponse(response);
    }

    private Response indexHtml(){
        return newFixedLengthResponse(GlobalState.serverIndexHtml);
    }


}
