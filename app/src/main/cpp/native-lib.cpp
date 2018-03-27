#include "configue.h"
#include "duktape.h"
#include <jni.h>
#include <string>
#include <map>

JavaVM *jvm;
/*
Notice we don't make the any check on ctxs
creat(id) , automatical create id with default thread id
creatNew(id, thread_id), assume creat(id) has been called and thread id is not default
creatNewWithNewEnv, similar
ctxs[heapId][threadId] -> ctx
The check process is implemented in  corresponding java wrap class
Notice from the documetns, duk_context is automatically garbage collected?
 */
std::map<int, std::map<int,duk_context*>> ctxs;

JNIEnv* getCurrentEnv(){
  /* one should cache jvm instead of env.
   */
  JNIEnv* env;
  jvm->AttachCurrentThread(&env, NULL);
  return env;
}
const char *getJavaString(jstring s, JNIEnv *env) {
  return env->GetStringUTFChars(s, 0);
}
void releaseJavaString(const char *c, jstring s, JNIEnv *env) {
  env->ReleaseStringUTFChars(s, c);
}
jstring newJavaString(const char *c, JNIEnv *env){
  return env->NewStringUTF(c);
}
void printToJsLog(const char *c){
  JNIEnv *env=getCurrentEnv();
  jclass cls = env->FindClass(GLOBALSTATE);
  jmethodID printLog = env->GetStaticMethodID(cls, JSPRINT, JSPRINTTYPE);
  jstring js=newJavaString(c, env);
  env->CallStaticVoidMethod(cls, printLog,js);
  env->DeleteLocalRef(js);
  /* get release, new delete*/
}

duk_ret_t native_print(duk_context *ctx) {
  duk_push_string(ctx, " ");
  duk_insert(ctx, 0);
  duk_join(ctx, duk_get_top(ctx) - 1);
  const char *c = duk_safe_to_string(ctx, -1);
  printToJsLog(c);
  return 0;
}

duk_ret_t eval_raw(duk_context *ctx, void *udata) {
  (void)udata;
  duk_eval(ctx);
  return 1;
}

duk_ret_t ctxs_size(duk_context *ctx){
  duk_push_int(ctx,ctxs.size());
  return 1;
}

duk_ret_t tostring_raw(duk_context *ctx, void *udata) {
  (void)udata;
  duk_to_string(ctx, -1);
  return 1;
}


const char *runString(const char *input, int id, int threadId) {  
  const char *result;
  duk_context *ctx=ctxs[id][threadId];
  duk_push_string(ctx, input);
  duk_safe_call(ctx, eval_raw, NULL, 1 /*nargs*/, 1 /*nrets*/);
  duk_safe_call(ctx, tostring_raw, NULL, 1 /*nargs*/, 1 /*nrets*/);
  result = duk_get_string(ctx, -1);
  duk_pop(ctx);
  return result;
}

  
void registerFuncGlobal(const char * name, duk_c_function func, duk_context *ctx ){
  duk_push_c_function(ctx, func, DUK_VARARGS);
  duk_put_global_string(ctx, name);  
}

/* tutorial examples*/
duk_ret_t getInt(duk_context *ctx){
  duk_push_int(ctx, 1234);
  return 1;
}
duk_ret_t isString(duk_context *ctx){
  // -1 is the last argument
  if(duk_is_string(ctx,-1)){
    duk_push_string(ctx,"is string");
  }
  else{
    duk_push_string(ctx,"not string");
  }
  return 1;
}
duk_ret_t getParaNumber(duk_context *ctx){
  // 0, 1, 2...
  // ..   -2 ,-1
  duk_idx_t idx_top=duk_get_top_index(ctx);
  if (idx_top==DUK_INVALID_INDEX){
    duk_push_int(ctx,0);
  }
  else{
    duk_push_int(ctx,idx_top+1);
  }
  return 1;
}

const char * typeToString(duk_int_t type){
  if(type==DUK_TYPE_BOOLEAN){
    return "boolean";
  }
  else if(type==DUK_TYPE_NUMBER){
    return "number";
  }
  else if(type==DUK_TYPE_OBJECT){
    return "object";
  }
  else if(type==DUK_TYPE_LIGHTFUNC){
    return "func";
  }
  else if(type==DUK_TYPE_BUFFER){
    return "buffer";
  }
  else if(type==DUK_TYPE_STRING){
    return "stirng";
  }
  else return "unknown";
  
}


duk_ret_t inspect(duk_context *ctx){ 
  duk_idx_t idx_top=duk_get_top_index(ctx);
  if (idx_top!=DUK_INVALID_INDEX){
    for(int i=0;i<idx_top+1;i++){
      printToJsLog(typeToString(duk_get_type(ctx,i)));
      printToJsLog("\n");
    }
  }
  return 0;
}
/* create a object and add properties to it*/
duk_ret_t createObj(duk_context *ctx){ 
  duk_idx_t obj_idx;
  obj_idx=duk_push_object(ctx);
  duk_push_int(ctx,456);
  duk_put_prop_string(ctx,obj_idx,"prop");
  return 1;
}
/*  pass args as obj, key1, prop1,...*/
duk_ret_t addObjPropFromList(duk_context *ctx){ 
  duk_idx_t idx_top=duk_get_top_index(ctx);
  for(int i=0;i<idx_top;i=i+2){
    duk_put_prop(ctx,0);
  }
  return 1;
}

