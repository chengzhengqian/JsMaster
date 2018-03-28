package com.serendipity.chengzhengqian.jsmaster;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

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
    public static boolean isDebug=true;
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
    public static String pretty(Object r){
        if(r instanceof Class<?>)
            return ((Class) r).getSimpleName();
        else if(r instanceof Integer || r instanceof Double|| r instanceof String)
            return r.toString();
        else
            return ("[obj:"+r.getClass().getSimpleName()+"]");
    }
    public static String valueOf(Object r){
        if(r instanceof Class<?>)
            return ((Class) r).getSimpleName()+":class";
        else if(r instanceof Integer || r instanceof Double|| r instanceof String)
            return r.toString();
        else
            return ("[obj:"+r.getClass().getSimpleName()+"]");
    }
    public static String stringFromJSObjectArray(Object[] paras){
        StringBuilder result=new StringBuilder();
        int index=0;
        for(Object r: paras){
            if(index>0)
                result.append(", ");
            result.append(pretty(r));

        }

        return result.toString();
    }
    static class CallResult{
        Object result;
        String exception;
        int type;
        CallResult(Object result, String exception, int type){
            if(result==null)
                this.result=JsNullValue;
            else
                this.result=result;
            this.exception=exception;
            this.type=type;
        }
    }
    static class FieldResult{
        Object result;
        String exception;
        int type;
        FieldResult(Object result, String exception, int type){
            if(result==null)
                this.result=JsNullValue;
            else
                this.result=result;
            this.exception=exception;
            this.type=type;
        }
    }
    public static final int OnlyStaticMethod=0;
    public static final int OnlyObjectMethod=0;
    public static List<Method> getOverloaedMethods(Class<?> c, String name, int type){
        LinkedList<Method> result=new LinkedList<>();
        for(Method m:c.getMethods()){
            if(m.getName().equals(name)){
                if(type==(OnlyStaticMethod)&& Modifier.isStatic(m.getModifiers())){
                    result.add(m);
                }
                else if(type==(OnlyObjectMethod)&& !Modifier.isNative(m.getModifiers())){
                    result.add(m);
                }
            }
        }
        return result;
    }
    public static boolean isTypeMatch(Class<?> c, Object[] paras, int i){
        if(c.equals(int.class)){
            if(paras[i] instanceof Integer)
                return true;
        }
        else if(c.equals(double.class)){
            if(paras[i] instanceof Double)
                return true;
        }

        boolean isMatched = (c).isInstance(paras[i]);
        return isMatched;


    }
    public static Constructor[] getOverloaedConstructors(Class<?> c){
        return c.getConstructors();
    }
    public static boolean isMethodMatched(Method m, Object[] paras){

        Class<?>[] types=m.getParameterTypes();
        if(paras.length==types.length){
            for(int i=0;i<types.length;i++){
                if(!isTypeMatch(types[i],paras,i)){
                    return false;
                }
            }
            return true;
        }
        return false;

    }
    public static boolean isConstructorMatched(Constructor m, Object[] paras){
        Class<?>[] types=m.getParameterTypes();
        if(paras.length==types.length){
        for(int i=0;i<types.length;i++){
            if(!isTypeMatch(types[i],paras,i)){
                return false;
                }
            }
            return true;
        }
        return false;

    }
    public static Method getMatchedMethod(List<Method> methods, Object[] paras){
        for(Method m:methods){
            boolean isMatched=isMethodMatched(m,paras);
            if(isMatched){
                return m;
            }
        }
        return null;
    }
    public static Constructor getMatchedConstructor(Constructor[] methods, Object[] paras){
        for(Constructor m:methods){
            boolean isMatched=isConstructorMatched(m,paras);
            if(isMatched){
                return m;
            }
        }
        return null;
    }
    public static final int CallResultSuccess=0;
    public static final int CallResultFail=1;
    public static final int CallMethodNotFound=2;
    public static final int CallMethodArgNotMatch=2;
    /* assuming the first object is a instance of class<?> and second is string and paras has be converted
    to usual java Object */
    public static String getUnMatchedInfo(List<Method> methods, Object[] paras, String name){
        StringBuilder sb=new StringBuilder();
        sb.append(name+" is called with ");
        for(Object b :paras){
            sb.append(b.getClass().getSimpleName()+", ");
        }
        sb.append("\nbut the following are possible choices:\n");
        for(Method m: methods){
            for(Class<?> c: m.getParameterTypes()){
                sb.append(c.getSimpleName()+", ");
            }
            sb.append(";\n");
        }
        return sb.toString();
    }
    public static String getUnMatchedInfo(Constructor[] methods, Object[] paras,String className){
        StringBuilder sb=new StringBuilder();
        sb.append(className+" is initialized with ");
        for(Object b :paras){
            sb.append(b.getClass().getSimpleName()+", ");
        }
        sb.append("\nbut the following are possible choices:\n");
        for(Constructor m: methods){
            for(Class<?> c: m.getParameterTypes()){
                sb.append(c.getSimpleName()+", ");
            }
            sb.append(";\n");
        }
        return sb.toString();
    }
    public static final String initMethodName="new";
    public static CallResult callConstructor(Class<?>c,Object[] paras){
        Constructor[] methods=getOverloaedConstructors(c);
        if(methods.length==0){
            return new CallResult(null,c.getSimpleName()+" has no constructor!",CallMethodNotFound);
        }
        Constructor m=getMatchedConstructor(methods,paras);
        Object b;
        if(m==null){
            return new CallResult(null,
                    getUnMatchedInfo(methods,paras,c.getName()),CallMethodArgNotMatch);
        }
        else {
            try {
                b=m.newInstance(paras);
            } catch (Exception e){
                return new CallResult(null, e.toString(),CallResultFail);
            }
        }

        return new CallResult(b,null,CallResultSuccess);
    }
    public static CallResult callStaticMethod(Class<?>c, String name, Object[] paras){
        List<Method> methods=getOverloaedMethods(c,name,OnlyStaticMethod);
        if(methods.size()==0){
            return new CallResult(null,name+" not found as static!",CallMethodNotFound);
        }
        Method m=getMatchedMethod(methods,paras);
        Object b;
        if(m==null){
            return new CallResult(null,
                    getUnMatchedInfo(methods,paras,name),CallMethodArgNotMatch);
        }
        else {
            try {
                b=m.invoke(null,paras);
                return new CallResult(b,null,CallResultSuccess);
            } catch (Exception e){
                return new CallResult(null, e.toString(),CallResultFail);
            }
        }


    }
    public static CallResult callClassMethod(Class<?> c, String name, Object[] paras){
        if(name.equals(initMethodName)){
            return callConstructor(c,paras);
        }
        else
            return callStaticMethod(c,name,paras);

    }
    public static CallResult callObjectMethod( Object obj,String name,Object[] paras){
        Class<?> c=obj.getClass();
        List<Method> methods=getOverloaedMethods(c,name,OnlyObjectMethod);
        if(methods.size()==0){
            return new CallResult(null,name+" not found as object method!",CallMethodNotFound);
        }
        Method m=getMatchedMethod(methods,paras);
        Object b;
        if(m==null){
            return new CallResult(null,
                    getUnMatchedInfo(methods,paras,name),CallMethodArgNotMatch);
        }
        else {
            try {
                b=m.invoke(obj,paras);
                return new CallResult(b,null,CallResultSuccess);
            } catch (Exception e){
                return new CallResult(null, e.toString(),CallResultFail);
            }
        }



    }
    /* if function must be called with two parameters*/
    public static Object call(Object[] paras){

        Object target=convertToJavaObject(paras[0]);
        String name= (String) convertToJavaObject(paras[1]);
        Object[] funcParas=new Object[paras.length-2];
        /* handle javascript method*/
        if(name.equals("valueOf")&&paras.length==2){
            return valueOf(target);
        }
        for(int i=0;i<paras.length-2;i++){
            funcParas[i]=convertToJavaObject(paras[i+2]);
        }
        if(isDebug)
            GlobalState.sendToMain(GlobalState.ADDLOGINFO,
                    "call->"+pretty(target)+"."+name+"("+stringFromJSObjectArray(funcParas)+")\n");
        if(target instanceof Class<?>){
            CallResult result= callClassMethod((Class<?>) target,name,funcParas);
            if(result.type==CallResultSuccess){
                return convertToJSObject(result.result);
            }
            GlobalState.sendToMain(GlobalState.ADDLOGINFO,result.exception);
            result=callObjectMethod(target,name,funcParas);
            if(result.type==CallResultSuccess){
                return convertToJSObject(result.result);
            }
            GlobalState.sendToMain(GlobalState.ADDLOGERROR,result.exception);
        }
        else {
            CallResult result= callObjectMethod(target,name,funcParas);
            if(result.type==CallResultSuccess){
                return convertToJSObject(result.result);
            }
            GlobalState.sendToMain(GlobalState.ADDLOGINFO,result.exception);
            result=callClassMethod(target.getClass(),name,funcParas);
            if(result.type==CallResultSuccess){
                return convertToJSObject(result.result);
            }
            GlobalState.sendToMain(GlobalState.ADDLOGERROR,result.exception);
        }
        return JsNullValue;
    }
    public static FieldResult getStaticField(Class<?> c, String name){
        try {
            Field f=c.getField(name);
            return new FieldResult(f.get(null),null,CallResultSuccess);
        } catch (NoSuchFieldException e) {
            return new FieldResult(null,e.toString(),CallMethodNotFound);
        } catch (IllegalAccessException e) {
            return new FieldResult(null,e.toString(),CallResultFail);
        }

    }
    public static FieldResult setStaticField(Class<?> c, String name,Object val){
        try {
            Field f=c.getField(name);
            f.set(null,val);
            return new FieldResult(null,null,CallResultSuccess);
        } catch (NoSuchFieldException e) {
            return new FieldResult(null,e.toString(),CallMethodNotFound);
        } catch (IllegalAccessException e) {
            return new FieldResult(null,e.toString(),CallResultFail);
        }

    }
    public static FieldResult getObjectField(Object obj, String name){
        try {
            Class<?> c=obj.getClass();
            Field f=c.getField(name);
            return new FieldResult(f.get(obj),null,CallResultSuccess);
        } catch (NoSuchFieldException e) {
            return new FieldResult(null,e.toString(),CallMethodNotFound);
        } catch (IllegalAccessException e) {
            return new FieldResult(null,e.toString(),CallResultFail);
        }

    }
    public static FieldResult setObjectField(Object obj, String name,Object val){
        try {
            Class<?> c=obj.getClass();
            Field f=c.getField(name);
            f.set(obj,val);
            return new FieldResult(null,null,CallResultSuccess);
        } catch (NoSuchFieldException e) {
            return new FieldResult(null,e.toString(),CallMethodNotFound);
        } catch (IllegalAccessException e) {
            return new FieldResult(null,e.toString(),CallResultFail);
        }

    }
    public static Object field(Object[] paras){

        Object target=convertToJavaObject(paras[0]);
        String name= (String) convertToJavaObject(paras[1]);
        if(isDebug)
            GlobalState.sendToMain(GlobalState.ADDLOGINFO,
                    "field->"+pretty(target)+"."+name+"\n");
        if(target instanceof Class<?>){
            FieldResult r=getStaticField((Class<?>) target,name);
            if(r.type==CallResultSuccess){
                return convertToJSObject(r.result);
            }
            GlobalState.sendToMain(GlobalState.ADDLOGINFO,r.exception);
            r=getObjectField(target,name);
            if(r.type==CallResultSuccess){
                return convertToJSObject(r.result);
            }
            GlobalState.sendToMain(GlobalState.ADDLOGERROR,r.exception);
        }
        else {
            FieldResult r=getObjectField(target,name);
            if(r.type==CallResultSuccess){
                return convertToJSObject(r.result);
            }
            GlobalState.sendToMain(GlobalState.ADDLOGINFO,r.exception);
            r=getStaticField((Class<?>) target,name);
            if(r.type==CallResultSuccess){
                return convertToJSObject(r.result);
            }
            GlobalState.sendToMain(GlobalState.ADDLOGERROR,r.exception);
        }
        return name;
    }
    public static Object setField(Object[] paras){
        Object target=convertToJavaObject(paras[0]);
        String name= (String) convertToJavaObject(paras[1]);
        Object val;
        if(paras.length>2)
            val=convertToJavaObject(paras[2]);
        else {
            GlobalState.sendToMain(GlobalState.ADDLOGERROR,"set field need a value");
            return null;
        }
        if(isDebug)
            GlobalState.sendToMain(GlobalState.ADDLOGINFO,
                    "setField->"+pretty(target)+"."+name+"="+pretty(val)+"\n");
        if(target instanceof Class<?>){
            FieldResult r=setStaticField((Class<?>) target,name,val);
            if(r.type==CallResultSuccess){
                return convertToJSObject(r.result);
            }
            GlobalState.sendToMain(GlobalState.ADDLOGINFO,r.exception);
            r=setObjectField(target,name,val);
            if(r.type==CallResultSuccess){
                return convertToJSObject(r.result);
            }
            GlobalState.sendToMain(GlobalState.ADDLOGERROR,r.exception);
        }
        else {
            FieldResult r=setObjectField(target,name,val);
            if(r.type==CallResultSuccess){
                return convertToJSObject(r.result);
            }
            GlobalState.sendToMain(GlobalState.ADDLOGINFO,r.exception);
            r=setStaticField((Class<?>) target,name,val);
            if(r.type==CallResultSuccess){
                return convertToJSObject(r.result);
            }
            GlobalState.sendToMain(GlobalState.ADDLOGERROR,r.exception);
        }
        return "null";
    }
    public static Object findWindow(Object[] paras){
        if(paras.length>0) {
            if (isDebug)
                GlobalState.sendToMain(GlobalState.ADDLOGINFO,
                        "findWindow->" + pretty(paras[0]) + "\n");
            if(MainActivity.windows.get(paras[0])!=null){
                return convertToJSObject(MainActivity.windows.get(paras[0]));
            }
        }
        return "null";
    }
    public static Object release(Object[] paras){

        for(Object b:paras){
            if(b instanceof JObject){
                objects.remove(((JObject) b).id);
                if(isDebug){
                    GlobalState.sendToMain(GlobalState.ADDLOGINFO,
                            ((JObject) b).id+" is released. "+objects.size()+" remains\n");
                }
            }
            else {
                if(isDebug){
                    GlobalState.sendToMain(GlobalState.ADDLOGINFO,"release should be a Java Object");
                }
            }
        }
        return JsNullValue;
    }
    private static final String JsNullValue="null";
    private static Object convertToJavaObject(Object para) {
        if(para instanceof JObject)
            if(objects.get(((JObject) para).id)==null){
                GlobalState.sendToMain(GlobalState.ADDLOGERROR,"id "+((JObject) para).id+" is invalid!");
                return JsNullValue;
            }
            else return objects.get(((JObject) para).id);
        else
            return para;
    }
    private static Object convertToJSObject(Object para) {
        if(para instanceof Integer || para instanceof Double)
            return para;
        else{
            int id=JsJavaInterface.getNewId();
            JsJavaInterface.objects.put(id,para);
            return new JObject(id);
        }

    }

}
