package com.serendipity.chengzhengqian.jsmaster;


import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsEngine {
    /*
    * Id corresponds a js context, i.e, a thread handle running on a heap,(probably shared by other context.
    * It have its own value stack
    * */
    public static final int DefaultEngineId =0;
    public static final int DefaultThreadId=0;
    public static Pattern threadIdForm=Pattern.compile("@@(.*)@@");

    private static native String runJS(String x, int id,int threadId);
    /* automatically create id and DefaultThreadId */
    private static native void create(int id);
    private static void createWithInitFile(int id){
        create(id);
        runJS(initString,id,DefaultThreadId);
    }
    public static String initString="";
    /* Assuming id has be create and threadId is different from DefaultThreadId*/
    private static native void createNewThread(int id, int threadId);
    private static native void createNewThreadNewEnv(int id, int threadId);
    private static native void destroy(int id);

    private static void registerContexts(int id, int threadId, boolean isNew){
        if(contexts.get(id)==null){
            HashMap<Integer,Boolean> threads=new HashMap<>();
            threads.put(DefaultThreadId,true);
            contexts.put(id,threads);
        }
        if(threadId!=DefaultThreadId){
            contexts.get(id).put(threadId,isNew);
        }
    }
    private static void clearContexts(){
        contexts.clear();
    }
    public static void init(String initFile){

        initString=initFile;
        createWithInitFile(DefaultEngineId);
        registerContexts(DefaultEngineId,DefaultThreadId,true);
    }
    public static void close(){
        for(Integer i:contexts.keySet()){
            destroy(i);
        }
        clearContexts();
    }
    private static HashMap<Integer,HashMap<Integer,Boolean>> contexts=new HashMap<>();
    /* check the heap id and thread id and then run. Create them if necessary
    * */
    public static String runJavaScript(String x, int id){

        Matcher m=threadIdForm.matcher(x);
        int threadId=DefaultThreadId;
        boolean isNewEnv=false;
        boolean isExist;
        String created="";
        /*has annotation for non default thread Id*/
        if(m.find()){
            String keys=m.group(1);
            if(keys.startsWith("new")){
                keys=keys.substring(3,keys.length());
                isNewEnv=true;
            }
            threadId=Integer.valueOf(keys);
            x=x.substring(m.end(),x.length());
        }

        if(contexts.get(id)==null){
            createWithInitFile(id);
            registerContexts(id,DefaultThreadId,true);

        }
        if(contexts.get(id).get(threadId)==null)isExist=false;
        else isExist=true;

        if(!isExist){
            if(isNewEnv){
                createNewThreadNewEnv(id,threadId);
                registerContexts(id,threadId,true);
                created=" created with New Env";
            }
            else {
                createNewThread(id, threadId);
                registerContexts(id,threadId,false);
                created=" created";
            }
        }
        GlobalState.sendToMain(GlobalState.ADDLOGINFO,"\n>>> "+id+ " : "+threadId+created+"\n");
        String result="";
        try {
            result=runJS(x, id, threadId);
        }
        catch (Exception e){
            GlobalState.sendToMain(GlobalState.ADDLOGERROR,e.toString());
        }
        return result;
    }
}