duk_ret_t compileFunc(duk_context *ctx){
  duk_push_string(ctx,"function f(x){return x;}");
  duk_push_string(ctx,"compileFunc.js");
  duk_compile(ctx,DUK_COMPILE_FUNCTION);
  return 1;
}

/* end of tutorials*/
/* use proxy to handle arbitrary get method*/
/* the wrapped function to manipulate jni and refection*/
/* use a hash table */
/* this functions is as raw as possible, we try to implement advanced features in java or javascript side*/
/* push {__id__:id} to stack*/
void __pushObjectFromId__(duk_context *ctx, int id){
  duk_push_object(ctx);
  duk_push_string(ctx,JSJAVAOBJECTHANDLEKEY);
  duk_push_int(ctx,id);
  duk_put_prop(ctx,-3);
}
/* wrap to top of stack (assumging int) to {__id__:id}*/
duk_ret_t __wrapToObject__(duk_context *ctx, void * udata){
  int id=duk_get_int(ctx,-1);
  __pushObjectFromId__(ctx,id);
  return 1;
}
/* this does the exact opposite part,  object -> (unwraped object)  int where int is above unwrapped object  0, java object, 1, integer,   (-1,0) for unknown
   This does:
   check if it is a object (ie. wrapped from pervious Java calls), then push obj.__id__ to stakc and set t_ 
   check if it is a number, and if it is a integer value , then set t_ otherwise, treat it as double
*/

duk_ret_t __unwrapObject__(duk_context *ctx, void* t_){
  int* t=(int *)t_;
  duk_int_t type=duk_get_type(ctx,-1);
  if(type==DUK_TYPE_OBJECT){
    duk_push_string(ctx,JSJAVAOBJECTHANDLEKEY);
    duk_get_prop(ctx,-2);
    (*t)=JAVAOBJECT;
  }
  else if(type==DUK_TYPE_NUMBER){
    double result=duk_get_number(ctx,-1);
    if(result==ceil(result)){
      (*t)=JAVAINTEGER;
    }
    else{
      (*t)=JAVADOUBLE;
    }
  }
  else if(type==DUK_TYPE_STRING){
    (*t)=JAVASTRING;
  }
  else{
    duk_push_int(ctx,0);
    duk_push_int(ctx,JAVAUNKNOWN);
  }

  return 1;
}
/* string -> bare class, get the handle for class<?> object by its full name, there should be a javascript function wrap it ot a object and handle as a proxy*/

/*wrap js values in entire stack to a jobject Array and pop them*/
void __popStackToJObjectArray__(duk_context *ctx, JNIEnv *env,jobjectArray* params){
  duk_idx_t idx_top=duk_get_top_index(ctx);
  int size=idx_top+1;
  jclass intClass= env->FindClass("java/lang/Integer");
  jclass doubleClass= env->FindClass("java/lang/Double");
  jclass objClass= env->FindClass("java/lang/Object");
  jclass JObjClass = env->FindClass(JSOBJECT);
  jmethodID initInt = (env)->GetMethodID( intClass, "<init>", "(I)V");
  jmethodID initDouble = (env)->GetMethodID( doubleClass, "<init>", "(D)V");
  jmethodID initJObj = (env)->GetMethodID( JObjClass, "<init>", "(I)V");
  (*params)=env->NewObjectArray(size,objClass,NULL);
  int type;
  for(int i=size-1;i>=0;i--){
    duk_safe_call(ctx,__unwrapObject__,&type,1,1);
    jobject jp;
    if(type==JAVAINTEGER)
      jp=env->NewObject(intClass,initInt,duk_get_int(ctx,-1));
    else if(type==JAVADOUBLE)
      jp=env->NewObject(doubleClass,initDouble,duk_get_number(ctx,-1));
    else if(type==JAVASTRING)
      jp=env->NewStringUTF(duk_get_string(ctx,-1));
    else if(type==JAVAOBJECT)
      jp=env->NewObject(JObjClass,initJObj,duk_get_int(ctx,-1));
    else 
      jp=env->NewObject(intClass,initInt,duk_get_int(ctx,-1));
    env->SetObjectArrayElement(*params,i,jp);
    duk_pop(ctx);
    env->DeleteLocalRef(jp);
  }
}
/* wrap jObject into bare form. For int double, string etc, just use the javascript type*/
void __pushJObjectToStack__(duk_context *ctx, JNIEnv *env, jobject obj){
  jclass intClass= env->FindClass("java/lang/Integer");
  jclass doubleClass= env->FindClass("java/lang/Double");
  jclass stringClass= env->FindClass("java/lang/String");
  jclass objClass= env->FindClass("java/lang/Object");
  jclass JObjClass = env->FindClass(JSOBJECT);
  if(env->IsInstanceOf(obj,intClass)){    
    jmethodID intValue=env->GetMethodID(intClass,"intValue","()I");
    duk_push_int(ctx,env->CallIntMethod(obj,intValue));
  }
  else  if(env->IsInstanceOf(obj,doubleClass)){
    jmethodID doubleValue=env->GetMethodID(intClass,"doubleValue","()D");
    duk_push_number(ctx,env->CallDoubleMethod(obj,doubleValue));
  }
  else  if(env->IsInstanceOf(obj,stringClass)){
    duk_push_string(ctx,getJavaString((jstring)obj,env));
  }
  else  if(env->IsInstanceOf(obj,JObjClass)){
    jfieldID idField=env->GetFieldID(JObjClass,"id","I");
    int id=env->GetIntField(obj,idField);
    __pushObjectFromId__(ctx,id);
  }
  else
    duk_push_int(ctx,JAVAUNKNOWN);
}

