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
  JNIEnv *currentEnv=getCurrentEnv();
  jclass cls = currentEnv->FindClass(GLOBALSTATE);
  jmethodID printLog = currentEnv->GetStaticMethodID(cls, JSPRINT, JSPRINTTYPE);
  currentEnv->CallStaticVoidMethod(cls, printLog, currentEnv->NewStringUTF(c));

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
  const char *res;
  duk_context *ctx=ctxs[id][threadId];
  duk_push_string(ctx, input);
  duk_safe_call(ctx, eval_raw, NULL, 1 /*nargs*/, 1 /*nrets*/);
  duk_safe_call(ctx, tostring_raw, NULL, 1 /*nargs*/, 1 /*nrets*/);
  res = duk_get_string(ctx, -1);
  duk_pop(ctx);
  return res;
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
duk_ret_t __wrapToObject__(duk_context *ctx, void * udata){
  int id=duk_get_int(ctx,-1);
  duk_push_object(ctx);
  duk_push_string(ctx,JSJAVAOBJECTHANDLEKEY);
  duk_push_int(ctx,id);
  duk_put_prop(ctx,-3);
  return 1;
}
/* this does the exact opposite part,  object -> (unwraped object)  int where int is above unwrapped object  0, java object, 1, integer,   (-1,0) for unknown*/
/* it seems safe to just return 1 value*/
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

duk_ret_t __loadClass__(duk_context *ctx){
  const char* c=duk_get_string(ctx,0);
  JNIEnv *currentEnv=getCurrentEnv();
  jclass cls = currentEnv->FindClass(JSJAVAINTERFACE);
  jmethodID loadClass = currentEnv->GetStaticMethodID(cls, JSJAVALOADCLASS , JSJAVALOADCLASSTYPE);
  int result=currentEnv->CallStaticIntMethod(cls, loadClass, currentEnv->NewStringUTF(c));
  duk_push_int(ctx,result);
  duk_safe_call(ctx,__wrapToObject__,NULL,1,1);
  return 1;
}


/* (class obj, string, para1, para2, ..), there is at least two ..*/
duk_ret_t __callStaticMethod__(duk_context *ctx){ 
  duk_idx_t idx_top=duk_get_top_index(ctx);
  int size=idx_top+1;
  JNIEnv *env=getCurrentEnv();
  jclass JJClass = env->FindClass(JSJAVAINTERFACE);
  jmethodID callStaticMethod = env->GetStaticMethodID(JJClass, JSJAVACALLSTATICMETHOD , JSJAVACALLSTATICMETHODTYPE);

  jclass intClass= env->FindClass("java/lang/Integer");
  jclass doubleClass= env->FindClass("java/lang/Double");
  jclass objClass= env->FindClass("java/lang/Object");
  jclass JObjClass = env->FindClass(JSOBJECT);
  jmethodID initInt = (env)->GetMethodID( intClass, "<init>", "(I)V");
  jmethodID initDouble = (env)->GetMethodID( doubleClass, "<init>", "(D)V");
  jmethodID initJObj = (env)->GetMethodID( JObjClass, "<init>", "(I)V");

  jobjectArray params=env->NewObjectArray(size,objClass,NULL);
  int type;
  for(int i=0;i<size;i++){

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

    env->SetObjectArrayElement(params,idx_top-i,jp);
    duk_pop(ctx);
    env->DeleteLocalRef(jp);
  }
  env->CallStaticObjectMethod(JJClass, callStaticMethod, params);
  duk_push_string(ctx,"call static");
  /* import to return this number, otherwisize*/
  return 1;
}

void initCtx(duk_context *ctx){
  registerFuncGlobal("print",native_print,ctx);
  registerFuncGlobal("ctxs_size",ctxs_size,ctx);
  registerFuncGlobal("__loadClass__", __loadClass__, ctx);
  registerFuncGlobal("__callStaticMethod__", __callStaticMethod__, ctx);
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
  duk_context *new_ctx;
  duk_context *default_ctx=ctxs[id][DEFAUlTTHREADID];
  duk_push_thread(default_ctx);
  new_ctx=duk_get_context(default_ctx,-1);
  ctxs[id][threadId]=new_ctx;
}

extern "C" JNIEXPORT void JNICALL
Java_com_serendipity_chengzhengqian_jsmaster_JsEngine_createNewThreadNewEnv(JNIEnv *env,
								      jobject obj, jint id, jint threadId) {
  duk_context *new_ctx;
  duk_context *default_ctx=ctxs[id][DEFAUlTTHREADID];
  (void)duk_push_thread_new_globalenv((default_ctx));
  new_ctx=duk_get_context(default_ctx,-1);
  ctxs[id][threadId]=new_ctx;
}

extern "C" JNIEXPORT void JNICALL
Java_com_serendipity_chengzhengqian_jsmaster_JsEngine_destroy(JNIEnv *env,
                                                              jobject obj, jint id) {
  duk_destroy_heap(ctxs[id][DEFAUlTTHREADID]);
  ctxs.erase(ctxs.find(id));
}
