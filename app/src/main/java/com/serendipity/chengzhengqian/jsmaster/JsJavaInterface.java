package com.serendipity.chengzhengqian.jsmaster;
import java.util.Random;
import java.util.HashMap;

/*
* this class provides key API to let duktape reflectly create objects and call fucntion
* */
public class JsJavaInterface {
    /* a table to hold all referece to java ojbect */
    private static Random rand=new Random();
    public static HashMap<Integer,Object>objects=new HashMap<>();
    public static int getNewId(){
        int result=rand.nextInt();
        while(objects.get(result)!=null){
            result=rand.nextInt();
        }
        return result;

    }
    public static Object loadClass(Object[] paras){
        if(paras.length>0) {
            String className="";
            if(paras[0] instanceof String){
                className= (String) paras[0];
            }
            else{
                GlobalState.sendToMain(GlobalState.ADDLOGERROR,"class name should be string");
                return 0;
            }
            try {

                Class<?> c = Class.forName(className);
                int id = getNewId();
                objects.put(id, c);
                return new JObject(id);
            } catch (Exception e) {
                GlobalState.sendToMain(GlobalState.ADDLOGERROR, e.toString());
            }
        }
        return 0;
    }
    public static Object call(Object[] paras){
        GlobalState.sendToMain(GlobalState.ADDLOG,"static:\n");
        for(Object b:paras){
            if(b instanceof JObject){
               // GlobalState.sendToMain(GlobalState.ADDLOG,"Java Object :"+((JObject) b).id+"\n");
                Object p=JsJavaInterface.objects.get(((JObject) b).id);
                if(p!=null){
                    GlobalState.sendToMain(GlobalState.ADDLOG,p.getClass().getSimpleName()+":"+p.toString()+"\n");
                }
                else {
                    GlobalState.sendToMain(GlobalState.ADDLOG,"Invalid id:"+((JObject) b).id+"\n");
                }
            }
            else
            GlobalState.sendToMain(GlobalState.ADDLOG,b.getClass().getSimpleName()+":"+b.toString()+"\n");
        }
        if(paras.length>0)
            return paras[paras.length-1];
        return -1;
    }

}