duk_ret_t  __callJsJavaInterface__(duk_context *ctx, const char * methodName){
  JNIEnv *env=getCurrentEnv();
  jclass JJClass = env->FindClass(JSJAVAINTERFACE);
  jmethodID method =
    env->GetStaticMethodID(JJClass, methodName , JSJAVAMETHODTYPE);
  jobjectArray params;
  __popStackToJObjectArray__(ctx,env,&params);
  jobject result=env->CallStaticObjectMethod(JJClass, method, params);
  __pushJObjectToStack__(ctx,env,result);
  return 1;
}

duk_ret_t __loadClass__(duk_context *ctx){
  return __callJsJavaInterface__(ctx, JSJAVALOADCLASS);
}
/* (class obj, string, para1, para2, ..), there is at least two .., this is intended as more general then the orignal desing, all static, object method and field can be called in this way, to make thing simple, we treat field as method without parameters. and the jaa side will automatically determine whether we should call it staticlaly or as object method*/
duk_ret_t __call__(duk_context *ctx){
    return __callJsJavaInterface__(ctx, JSJAVACALL);
}


/* necessary c interfact to interopated with Java, I try to keep this as simple as possible*/
void initCtx(duk_context *ctx){
  registerFuncGlobal("print",native_print,ctx);
  registerFuncGlobal("__ctxs__",ctxs_size,ctx);
  registerFuncGlobal("__loadClass__", __loadClass__, ctx);
  registerFuncGlobal("__call__", __call__, ctx);
  if(TUTORIAL){
    registerFuncGlobal("getInt", getInt, ctx);
    registerFuncGlobal("isString", isString, ctx);
    registerFuncGlobal("getParaNumber", getParaNumber, ctx);
    registerFuncGlobal("inspect", inspect, ctx);
    registerFuncGlobal("createObj", createObj, ctx);
    registerFuncGlobal("addObjPropFromList", addObjPropFromList, ctx);
    registerFuncGlobal("compileFunc", compileFunc, ctx);
  }
}

extern "C" JNIEXPORT void JNICALL
Java_com_serendipity_chengzhengqian_jsmaster_JsEngine_create(JNIEnv *env,
                                                             jobject obj, jint id) {
  duk_context *ctx= duk_create_heap_default();
  ctxs[id][DEFAUlTTHREADID]=ctx;
  initCtx(ctx);
}

/* assuming threadId is different from DefaultId, and DefualtId has been created!!!
*/
extern "C" JNIEXPORT void JNICALL
Java_com_serendipity_chengzhengqian_jsmaster_JsEngine_createNewThread(JNIEnv *env,
								      jobject obj, jint id, jint threadId) {

  duk_context *default_ctx=ctxs[id][DEFAUlTTHREADID];
  duk_push_thread(default_ctx);
  ctxs[id][threadId]=duk_get_context(default_ctx,-1);
}

extern "C" JNIEXPORT void JNICALL
Java_com_serendipity_chengzhengqian_jsmaster_JsEngine_createNewThreadNewEnv(JNIEnv *env,
								      jobject obj, jint id, jint threadId) {

  duk_context* default_ctx=ctxs[id][DEFAUlTTHREADID];
  duk_push_thread_new_globalenv((default_ctx));
  duk_context* new_ctx=duk_get_context(default_ctx,-1);
  ctxs[id][threadId]=new_ctx;
}

extern "C" JNIEXPORT void JNICALL
Java_com_serendipity_chengzhengqian_jsmaster_JsEngine_destroy(JNIEnv *env,
                                                              jobject obj, jint id) {
  duk_destroy_heap(ctxs[id][DEFAUlTTHREADID]);
  ctxs.erase(ctxs.find(id));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_serendipity_chengzhengqian_jsmaster_JsEngine_runJS(JNIEnv *env,
                                                                    jobject obj,
                                                                    jstring s, jint id, jint threadId) {
  env->GetJavaVM(&jvm);
  const char *c = getJavaString(s, env);
  jstring result = newJavaString(runString(c,id,threadId),env);
  releaseJavaString(c,s,env);
  return result;
}
