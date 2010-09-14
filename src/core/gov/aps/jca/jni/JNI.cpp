/**********************************************************************
 *
 *      Original Author: Eric Boucher
 *      Date:            05/05/2003
 *
 *      Experimental Physics and Industrial Control System (EPICS)
 *
 *      Copyright 1991, the University of Chicago Board of Governors.
 *
 *      This software was produced under  U.S. Government contract
 *      W-31-109-ENG-38 at Argonne National Laboratory.
 *
 *      Beamline Controls & Data Acquisition Group
 *      Experimental Facilities Division
 *      Advanced Photon Source
 *      Argonne National Laboratory
 *
 *
 * $Id: JNI.cpp,v 1.9 2006-12-19 10:53:41 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

#define DEBUG_CLASSLOADER 0
#define DEBUG_EXCEPTION 0
#define DEBUG_THREADS 0

#define FIX_CONTEXT_CALLBACKS 1  // This needs to be removed, if not set it will fail completely

// Set if you want exceptionCheck() to be called, esp. in callbacks.
// Its only function is to print a message.  The user still has to
// handle the exception.
#define CHECK_EXCEPTION 1

#include "gov_aps_jca_jni_JNI.h"

#include <stdlib.h>
#include <epicsVersion.h>
#include <string.h>
#include <cadef.h>

#define ERROR   10
#define WARNING 20
#define INFO    30
#define VERBOSE 40

// Define this as needed for debugging
//#define LOG_LEVEL INFO

#ifdef LOG_LEVEL
#define log(level,msg) if(level<=LOG_LEVEL) printf(msg)
#define log1(level,msg,arg1) if(level<=LOG_LEVEL) printf(msg,arg1)
#define log2(level,msg,arg1,arg2) if(level<=LOG_LEVEL) printf(msg,arg1,arg2)
#else
#define log(level,msg)
#define log1(level,msg,arg1)
#define log2(level,msg,arg1,arg2)
#endif

// Required JNI version.  Current (JDK1.5) possibilities are
// JNI_VERSION_1_1, JNI_VERSION_1_2, and JNI_VERSION_1_4.  We need
// JNI_VERSION_1_2 to use functions such as GetEnv and NewWeakGlobalRef.
// We need JNI_VERSION_1_4 for AttachCurrentThreadAsDaemon.
unsigned jniVersion = JNI_VERSION_1_4;

// Pointer to Java VM
static JavaVM* _jvm = NULL;

#include <epicsThread.h>
#include <epicsAssert.h>

// Attach JVM thread flag
static epicsThreadPrivateId _jvmThreadAttached = 0;
static epicsThreadOnceId _jvmThreadIdOnce = EPICS_THREAD_ONCE_INIT;

/*
static void jniThreadCleanUpOnce(void *)
{
    if (_jvmThreadAttached) {
        epicsThreadPrivateDelete(_jvmThreadAttached);
        _jvmThreadAttached = 0;
    }
}
*/

// runs once only for each process
static void jniThreadInitOnce(void *)
{
    _jvmThreadAttached = epicsThreadPrivateCreate();
}

// Be backward compatible, EPICS3.14.9(.2) required.
#if ((EPICS_REVISION >= 14) && (EPICS_MODIFICATION >= 9))
#include <epicsExit.h>
#else
epicsShareFunc void epicsShareAPI epicsExit(int status);
epicsShareFunc int epicsShareAPI epicsAtThreadExit(
                 void (*epicsExitFunc)(void *arg),void *arg) { return 0; }
#endif

static void jniAtCAThreadExit(void *pvt) {
	_jvm->DetachCurrentThread();
}

// Must be called by every CA callback, also needed to obtain JVMEnv.
#define INITIALIZE_CALLBACK(retval) \
	JNIEnv* env = (JNIEnv*)epicsThreadPrivateGet(_jvmThreadAttached); \
	if (env == 0) { \
		int attachStatus = _jvm->AttachCurrentThreadAsDaemon((void**)&env, NULL); \
		if (attachStatus < 0) return retval; \
		epicsThreadPrivateSet(_jvmThreadAttached, (void*)env); \
		epicsAtThreadExit(jniAtCAThreadExit, (void*)0); \
	}

// Cached method IDs
static jmethodID _contextMessageMID = NULL;
static jmethodID _contextExceptionMID = NULL;
static jmethodID _connectionMID = NULL;
static jmethodID _accessRightsMID = NULL;
static jmethodID _arrayPutMID = NULL;
static jmethodID _arrayGetMID = NULL;
static jmethodID _arrayMonitorMID = NULL;

#if DEBUG_CLASSLOADER
jclass testFindClass(JNIEnv *env, const char *name) {
	print("FindClass for %s\n", name);
	jclass clazz= env->FindClass(name);
	jboolean check= env->ExceptionCheck();
	if(check == JNI_TRUE) {
		print("  FindClass failed: clazz=%p\n", (void *)clazz);
		env->ExceptionDescribe();
		env->ExceptionClear();
	} else {
		print("  class=%p\n", (void *)clazz);
	}
	return clazz;
}
#endif

#if DEBUG_THREADS  || DEBUG_EXCEPTION
// Use for debugging flexibility.  On WIN32, stderr goes to the CA
// console if CA is in debug mode, and stdout is lost (or in Eclipse
// appears after the process ends).
int print(const char *fmt, ...) {
	static char string[2048];
	va_list vargs;
	int nchars=0;

	va_start(vargs, fmt);
	nchars+=vsprintf(string, fmt,vargs);
	va_end(vargs);

#ifdef WIN32
	fprintf(stderr, string);
	fflush(stderr);
	printf(string);
	fflush(stdout);
#else
	printf(string);
	fflush(stdout);
#endif

	return nchars;
}
#endif

#if DEBUG_THREADS
void getThreadInfo(JNIEnv *env) {
	int verbose = 0;
	jmethodID methodID;

	// Find the Thread class
	const char *threadClassName = "java/lang/Thread";
	jclass threadClass= env->FindClass(threadClassName);
	if(!threadClass) {
		print("%s not found\n", threadClassName);
		env->ExceptionClear();
		goto RETURN;
	}
	// Find the current thread
	char *methodName = "currentThread";
	char *methodSignature = "()Ljava/lang/Thread;";
	methodID= env->GetStaticMethodID(threadClass, methodName, methodSignature);
	if(!methodID) {
		print("%s %s method not found\n", methodName, methodSignature);
		env->ExceptionClear();
		goto RETURN;
	} else if(verbose) {
		print("%s %s method found\n", methodName, methodSignature);
	}
	jobject currentThread = env->CallStaticObjectMethod(threadClass, methodID);
	if(!currentThread) {
		print("currentThread not found\n");
		env->ExceptionClear();
		goto RETURN;
	}

	// Get the thread name
	methodName = "getName";
	methodSignature = "()Ljava/lang/String;";
	methodID= env->GetMethodID(threadClass, methodName, methodSignature);
	if(!methodID) {
		print("%s %s method not found\n", methodName, methodSignature);
		env->ExceptionClear();
		goto RETURN;
	} else if(verbose) {
		print("%s %s method found\n", methodName, methodSignature);
	}
	jstring threadName = (jstring)env->CallObjectMethod(currentThread, methodID);
	if(!threadName) {
		print("threadName not found\n");
		env->ExceptionClear();
		goto RETURN;
	} else {
		if(verbose) print("threadName found\n");
		const char *cThreadName = env->GetStringUTFChars(threadName, NULL);
		print("Name of current thread: %s\n", cThreadName);
		env->ReleaseStringUTFChars(threadName, cThreadName);
	}

	// Get the thread group
	methodName = "getThreadGroup";
	methodSignature = "()Ljava/lang/ThreadGroup;";
	methodID= env->GetMethodID(threadClass, methodName, methodSignature);
	if(!methodID) {
		print("%s %s method not found\n", methodName, methodSignature);
		env->ExceptionClear();
		goto RETURN;
	} else if(verbose) {
		print("%s %s method found\n", methodName, methodSignature);
	}
	jobject threadGroup = env->CallObjectMethod(currentThread, methodID);
	if(!threadGroup) {
		print("threadGroup not found\n");
		env->ExceptionClear();
		goto RETURN;
	} else {
		if(verbose) print("threadGroup found\n");
	}
// Get the thread group name
	const char *threadGroupClassName = "java/lang/ThreadGroup";
	jclass threadGroupClass= env->FindClass(threadGroupClassName);
	if(!threadGroupClass) {
		print("%s not found\n", threadGroupClassName);
		env->ExceptionClear();
		goto RETURN;
	}
	methodName = "getName";
	methodSignature = "()Ljava/lang/String;";
	methodID= env->GetMethodID(threadGroupClass, methodName, methodSignature);
	if(!methodID) {
		print("%s %s method not found\n", methodName, methodSignature);
		env->ExceptionClear();
		goto RETURN;
	} else if(verbose) {
		print("%s %s method found\n", methodName, methodSignature);
	}
	jstring threadGroupName = (jstring)env->CallObjectMethod(threadGroup, methodID);
	if(!threadGroupName) {
		print("threadGroupName not found\n");
		env->ExceptionClear();
		goto RETURN;
	} else {
		if(verbose) print("threadGroupName found\n");
		const char *cThreadGroupName = env->GetStringUTFChars(threadGroupName, NULL);
		print("Name of current thread group: %s\n", cThreadGroupName);
		env->ReleaseStringUTFChars(threadName, cThreadGroupName);
	}

// Get the number of threads
	methodName = "activeCount";
	methodSignature = "()I";
	methodID= env->GetStaticMethodID(threadClass, methodName, methodSignature);
	if(!methodID) {
		print("%s %s method not found\n", methodName, methodSignature);
		env->ExceptionClear();
		goto RETURN;
	} else if(verbose) {
		print("%s %s method found\n", methodName, methodSignature);
	}
	jint activeCount = env->CallStaticIntMethod(threadClass, methodID);
	if(!activeCount) {
		print("activeCount not found\n");
		env->ExceptionClear();
		goto RETURN;
	} else {
		if(verbose) print("activeCount found\n");
		print("Number of threads: %d\n", (int)activeCount);
	}

	RETURN:
	return;
}
#endif

static void checkstatus(JNIEnv* env, int code) {
//  printf("Success: %d\n", CA_EXTRACT_SUCCESS(code));

  if (CA_EXTRACT_SUCCESS(code)) return;
//  printf("failed: %s\n", ca_message(code));

  jclass exceptionClass= env->FindClass("gov/aps/jca/jni/JNIException");
  jmethodID mid= env->GetMethodID(exceptionClass, "<init>", "(I)V");

  jthrowable cae= (jthrowable) env->NewObject(exceptionClass,mid,CA_EXTRACT_MSG_NO(code));
  env->Throw(cae);
}


#define MAX_ENV_LENGTH 512

#ifdef _WIN32
  #define putenv(env) _putenv(env)
#elif defined(SOLARIS)


#define NB_ENV_INC  5
static int _nb_env= 10;

static char** _env_ptr=NULL;

static char* getEnvPtr(const char* ptr) {
  int t;
  // Initialize the local environment
  if(_env_ptr==NULL) {
    log(VERBOSE, "Allocating local environment table\n");
    _env_ptr= new char*[_nb_env];
    for(t=0; t<_nb_env; ++t) _env_ptr[t]=NULL;
  }
  if(ptr!=NULL) {
    // Search if ptr is already part of the environment.
    for(t=0; t<_nb_env; ++t) {
      // If yes return the same ptr to overwrite it.
      if(_env_ptr[t]==ptr) {
        log2(VERBOSE, "Overwriting local variable [%X]: %s\n", ptr, ptr);
        return (char*)ptr;
      }
    }
  }

  // ptr was not part of the environment.
  // Search a free pointer.
  for(t=0; t<_nb_env; ++t) {
    // We found an empty pointer. Return it.
    if(_env_ptr[t]!=NULL && strlen(_env_ptr[t])==0) {
      log1(VERBOSE, "Returning unused local variable [%X]\n", _env_ptr[t]);
      return _env_ptr[t];
    }
    // We found a NULL pointer. Allocate the memory for the new environment and return the pointer.
    if(_env_ptr[t]==NULL) {
      _env_ptr[t]= new char[MAX_ENV_LENGTH];
      log1(VERBOSE, "Allocating new local variable [%X]\n", _env_ptr[t]);
      return _env_ptr[t];
    }
  }

  // No pointer left. Increase the size of the local environment.
  log(VERBOSE, "Increasing local environment table size\n");
  char** old_env_ptr= _env_ptr;
  _env_ptr= new char*[_nb_env+NB_ENV_INC];
  // Copy the pointers from the old environment to the new environment.
  for(t=0; t<_nb_env; ++t) {
    _env_ptr[t]=old_env_ptr[t];
  }
  delete old_env_ptr;

  t=_nb_env;
  _nb_env+= NB_ENV_INC;
  // Allocate a new pointer and return it.
  _env_ptr[t]= new char[MAX_ENV_LENGTH];
  log1(VERBOSE, "Allocating new local variable [%X]\n", _env_ptr[t]);
  return _env_ptr[t];
}

// Mark an env pointer as available.
static void freeEnvPtr(const char* ptr) {
  for(int t=0; t<_nb_env; ++t) {
    if(_env_ptr[t]==ptr) {
      log1(VERBOSE, "Marking local variable as unused [%X]\n", ptr);
      _env_ptr[t][0]='\0';
    }
  }
}
#endif


JNIEXPORT void JNICALL Java_gov_aps_jca_jni_JNI__1setenv(JNIEnv *env, jclass, jstring jname, jstring jvalue) {

  const char*name=env->GetStringUTFChars( jname, NULL );
  const char*value=env->GetStringUTFChars( jvalue, NULL );

  log2(VERBOSE, "Setting env: %s=%s\n", name, value);


#if defined(_WIN32)
  // On win32 systems, the _putenv function seems to make it's own copy of the env variable.
  // I'm just hopping it also free the copy when the env variable is removed.
  char buff[MAX_ENV_LENGTH+1];

  strncpy(buff, name, MAX_ENV_LENGTH-1);
  if(strlen(value)>0) {
    strncat(buff, "=", MAX_ENV_LENGTH-1-strlen(name));
    strncat(buff, value, MAX_ENV_LENGTH-2-strlen(name));
  }
  putenv(buff);

#elif defined(SOLARIS)
  // setenv is BSD 4.3 and is not in stdlib.h on Solaris, so use putenv
  char*env_ptr=getenv( name );
  if( env_ptr!=NULL ) {
    env_ptr-=strlen( name )+1;
  }
  env_ptr=getEnvPtr( env_ptr );

  strncpy( env_ptr, name, MAX_ENV_LENGTH-1 );
  if(strlen(value)>0) {
    strncat(env_ptr, "=", MAX_ENV_LENGTH-1-strlen(name));
    strncat(env_ptr, value, MAX_ENV_LENGTH-2-strlen(name));
  }
  putenv(env_ptr);

  if(strlen(value)==0) freeEnvPtr(env_ptr);

#else
  setenv( name, value, 1 );

#endif

  env->ReleaseStringUTFChars(jname, name);
  env->ReleaseStringUTFChars(jvalue, value);
}


JNIEXPORT jint JNICALL Java_gov_aps_jca_jni_JNI__1ca_1getVersion(JNIEnv* env, jclass) {
  return EPICS_VERSION;
}

JNIEXPORT jint JNICALL Java_gov_aps_jca_jni_JNI__1ca_1getRevision(JNIEnv* env, jclass) {
  return EPICS_REVISION;
}

JNIEXPORT jint JNICALL Java_gov_aps_jca_jni_JNI__1ca_1getModification(JNIEnv* env, jclass) {
  return EPICS_MODIFICATION;
}

JNIEXPORT jstring JNICALL Java_gov_aps_jca_jni_JNI__1ca_1getVersionString(JNIEnv *env, jclass) {
  return env->NewStringUTF(EPICS_VERSION_STRING);
}

JNIEXPORT jstring JNICALL Java_gov_aps_jca_jni_JNI__1ca_1getReleaseString(JNIEnv *env, jclass) {
  return env->NewStringUTF(epicsReleaseVersion);
}

static int cacheIDs(JNIEnv *env, jmethodID *mid, const char *className,
	const char *methodName, const char *methodSignature) {

	jclass cls;
	cls = env->FindClass(className);
	if(!cls) goto ERR;

	*mid = env->GetMethodID(cls, methodName, methodSignature);
	if(!*mid) goto ERR;

	return JNI_OK;

	ERR:
#if DEBUG_CLASSLOADER
	print("cacheIDs: Error occurred\n");
	print("  className:       %s\n", className);
	print("  methodName:      %s\n", methodName);
	print("  methodSignature: %s\n", methodSignature);
#endif

#if CHECK_EXCEPTION
	jboolean check= env->ExceptionCheck();
	if(check == JNI_TRUE) {
		fprintf(stderr, "cacheIDs: Exception occurred\n");
		fprintf(stderr, "  className:       %s\n", className);
		fprintf(stderr, "  methodName:      %s\n", methodName);
		fprintf(stderr, "  methodSignature: %s\n", methodSignature);
		env->ExceptionDescribe();
	}
#endif

	return JNI_ERR;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {

	log1(INFO, "\nJNI_OnLoad epicsThread= %s\n", epicsThreadGetNameSelf());

#if DEBUG_CLASSLOADER
	print("OnLoad: Entered\n");
#endif
	// Check if this version is supported
	JNIEnv *env;
	if(vm->GetEnv((void **)&env, jniVersion)) {
	    printf("This JVM version is not supported!\n");
		return JNI_ERR;
	}

	// Store the JavaVM pointer
	_jvm = vm;

	// Cache the jclass'es and jmethodID's of the JCA C handlers.
	// This is more efficient, but it is also necessary as using
	// FindClass in these callbacks may not find the classes.  This
	// happens in Eclipse, which uses its own class loader and also
	// sets the classpath to something that does not include the JCA
	// classes.  Use weak global references so the Java classes can be
	// garbage collected.
	int status;

	status=cacheIDs(env, &_contextMessageMID,
		"gov/aps/jca/jni/JNIContextMessageCallback",
		"fire", "(Ljava/lang/String;)V");
	if(status == JNI_ERR) return JNI_ERR;

	status=cacheIDs(env, &_contextExceptionMID,
		"gov/aps/jca/jni/JNIContextExceptionCallback",
		"fire", "(JIIJLjava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");
	if(status == JNI_ERR) return JNI_ERR;

	status=cacheIDs(env, &_connectionMID,
		"gov/aps/jca/jni/JNIConnectionCallback",
		"fire", "(Z)V");
	if(status == JNI_ERR) return JNI_ERR;

	status=cacheIDs(env, &_accessRightsMID,
		"gov/aps/jca/jni/JNIAccessRightsCallback",
		"fire", "(ZZ)V");
	if(status == JNI_ERR) return JNI_ERR;

	status=cacheIDs(env, &_arrayPutMID,
		"gov/aps/jca/jni/JNIPutCallback",
		"fire", "(IIJI)V");
	if(status == JNI_ERR) return JNI_ERR;

	status=cacheIDs(env, &_arrayGetMID,
		"gov/aps/jca/jni/JNIGetCallback",
		"fire", "(IIJI)V");
	if(status == JNI_ERR) return JNI_ERR;

	status=cacheIDs(env, &_arrayMonitorMID,
		"gov/aps/jca/jni/JNIMonitorCallback",
		"fire", "(IIJI)V");
	if(status == JNI_ERR) return JNI_ERR;

#if DEBUG_CLASSLOADER
	print("OnLoad: Completed successfully\n");
#endif
	return jniVersion;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {

	log1(INFO, "\nJNI_OnUnload epicsThread= %s\n", epicsThreadGetNameSelf());

	return ;
}

#if FIX_CONTEXT_CALLBACKS
// Storing these data in thread-private storage does not work for
// passing them to callbacks.  The callbacks may come back in a
// different thread.  It does work for keeping track of GlobalRef's.
static epicsThreadPrivateId messageCallbackID;
static epicsThreadPrivateId exceptionCallbackID;

// For the exception callback, the callback ID may be passed via the
// userargs.  For the message callback, we keep a global variable,
// although this makes it per process and is not very satisfactory.
static jobject globalMessageCallbackID = NULL;
#endif

extern "C" {
  static int theMessageCallback(const char* pFormat, va_list args) {

    log1(INFO, "\nMessageCallback epicsThread= %s\n", epicsThreadGetNameSelf());

#if FIX_CONTEXT_CALLBACKS
    jobject callback= (jobject) epicsThreadPrivateGet(messageCallbackID);
		// If this didn't work, try the global variable
		if(!callback) callback= globalMessageCallbackID;
#endif

#if DEBUG_EXCEPTION
		print("theMessageCallback: callback = 0x%p\n", (void *)callback);
    char buf0[1024];
    sprintf(buf0, pFormat, args);
		print(buf0);
#endif

    if (callback==NULL) return ECA_NORMAL;

	INITIALIZE_CALLBACK(ECA_NORMAL);

    char buf[1024];
    sprintf(buf, pFormat, args);
    jstring msg= env->NewStringUTF(buf);

    env->CallVoidMethod(callback, _contextMessageMID, msg);

#if CHECK_EXCEPTION
		jboolean check= env->ExceptionCheck();
		if(check == JNI_TRUE) {
			fprintf(stderr, "theMessageCallback: Exception occurred\n");
			env->ExceptionDescribe();
		}
#endif

    return ECA_NORMAL;
  }


  static void theExceptionCallback(struct exception_handler_args args) {

    log1(INFO, "\nExceptionCallback epicsThread= %s\n", epicsThreadGetNameSelf());

#if FIX_CONTEXT_CALLBACKS
    jobject callback= (jobject) args.usr;
#endif

#if DEBUG_EXCEPTION
		print("theExceptionCallback: callback = 0x%p\n", (void *)callback);
#endif

    if (callback==NULL) return;

	INITIALIZE_CALLBACK();

    jstring msg=env->NewStringUTF(ca_message(args.stat));
    jstring ctxtInfo=NULL;
    if (args.ctx!=NULL) ctxtInfo= env->NewStringUTF(args.ctx);
    jstring file=NULL;
    if (args.pFile!=NULL) file= env->NewStringUTF(args.pFile);

    env->CallVoidMethod(callback, _contextExceptionMID,  (jlong) args.chid,
                        (jint) args.type,
                        (jint) args.count,
                        (jlong) args.addr,
                        (jstring) msg,
                        (jstring) ctxtInfo,
                        (jstring) file,
                        (jint) args.lineNo );

#if CHECK_EXCEPTION
		jboolean check= env->ExceptionCheck();
		if(check == JNI_TRUE) {
			fprintf(stderr, "theExceptionCallback: Exception occurred\n");
			env->ExceptionDescribe();
		}
#endif

  }
} /* extern "C" */


#if FIX_CONTEXT_CALLBACKS
static void threadStorageInit(void *arg) {
	messageCallbackID= epicsThreadPrivateCreate();
	exceptionCallbackID= epicsThreadPrivateCreate();
}
#endif

// There can be only one context per thread, but it can be created and
// destroyed multiple times.  There can be simultaneous contexts in
// separate threads.
JNIEXPORT jlong JNICALL Java_gov_aps_jca_jni_JNI__1ctxt_1contextCreate(JNIEnv *env, jclass, jboolean preemptive_callback) {

#if DEBUG_THREADS
    print("contextCreate: epicsThread= %s\n", epicsThreadGetNameSelf());
		getThreadInfo(env);
#endif

	// Initialize callback TTS for callbacks
    epicsThreadOnce (&_jvmThreadIdOnce, jniThreadInitOnce, (void*)0);

#if FIX_CONTEXT_CALLBACKS
	// Create EPICS thread-private storage for the message and exception
	// callbacks.  This is done once per process.  The thread-private
	// storage cannot be used to pass the methodIDs to the callback
	// because the callbacks may happen in different threads from this
	// one.  This storage is used for deleting global references only.
	// The exceptionMethodID is passed via userargs to
	// ca_add_exception_event.  There is no similar userargs for
	// ca_replace_printf_handler.
	static epicsThreadOnceId threadStorageOnceFlag = EPICS_THREAD_ONCE_INIT;
	epicsThreadOnce(&threadStorageOnceFlag, threadStorageInit, NULL);

	// Zero the storage for this thread (no callbacks until they are set)
#if 0
	// Not sure what to do here
	globalMessageCallbackID = NULL;
#endif
	epicsThreadPrivateSet(messageCallbackID, NULL);
	epicsThreadPrivateSet(exceptionCallbackID, NULL);
#endif

  ca_preemptive_callback_select ca_mode;

  if(preemptive_callback) ca_mode= ca_enable_preemptive_callback;
  else ca_mode= ca_disable_preemptive_callback;

#if DEBUG_EXCEPTION
	print("contextCreate: ca_context_create\n");
#endif
  checkstatus( env, ca_context_create(ca_mode) );

  return (jlong) ca_current_context();

}


JNIEXPORT void JNICALL Java_gov_aps_jca_jni_JNI__1ctxt_1contextDestroy(JNIEnv *env, jclass) {

#if DEBUG_THREADS
	print("contextCreate: epicsThread= %s\n", epicsThreadGetNameSelf());
	getThreadInfo(env);
#endif

#if FIX_CONTEXT_CALLBACKS
	jobject msg= (jobject) epicsThreadPrivateGet(messageCallbackID);
	jobject excpt= (jobject) epicsThreadPrivateGet(exceptionCallbackID);
#endif

#if DEBUG_EXCEPTION
	print("contextDestroy: ca_context_destroy\n");
#endif
	ca_context_destroy() ;

	if (msg!=NULL) {
		env->DeleteGlobalRef(msg);
#if FIX_CONTEXT_CALLBACKS
#if 0
		// Not sure what to do here
		globalMessageCallbackID = NULL;
#endif
		epicsThreadPrivateSet(messageCallbackID, NULL);
#endif
	}
  if (excpt!=NULL) {
		env->DeleteGlobalRef(excpt);
#if FIX_CONTEXT_CALLBACKS
		epicsThreadPrivateSet(exceptionCallbackID, NULL);
#endif
#if DEBUG_EXCEPTION
		print("  messageCallbackID=0x%p exceptionCallbackID=0x%p\n",
			(void *)messageCallbackID, (void *)exceptionCallbackID);
#endif
	}
}


JNIEXPORT void JNICALL Java_gov_aps_jca_jni_JNI__1ctxt_1setMessageCallback(JNIEnv *env, jclass, jobject l) {

#if FIX_CONTEXT_CALLBACKS
  jobject old= (jobject) epicsThreadPrivateGet(messageCallbackID);
#endif

  if (l!=NULL) l= env->NewGlobalRef(l);

#if DEBUG_EXCEPTION
	print("setMessageCallback: callback = 0x%p old = 0x%p\n",
		(void *)l, (void *)old);
#endif

#if FIX_CONTEXT_CALLBACKS
	globalMessageCallbackID = l;
  epicsThreadPrivateSet(messageCallbackID, (void*) l);
#endif

  if (old!=NULL) env->DeleteGlobalRef(old);

  if (l!=NULL) {
    checkstatus( env, ca_replace_printf_handler(&theMessageCallback) );
  } else {
    checkstatus( env, ca_replace_printf_handler(NULL) );
  }

}


JNIEXPORT void JNICALL Java_gov_aps_jca_jni_JNI__1ctxt_1setExceptionCallback(JNIEnv *env, jclass, jobject l) {
#if FIX_CONTEXT_CALLBACKS
  jobject old= (jobject) epicsThreadPrivateGet(exceptionCallbackID);
#endif

  if (l!=NULL) l= env->NewGlobalRef(l);

#if FIX_CONTEXT_CALLBACKS
  epicsThreadPrivateSet(exceptionCallbackID, (void*) l);
#endif

#if DEBUG_EXCEPTION
		print("setExceptionCallback: callback = 0x%p old = 0x%p\n",
			(void *)l, (void *)old);
#endif

  if (old!=NULL) env->DeleteGlobalRef(old);

  if (l!=NULL) {
#if FIX_CONTEXT_CALLBACKS
		// Pass the callback ID as userargs.  It cannot be obtained from
		// the thread-private storage if the callback comes in a different
		// thread.
    checkstatus( env, ca_add_exception_event(&theExceptionCallback, (void *)l) );
#endif
  } else {
    checkstatus( env, ca_add_exception_event(NULL, NULL) );
  }

}





JNIEXPORT void JNICALL Java_gov_aps_jca_jni_JNI__1ctxt_1pendIO(JNIEnv *env, jclass, jdouble timeout) {
  checkstatus( env, ca_pend_io(timeout) );
}

JNIEXPORT jboolean JNICALL Java_gov_aps_jca_jni_JNI__1ctxt_1testIO(JNIEnv *env, jclass) {
  int status= ca_test_io();
  checkstatus( env, status );
  return status==ECA_IOINPROGRESS;
}

JNIEXPORT void JNICALL Java_gov_aps_jca_jni_JNI__1ctxt_1pendEvent(JNIEnv* env, jclass, jdouble timeout) {
  checkstatus( env, ca_pend_event(timeout) );
}

JNIEXPORT void JNICALL Java_gov_aps_jca_jni_JNI__1ctxt_1poll(JNIEnv* env, jclass) {
  checkstatus( env, ca_poll() );
}

JNIEXPORT void JNICALL Java_gov_aps_jca_jni_JNI__1ctxt_1flushIO(JNIEnv* env, jclass) {
  checkstatus( env, ca_flush_io() );
}

JNIEXPORT void JNICALL Java_gov_aps_jca_jni_JNI__1ctxt_1attachThread(JNIEnv *env, jclass, jlong ctxtID) {
  checkstatus( env, ca_attach_context( (ca_client_context*)ctxtID ) );
}


/**************************************************
 *
 *          CHANNEL
 *
 **************************************************/

struct usrarg {
  jobject cnxCallback;
  jobject accessCallback;
  usrarg() : cnxCallback(NULL), accessCallback(NULL) {
  }
};


#define CHID(pchid) (*((chid*)pchid))
#define PCHID(pchid) ((chid*)pchid)

#define CNXCALLBACK(chid) ((jobject)((usrarg*)ca_puser(chid))->cnxCallback)
#define SET_CNXCALLBACK(chid, cb) ((usrarg*)ca_puser(chid))->cnxCallback=cb
#define ACCESSCALLBACK(chid) ((jobject)((usrarg*)ca_puser(chid))->accessCallback)
#define SET_ACCESSCALLBACK(chid, cb) ((usrarg*)ca_puser(chid))->accessCallback=cb

extern "C" {
  static void theConnectionCallback(struct connection_handler_args args) {
#if DEBUG_CLASSLOADER
		print("the ConnectionCallback: Entered\n");
#endif
    if (CNXCALLBACK(args.chid)==NULL) return;

    log1(INFO, "\nConnectionCallback epicsThread= %s\n", epicsThreadGetNameSelf());

	INITIALIZE_CALLBACK();

    env->CallVoidMethod(CNXCALLBACK(args.chid), _connectionMID, args.op==CA_OP_CONN_UP);

#if CHECK_EXCEPTION
		jboolean check= env->ExceptionCheck();
		if(check == JNI_TRUE) {
			fprintf(stderr, "theConnectionCallback: Exception occurred\n");
			env->ExceptionDescribe();
		}
#endif

#if DEBUG_CLASSLOADER
		print("the ConnectionCallback: Completed successfully\n");
#endif
}

  static void theAccessRightsCallback(struct access_rights_handler_args args) {
    if (ACCESSCALLBACK(args.chid)==NULL) return;

    log1(INFO, "\nAccessRightsCallback epicsThread= %s\n", epicsThreadGetNameSelf());

 	INITIALIZE_CALLBACK();

    env->CallVoidMethod(ACCESSCALLBACK(args.chid), _accessRightsMID,
			args.ar.read_access==1, args.ar.write_access==1);

#if CHECK_EXCEPTION
		jboolean check= env->ExceptionCheck();
		if(check == JNI_TRUE) {
			fprintf(stderr, "theAccessRightsCallback: Exception occurred\n");
			env->ExceptionDescribe();
		}
#endif

}

  static void theArrayPutCallback(struct event_handler_args args) {

    log1(INFO, "\nPutCallback epicsThread= %s\n", epicsThreadGetNameSelf());

	INITIALIZE_CALLBACK();

    env->CallVoidMethod((jobject) args.usr, _arrayPutMID,
			args.type, args.count, (jlong) args.dbr, CA_EXTRACT_MSG_NO(args.status));
    env->DeleteGlobalRef((jobject)args.usr);

#if CHECK_EXCEPTION
		jboolean check= env->ExceptionCheck();
		if(check == JNI_TRUE) {
			fprintf(stderr, "theArrayPutCallback: Exception occurred\n");
			env->ExceptionDescribe();
		}

  }
#endif

  static void theArrayGetCallback(struct event_handler_args args) {

    log1(INFO, "\nGetCallback epicsThread= %s\n", epicsThreadGetNameSelf());

	INITIALIZE_CALLBACK();

    env->CallVoidMethod((jobject) args.usr, _arrayGetMID,
			args.type, args.count, (jlong) args.dbr, CA_EXTRACT_MSG_NO(args.status));
    env->DeleteGlobalRef((jobject)args.usr);

#if CHECK_EXCEPTION
		jboolean check= env->ExceptionCheck();
		if(check == JNI_TRUE) {
			fprintf(stderr, "theArrayGetCallback: Exception occurred\n");
			env->ExceptionDescribe();
		}
#endif

  }


  static void theArrayMonitorCallback(struct event_handler_args args) {

    log1(INFO, "\nMonitorCallback epicsThread= %s\n", epicsThreadGetNameSelf());

	INITIALIZE_CALLBACK();

    env->CallVoidMethod((jobject) args.usr, _arrayMonitorMID,
			args.type, args.count, (jlong) args.dbr, CA_EXTRACT_MSG_NO(args.status));

#if CHECK_EXCEPTION
		jboolean check= env->ExceptionCheck();
		if(check == JNI_TRUE) {
			fprintf(stderr, "theArrayMonitorCallback: Exception occurred\n");
			env->ExceptionDescribe();
		}
#endif

  }
} /* extern "C" */



JNIEXPORT jlong JNICALL Java_gov_aps_jca_jni_JNI__1ch_1channelCreate(JNIEnv *env, jclass, jstring name, jobject l, jshort priority) {
  int status;
  chid* pchid= new chid;
  usrarg* pusrarg= new usrarg;

  const char* pvname= env->GetStringUTFChars(name,NULL);

  if (l!=NULL) {
    pusrarg->cnxCallback= env->NewGlobalRef(l);
    status= ca_create_channel(pvname, &theConnectionCallback, (void*) pusrarg, priority, PCHID(pchid));
  } else {
    status= ca_create_channel(pvname, NULL, (void*) pusrarg, priority, PCHID(pchid));
  }

  env->ReleaseStringUTFChars(name, pvname);

  checkstatus( env, status );

  if (status==ECA_NORMAL) return (jlong)pchid;

  delete pchid;
  delete pusrarg;
  return 0;
}


JNIEXPORT void JNICALL Java_gov_aps_jca_jni_JNI__1ch_1setConnectionCallback(JNIEnv *env, jclass, jlong channelID, jobject l) {
  jobject oldcb= CNXCALLBACK(CHID(channelID));

  int status;
  if (l!=NULL) {
    SET_CNXCALLBACK(CHID(channelID), env->NewGlobalRef(l));
    status= ca_change_connection_event(CHID(channelID), &theConnectionCallback);
  } else {
    SET_CNXCALLBACK(CHID(channelID), NULL);
    status= ca_change_connection_event(CHID(channelID), NULL);
  }

  if (oldcb!=NULL) env->DeleteGlobalRef(oldcb);

  checkstatus( env, status );
}


JNIEXPORT void JNICALL Java_gov_aps_jca_jni_JNI__1ch_1setAccessRightsCallback(JNIEnv *env, jclass, jlong channelID, jobject l) {
  if (ACCESSCALLBACK(CHID(channelID))!=NULL) {
    env->DeleteGlobalRef(ACCESSCALLBACK(CHID(channelID)));
  }

  if (l!=NULL) {
    SET_ACCESSCALLBACK(CHID(channelID), env->NewGlobalRef(l));
    checkstatus( env, ca_replace_access_rights_event(CHID(channelID), &theAccessRightsCallback) );
  } else {
    SET_ACCESSCALLBACK(CHID(channelID), NULL);
    checkstatus( env, ca_replace_access_rights_event(CHID(channelID), NULL) );
  }
}


JNIEXPORT void JNICALL Java_gov_aps_jca_jni_JNI__1ch_1channelDestroy(JNIEnv *env, jclass, jlong channelID) {

  if (CNXCALLBACK(CHID(channelID))!=NULL) env->DeleteGlobalRef(CNXCALLBACK(CHID(channelID)));

  delete (usrarg*)ca_puser(CHID(channelID));

  checkstatus( env, ca_clear_channel(CHID(channelID)) );

  delete PCHID(channelID);
}


JNIEXPORT jint JNICALL Java_gov_aps_jca_jni_JNI__1ch_1getFieldType(JNIEnv* env, jclass, jlong channelID) {
  return(jint) ca_field_type(CHID(channelID));
}

JNIEXPORT jint JNICALL Java_gov_aps_jca_jni_JNI__1ch_1getElementCount(JNIEnv* env, jclass, jlong channelID) {
  return(jint) ca_element_count(CHID(channelID));
}

JNIEXPORT jint JNICALL Java_gov_aps_jca_jni_JNI__1ch_1getState(JNIEnv* env, jclass, jlong channelID) {
  return(jint) ca_state(CHID(channelID));
}

JNIEXPORT jstring JNICALL Java_gov_aps_jca_jni_JNI__1ch_1getHostName(JNIEnv *env, jclass, jlong channelID) {
  return env->NewStringUTF(ca_host_name(CHID(channelID)));
}

JNIEXPORT jboolean JNICALL Java_gov_aps_jca_jni_JNI__1ch_1getReadAccess(JNIEnv* env, jclass, jlong channelID) {
  return ca_read_access(CHID(channelID));
}

JNIEXPORT jboolean JNICALL Java_gov_aps_jca_jni_JNI__1ch_1getWriteAccess(JNIEnv* env, jclass, jlong channelID) {
  return ca_write_access(CHID(channelID));
}



JNIEXPORT void JNICALL Java_gov_aps_jca_jni_JNI__1ch_1arrayPut(JNIEnv *env, jclass, jint type, jint length, jlong channelID, jlong dbrID) {
  checkstatus( env,  ca_array_put(type,length, CHID(channelID), (void*)dbrID) );
}

JNIEXPORT void JNICALL Java_gov_aps_jca_jni_JNI__1ch_1arrayPutCallback(JNIEnv *env, jclass, jint type, jint length, jlong channelID, jlong dbrID, jobject callback) {
  jobject cb= env->NewGlobalRef(callback);
  int status= ca_array_put_callback(type,length,CHID(channelID), (void*)dbrID, &theArrayPutCallback, cb);
  checkstatus( env, status  );

  if (status!=ECA_NORMAL) env->DeleteGlobalRef(cb);
}

JNIEXPORT void JNICALL Java_gov_aps_jca_jni_JNI__1ch_1arrayGet(JNIEnv *env, jclass, jint type, jint length, jlong channelID, jlong dbrID) {
  checkstatus( env, ca_array_get(type,length, CHID(channelID), (void*)dbrID) );
}

JNIEXPORT void JNICALL Java_gov_aps_jca_jni_JNI__1ch_1arrayGetCallback(JNIEnv *env, jclass, jint type, jint length, jlong channelID, jobject callback) {
  jobject cb= env->NewGlobalRef(callback);
  checkstatus( env,  ca_array_get_callback(type,length, CHID(channelID), &theArrayGetCallback, cb) );
}


struct MonitorID {
  jobject callback;
  evid    pevid;
};


JNIEXPORT jlong JNICALL Java_gov_aps_jca_jni_JNI__1ch_1addMonitor(JNIEnv *env, jclass, jint type, jint length, jlong channelID, jobject callback, jint mask) {
  MonitorID* monitorID= new MonitorID;
  monitorID->callback= env->NewGlobalRef(callback);
  checkstatus( env, ca_add_masked_array_event(type,length,CHID(channelID), &theArrayMonitorCallback, monitorID->callback,0,0,0,&monitorID->pevid,mask));
  return (jlong)monitorID;
}

JNIEXPORT void JNICALL Java_gov_aps_jca_jni_JNI__1ch_1clearMonitor(JNIEnv *env, jclass, jlong monitorID) {
  checkstatus( env, ca_clear_event(((MonitorID*)monitorID)->pevid) );
  env->DeleteGlobalRef(((MonitorID*)monitorID)->callback);
  // Delete monitor allocated in addMonitor to prevent memory leak
  delete ((MonitorID*)monitorID);
}




/********************************************
 *
 *                 DBR
 *
 ********************************************/

jobject newByte(JNIEnv* env, jbyte value) {
  jclass clazz= env->FindClass("java/lang/Byte");
  jmethodID init= env->GetMethodID(clazz, "<init>", "(B)V");
  return env->NewObject(clazz, init, value);
}

jobject newShort(JNIEnv* env, jshort value) {
  jclass clazz= env->FindClass("java/lang/Short");
  jmethodID init= env->GetMethodID(clazz, "<init>", "(S)V");
  return env->NewObject(clazz, init, value);
}

jobject newInt(JNIEnv* env, jint value) {
  jclass clazz= env->FindClass("java/lang/Integer");
  jmethodID init= env->GetMethodID(clazz, "<init>", "(I)V");
  return env->NewObject(clazz, init, value);
}

jobject newFloat(JNIEnv* env, jfloat value) {
  jclass clazz= env->FindClass("java/lang/Float");
  jmethodID init= env->GetMethodID(clazz, "<init>", "(F)V");
  return env->NewObject(clazz, init, value);
}

jobject newDouble(JNIEnv* env, jdouble value) {
  jclass clazz= env->FindClass("java/lang/Double");
  jmethodID init= env->GetMethodID(clazz, "<init>", "(D)V");
  return env->NewObject(clazz, init, value);
}


jobject newTimeStamp(JNIEnv* env, epicsTimeStamp stamp) {
  jclass clazz= env->FindClass("gov/aps/jca/dbr/TimeStamp");
  jmethodID  init= env->GetMethodID(clazz, "<init>", "(JJ)V");
  jlong sec= stamp.secPastEpoch;
  jlong nsec= stamp.nsec;
  return env->NewObject(clazz, init, sec, nsec);
}

jobjectArray newLabels(JNIEnv* env, short nbstrs, char strs[MAX_ENUM_STATES][MAX_ENUM_STRING_SIZE]) {
  jclass clazz= env->FindClass("java/lang/String");

  jobjectArray res= (jobjectArray)env->NewObjectArray(nbstrs, clazz, NULL);

  for (int t=0; t<nbstrs; ++t) {
    env->SetObjectArrayElement(res, t, env->NewStringUTF(strs[t]));
  }

  return res;
}


jstring newUnits(JNIEnv* env, char units[MAX_UNITS_SIZE]) {
  return env->NewStringUTF(units);
}




JNIEXPORT jlong JNICALL Java_gov_aps_jca_jni_JNI__1dbr_1create(JNIEnv* env, jclass, jint type, jint length) {
  return (jlong) malloc(dbr_size_n(type,length));
}

JNIEXPORT void JNICALL Java_gov_aps_jca_jni_JNI__1dbr_1destroy(JNIEnv* env, jclass, jlong dbrid) {
  free((void*)dbrid);
}


JNIEXPORT void JNICALL Java_gov_aps_jca_jni_JNI__1dbr_1setValue(JNIEnv *env, jclass, jlong dbrid, jint type, jint length, jobject value) {

  void* pvalue= dbr_value_ptr(dbrid,type);

  if (dbr_type_is_CHAR(type)) {
    env->GetByteArrayRegion((jbyteArray)value, 0,length, (jbyte*)pvalue);
  } else if (dbr_type_is_SHORT(type)) {
    env->GetShortArrayRegion((jshortArray)value, 0,length, (jshort*)pvalue);
  } else if (dbr_type_is_LONG(type)) {
    env->GetIntArrayRegion((jintArray)value, 0,length, (jint*)pvalue);
  } else if (dbr_type_is_FLOAT(type)) {
    env->GetFloatArrayRegion((jfloatArray)value, 0,length, (jfloat*)pvalue);
  } else if (dbr_type_is_DOUBLE(type)) {
    env->GetDoubleArrayRegion((jdoubleArray)value, 0,length, (jdouble*)pvalue);
  } else if (dbr_type_is_STRING(type)) {
    dbr_string_t* pstrs= (dbr_string_t*)pvalue;
    const char* str;
    for (int t=0; t<length; ++t) {
      jstring jstr= (jstring)  env->GetObjectArrayElement((jobjectArray)value,t);
      str= env->GetStringUTFChars(jstr , NULL);
      strncpy(pstrs[t],str,MAX_STRING_SIZE-1);
    }
  } else if (dbr_type_is_ENUM(type)) {
    env->GetShortArrayRegion((jshortArray)value, 0,length, (jshort*)pvalue);
  }
}



JNIEXPORT jobject JNICALL Java_gov_aps_jca_jni_JNI__1dbr_1getValue(JNIEnv *env, jclass, jlong dbrid, jint type, jint length, jobject value) {
  void* pvalue= dbr_value_ptr(dbrid,type);

  if (value==NULL) {
    if (dbr_type_is_CHAR(type)) {
      value= env->NewByteArray(length);
    } else if (dbr_type_is_SHORT(type)) {
      value= env->NewShortArray(length);
    } else if (dbr_type_is_LONG(type)) {
      value= env->NewIntArray(length);
    } else if (dbr_type_is_FLOAT(type)) {
      value= env->NewFloatArray(length);
    } else if (dbr_type_is_DOUBLE(type)) {
      value= env->NewDoubleArray(length);
    } else if (dbr_type_is_STRING(type)) {
      value= env->NewObjectArray(length, env->FindClass("java/lang/String"), NULL);
    } else if (dbr_type_is_ENUM(type)) {
      value= env->NewShortArray(length);
    }
  }

  if (dbr_type_is_CHAR(type)) {
    env->SetByteArrayRegion((jbyteArray)value, 0,length, (jbyte*)pvalue);
  } else if (dbr_type_is_SHORT(type)) {
    env->SetShortArrayRegion((jshortArray)value, 0,length, (jshort*)pvalue);
  } else if (dbr_type_is_LONG(type)) {
    env->SetIntArrayRegion((jintArray)value, 0,length, (jint*)pvalue);
  } else if (dbr_type_is_FLOAT(type)) {
    env->SetFloatArrayRegion((jfloatArray)value, 0,length, (jfloat*)pvalue);
  } else if (dbr_type_is_DOUBLE(type)) {
    env->SetDoubleArrayRegion((jdoubleArray)value, 0,length, (jdouble*)pvalue);
  } else if (dbr_type_is_STRING(type)) {
    dbr_string_t* pstrs= (dbr_string_t*)pvalue;
    jstring str;
    for (int t=0; t<length; ++t) {
      str= env->NewStringUTF(pstrs[t]);
      env->SetObjectArrayElement((jobjectArray)value,t, str);
    }
  } else if (dbr_type_is_ENUM(type)) {
    env->SetShortArrayRegion((jshortArray)value, 0, length, (jshort*)pvalue);
  }

  return value;
}



JNIEXPORT jshort JNICALL Java_gov_aps_jca_jni_JNI__1dbr_1getStatus(JNIEnv* env, jclass, jlong dbrid, jint type) {
  switch (type) {

  case DBR_STS_STRING:  return(jshort) ((dbr_sts_string*)dbrid)->status;
  case DBR_TIME_STRING: return(jshort) ((dbr_time_string*)dbrid)->status;

  case DBR_STS_ENUM:    return(jshort) ((dbr_sts_enum*)dbrid)->status;
  case DBR_TIME_ENUM:   return(jshort) ((dbr_time_enum*)dbrid)->status;
  case DBR_GR_ENUM:     return(jshort) ((dbr_gr_enum*)dbrid)->status;
  case DBR_CTRL_ENUM:   return(jshort) ((dbr_ctrl_enum*)dbrid)->status;

  case DBR_STS_CHAR:    return(jshort) ((dbr_sts_char*)dbrid)->status;
  case DBR_TIME_CHAR:   return(jshort) ((dbr_time_char*)dbrid)->status;
  case DBR_GR_CHAR:     return(jshort) ((dbr_gr_char*)dbrid)->status;
  case DBR_CTRL_CHAR:   return(jshort) ((dbr_ctrl_char*)dbrid)->status;

  case DBR_STS_SHORT:   return(jshort) ((dbr_sts_short*)dbrid)->status;
  case DBR_TIME_SHORT:  return(jshort) ((dbr_time_short*)dbrid)->status;
  case DBR_GR_SHORT:    return(jshort) ((dbr_gr_short*)dbrid)->status;
  case DBR_CTRL_SHORT:  return(jshort) ((dbr_ctrl_short*)dbrid)->status;

  case DBR_STS_LONG:    return(jshort) ((dbr_sts_long*)dbrid)->status;
  case DBR_TIME_LONG:   return(jshort) ((dbr_time_long*)dbrid)->status;
  case DBR_GR_LONG:     return(jshort) ((dbr_gr_long*)dbrid)->status;
  case DBR_CTRL_LONG:   return(jshort) ((dbr_ctrl_long*)dbrid)->status;

  case DBR_STS_FLOAT:   return(jshort) ((dbr_sts_float*)dbrid)->status;
  case DBR_TIME_FLOAT:  return(jshort) ((dbr_time_float*)dbrid)->status;
  case DBR_GR_FLOAT:    return(jshort) ((dbr_gr_float*)dbrid)->status;
  case DBR_CTRL_FLOAT:  return(jshort) ((dbr_ctrl_float*)dbrid)->status;

  case DBR_STS_DOUBLE:  return(jshort) ((dbr_sts_double*)dbrid)->status;
  case DBR_TIME_DOUBLE: return(jshort) ((dbr_time_double*)dbrid)->status;
  case DBR_GR_DOUBLE:   return(jshort) ((dbr_gr_double*)dbrid)->status;
  case DBR_CTRL_DOUBLE: return(jshort) ((dbr_ctrl_double*)dbrid)->status;
  }
  return(jshort)0;
}


JNIEXPORT jshort JNICALL Java_gov_aps_jca_jni_JNI__1dbr_1getSeverity(JNIEnv* env, jclass, jlong dbrid, jint type) {
  switch (type) {

  case DBR_STS_STRING:  return(jshort) ((dbr_sts_string*)dbrid)->severity;
  case DBR_TIME_STRING: return(jshort) ((dbr_time_string*)dbrid)->severity;

  case DBR_STS_ENUM:    return(jshort) ((dbr_sts_enum*)dbrid)->severity;
  case DBR_TIME_ENUM:   return(jshort) ((dbr_time_enum*)dbrid)->severity;
  case DBR_GR_ENUM:     return(jshort) ((dbr_gr_enum*)dbrid)->severity;
  case DBR_CTRL_ENUM:   return(jshort) ((dbr_ctrl_enum*)dbrid)->severity;

  case DBR_STS_CHAR:    return(jshort) ((dbr_sts_char*)dbrid)->severity;
  case DBR_TIME_CHAR:   return(jshort) ((dbr_time_char*)dbrid)->severity;
  case DBR_GR_CHAR:     return(jshort) ((dbr_gr_char*)dbrid)->severity;
  case DBR_CTRL_CHAR:   return(jshort) ((dbr_ctrl_char*)dbrid)->severity;

  case DBR_STS_SHORT:   return(jshort) ((dbr_sts_short*)dbrid)->severity;
  case DBR_TIME_SHORT:  return(jshort) ((dbr_time_short*)dbrid)->severity;
  case DBR_GR_SHORT:    return(jshort) ((dbr_gr_short*)dbrid)->severity;
  case DBR_CTRL_SHORT:  return(jshort) ((dbr_ctrl_short*)dbrid)->severity;

  case DBR_STS_LONG:    return(jshort) ((dbr_sts_long*)dbrid)->severity;
  case DBR_TIME_LONG:   return(jshort) ((dbr_time_long*)dbrid)->severity;
  case DBR_GR_LONG:     return(jshort) ((dbr_gr_long*)dbrid)->severity;
  case DBR_CTRL_LONG:   return(jshort) ((dbr_ctrl_long*)dbrid)->severity;

  case DBR_STS_FLOAT:   return(jshort) ((dbr_sts_float*)dbrid)->severity;
  case DBR_TIME_FLOAT:  return(jshort) ((dbr_time_float*)dbrid)->severity;
  case DBR_GR_FLOAT:    return(jshort) ((dbr_gr_float*)dbrid)->severity;
  case DBR_CTRL_FLOAT:  return(jshort) ((dbr_ctrl_float*)dbrid)->severity;

  case DBR_STS_DOUBLE:  return(jshort) ((dbr_sts_double*)dbrid)->severity;
  case DBR_TIME_DOUBLE: return(jshort) ((dbr_time_double*)dbrid)->severity;
  case DBR_GR_DOUBLE:   return(jshort) ((dbr_gr_double*)dbrid)->severity;
  case DBR_CTRL_DOUBLE: return(jshort) ((dbr_ctrl_double*)dbrid)->severity;
  }
  return(jshort)0;
}




JNIEXPORT jobject JNICALL Java_gov_aps_jca_jni_JNI__1dbr_1getUDL(JNIEnv *env, jclass, jlong dbrid, jint type) {
  switch (type) {

  case DBR_GR_CHAR:     return newByte(env, ((dbr_gr_char*)dbrid)->upper_disp_limit);
  case DBR_CTRL_CHAR:   return newByte(env, ((dbr_ctrl_char*)dbrid)->upper_disp_limit);

  case DBR_GR_SHORT:    return newShort(env, ((dbr_gr_short*)dbrid)->upper_disp_limit);
  case DBR_CTRL_SHORT:  return newShort(env, ((dbr_ctrl_short*)dbrid)->upper_disp_limit);

  case DBR_GR_LONG:     return newInt(env, ((dbr_gr_long*)dbrid)->upper_disp_limit);
  case DBR_CTRL_LONG:   return newInt(env, ((dbr_ctrl_long*)dbrid)->upper_disp_limit);

  case DBR_GR_FLOAT:    return newFloat(env, ((dbr_gr_float*)dbrid)->upper_disp_limit);
  case DBR_CTRL_FLOAT:  return newFloat(env, ((dbr_ctrl_float*)dbrid)->upper_disp_limit);

  case DBR_GR_DOUBLE:   return newDouble(env, ((dbr_gr_double*)dbrid)->upper_disp_limit);
  case DBR_CTRL_DOUBLE: return newDouble(env, ((dbr_ctrl_double*)dbrid)->upper_disp_limit);
  }
  return NULL;
}


JNIEXPORT jobject JNICALL Java_gov_aps_jca_jni_JNI__1dbr_1getLDL(JNIEnv *env, jclass, jlong dbrid, jint type) {
  switch (type) {

  case DBR_GR_CHAR:     return newByte(env, ((dbr_gr_char*)dbrid)->lower_disp_limit);
  case DBR_CTRL_CHAR:   return newByte(env, ((dbr_ctrl_char*)dbrid)->lower_disp_limit);

  case DBR_GR_SHORT:    return newShort(env, ((dbr_gr_short*)dbrid)->lower_disp_limit);
  case DBR_CTRL_SHORT:  return newShort(env, ((dbr_ctrl_short*)dbrid)->lower_disp_limit);

  case DBR_GR_LONG:     return newInt(env, ((dbr_gr_long*)dbrid)->lower_disp_limit);
  case DBR_CTRL_LONG:   return newInt(env, ((dbr_ctrl_long*)dbrid)->lower_disp_limit);

  case DBR_GR_FLOAT:
		// KE: Commented out the following.  Appears to be for debugging.
    // printf("gr_float: %E\n", (double)((dbr_gr_float*)dbrid)->lower_disp_limit);
    return newFloat(env, ((dbr_gr_float*)dbrid)->lower_disp_limit);
  case DBR_CTRL_FLOAT:  return newFloat(env, ((dbr_ctrl_float*)dbrid)->lower_disp_limit);

  case DBR_GR_DOUBLE:   return newDouble(env, ((dbr_gr_double*)dbrid)->lower_disp_limit);
  case DBR_CTRL_DOUBLE: return newDouble(env, ((dbr_ctrl_double*)dbrid)->lower_disp_limit);
  }
  return NULL;
}

JNIEXPORT jobject JNICALL Java_gov_aps_jca_jni_JNI__1dbr_1getUAL(JNIEnv *env, jclass, jlong dbrid, jint type) {
  switch (type) {

  case DBR_GR_CHAR:     return newByte(env, ((dbr_gr_char*)dbrid)->upper_alarm_limit);
  case DBR_CTRL_CHAR:   return newByte(env, ((dbr_ctrl_char*)dbrid)->upper_alarm_limit);

  case DBR_GR_SHORT:    return newShort(env, ((dbr_gr_short*)dbrid)->upper_alarm_limit);
  case DBR_CTRL_SHORT:  return newShort(env, ((dbr_ctrl_short*)dbrid)->upper_alarm_limit);

  case DBR_GR_LONG:     return newInt(env, ((dbr_gr_long*)dbrid)->upper_alarm_limit);
  case DBR_CTRL_LONG:   return newInt(env, ((dbr_ctrl_long*)dbrid)->upper_alarm_limit);

  case DBR_GR_FLOAT:    return newFloat(env, ((dbr_gr_float*)dbrid)->upper_alarm_limit);
  case DBR_CTRL_FLOAT:  return newFloat(env, ((dbr_ctrl_float*)dbrid)->upper_alarm_limit);

  case DBR_GR_DOUBLE:   return newDouble(env, ((dbr_gr_double*)dbrid)->upper_alarm_limit);
  case DBR_CTRL_DOUBLE: return newDouble(env, ((dbr_ctrl_double*)dbrid)->upper_alarm_limit);
  }
  return NULL;
}


JNIEXPORT jobject JNICALL Java_gov_aps_jca_jni_JNI__1dbr_1getUWL(JNIEnv *env, jclass, jlong dbrid, jint type) {
  switch (type) {

  case DBR_GR_CHAR:     return newByte(env, ((dbr_gr_char*)dbrid)->upper_warning_limit);
  case DBR_CTRL_CHAR:   return newByte(env, ((dbr_ctrl_char*)dbrid)->upper_warning_limit);

  case DBR_GR_SHORT:    return newShort(env, ((dbr_gr_short*)dbrid)->upper_warning_limit);
  case DBR_CTRL_SHORT:  return newShort(env, ((dbr_ctrl_short*)dbrid)->upper_warning_limit);

  case DBR_GR_LONG:     return newInt(env, ((dbr_gr_long*)dbrid)->upper_warning_limit);
  case DBR_CTRL_LONG:   return newInt(env, ((dbr_ctrl_long*)dbrid)->upper_warning_limit);

  case DBR_GR_FLOAT:    return newFloat(env, ((dbr_gr_float*)dbrid)->upper_warning_limit);
  case DBR_CTRL_FLOAT:  return newFloat(env, ((dbr_ctrl_float*)dbrid)->upper_warning_limit);

  case DBR_GR_DOUBLE:   return newDouble(env, ((dbr_gr_double*)dbrid)->upper_warning_limit);
  case DBR_CTRL_DOUBLE: return newDouble(env, ((dbr_ctrl_double*)dbrid)->upper_warning_limit);
  }
  return NULL;
}


JNIEXPORT jobject JNICALL Java_gov_aps_jca_jni_JNI__1dbr_1getLWL(JNIEnv *env, jclass, jlong dbrid, jint type) {
  switch (type) {

  case DBR_GR_CHAR:     return newByte(env, ((dbr_gr_char*)dbrid)->lower_warning_limit);
  case DBR_CTRL_CHAR:   return newByte(env, ((dbr_ctrl_char*)dbrid)->lower_warning_limit);

  case DBR_GR_SHORT:    return newShort(env, ((dbr_gr_short*)dbrid)->lower_warning_limit);
  case DBR_CTRL_SHORT:  return newShort(env, ((dbr_ctrl_short*)dbrid)->lower_warning_limit);

  case DBR_GR_LONG:     return newInt(env, ((dbr_gr_long*)dbrid)->lower_warning_limit);
  case DBR_CTRL_LONG:   return newInt(env, ((dbr_ctrl_long*)dbrid)->lower_warning_limit);

  case DBR_GR_FLOAT:    return newFloat(env, ((dbr_gr_float*)dbrid)->lower_warning_limit);
  case DBR_CTRL_FLOAT:  return newFloat(env, ((dbr_ctrl_float*)dbrid)->lower_warning_limit);

  case DBR_GR_DOUBLE:   return newDouble(env, ((dbr_gr_double*)dbrid)->lower_warning_limit);
  case DBR_CTRL_DOUBLE: return newDouble(env, ((dbr_ctrl_double*)dbrid)->lower_warning_limit);
  }
  return NULL;
}


JNIEXPORT jobject JNICALL Java_gov_aps_jca_jni_JNI__1dbr_1getLAL(JNIEnv *env, jclass, jlong dbrid, jint type) {
  switch (type) {

  case DBR_GR_CHAR:     return newByte(env, ((dbr_gr_char*)dbrid)->lower_alarm_limit);
  case DBR_CTRL_CHAR:   return newByte(env, ((dbr_ctrl_char*)dbrid)->lower_alarm_limit);

  case DBR_GR_SHORT:    return newShort(env, ((dbr_gr_short*)dbrid)->lower_alarm_limit);
  case DBR_CTRL_SHORT:  return newShort(env, ((dbr_ctrl_short*)dbrid)->lower_alarm_limit);

  case DBR_GR_LONG:     return newInt(env, ((dbr_gr_long*)dbrid)->lower_alarm_limit);
  case DBR_CTRL_LONG:   return newInt(env, ((dbr_ctrl_long*)dbrid)->lower_alarm_limit);

  case DBR_GR_FLOAT:    return newFloat(env, ((dbr_gr_float*)dbrid)->lower_alarm_limit);
  case DBR_CTRL_FLOAT:  return newFloat(env, ((dbr_ctrl_float*)dbrid)->lower_alarm_limit);

  case DBR_GR_DOUBLE:   return newDouble(env, ((dbr_gr_double*)dbrid)->lower_alarm_limit);
  case DBR_CTRL_DOUBLE: return newDouble(env, ((dbr_ctrl_double*)dbrid)->lower_alarm_limit);
  }
  return NULL;
}


JNIEXPORT jobject JNICALL Java_gov_aps_jca_jni_JNI__1dbr_1getUCL(JNIEnv *env, jclass, jlong dbrid, jint type) {
  switch (type) {

  case DBR_CTRL_CHAR:   return newByte(env, ((dbr_ctrl_char*)dbrid)->upper_ctrl_limit);

  case DBR_CTRL_SHORT:  return newShort(env, ((dbr_ctrl_short*)dbrid)->upper_ctrl_limit);

  case DBR_CTRL_LONG:   return newInt(env, ((dbr_ctrl_long*)dbrid)->upper_ctrl_limit);

  case DBR_CTRL_FLOAT:  return newFloat(env, ((dbr_ctrl_float*)dbrid)->upper_ctrl_limit);

  case DBR_CTRL_DOUBLE: return newDouble(env, ((dbr_ctrl_double*)dbrid)->upper_ctrl_limit);
  }
  return NULL;
}


JNIEXPORT jobject JNICALL Java_gov_aps_jca_jni_JNI__1dbr_1getLCL(JNIEnv *env, jclass, jlong dbrid, jint type) {
  switch (type) {

  case DBR_CTRL_CHAR:   return newByte(env, ((dbr_ctrl_char*)dbrid)->lower_ctrl_limit);

  case DBR_CTRL_SHORT:  return newShort(env, ((dbr_ctrl_short*)dbrid)->lower_ctrl_limit);

  case DBR_CTRL_LONG:   return newInt(env, ((dbr_ctrl_long*)dbrid)->lower_ctrl_limit);

  case DBR_CTRL_FLOAT:  return newFloat(env, ((dbr_ctrl_float*)dbrid)->lower_ctrl_limit);

  case DBR_CTRL_DOUBLE: return newDouble(env, ((dbr_ctrl_double*)dbrid)->lower_ctrl_limit);
  }
  return NULL;
}

JNIEXPORT jshort JNICALL Java_gov_aps_jca_jni_JNI__1dbr_1getPrecision(JNIEnv *env, jclass, jlong dbrid, jint type) {
  switch (type) {
  case DBR_GR_FLOAT:    return(jshort) ((dbr_gr_float*)dbrid)->precision;
  case DBR_CTRL_FLOAT:  return(jshort) ((dbr_ctrl_float*)dbrid)->precision;
  case DBR_GR_DOUBLE:   return(jshort) ((dbr_gr_double*)dbrid)->precision;
  case DBR_CTRL_DOUBLE: return(jshort) ((dbr_ctrl_double*)dbrid)->precision;
  }
  return(jshort)0;
}


JNIEXPORT jobject JNICALL Java_gov_aps_jca_jni_JNI__1dbr_1getTimeStamp(JNIEnv *env, jclass, jlong dbrid, jint type) {
  switch (type) {

  case DBR_TIME_STRING: return newTimeStamp(env, ((dbr_time_string*)dbrid)->stamp);

  case DBR_TIME_ENUM:   return newTimeStamp(env, ((dbr_time_enum*)dbrid)->stamp);

  case DBR_TIME_CHAR:   return newTimeStamp(env, ((dbr_time_char*)dbrid)->stamp);

  case DBR_TIME_SHORT:  return newTimeStamp(env, ((dbr_time_short*)dbrid)->stamp);

  case DBR_TIME_LONG:   return newTimeStamp(env, ((dbr_time_long*)dbrid)->stamp);

  case DBR_TIME_FLOAT:  return newTimeStamp(env, ((dbr_time_float*)dbrid)->stamp);

  case DBR_TIME_DOUBLE: return newTimeStamp(env, ((dbr_time_double*)dbrid)->stamp);

  }
  return NULL;
}


JNIEXPORT jobjectArray JNICALL Java_gov_aps_jca_jni_JNI__1dbr_1getLabels(JNIEnv *env, jclass, jlong dbrid, jint type) {
  switch (type) {
  case DBR_GR_ENUM:   return newLabels(env, ((dbr_gr_enum*)dbrid)->no_str, ((dbr_gr_enum*)dbrid)->strs);
  case DBR_CTRL_ENUM: return newLabels(env, ((dbr_ctrl_enum*)dbrid)->no_str, ((dbr_ctrl_enum*)dbrid)->strs);
  }
  return NULL;
}


JNIEXPORT jstring JNICALL Java_gov_aps_jca_jni_JNI__1dbr_1getUnits(JNIEnv *env, jclass, jlong dbrid, jint type) {
  switch (type) {

  case DBR_GR_CHAR:   return newUnits(env, ((dbr_gr_char*)dbrid)->units);
  case DBR_CTRL_CHAR: return newUnits(env, ((dbr_ctrl_char*)dbrid)->units);

  case DBR_GR_SHORT:   return newUnits(env, ((dbr_gr_short*)dbrid)->units);
  case DBR_CTRL_SHORT: return newUnits(env, ((dbr_ctrl_short*)dbrid)->units);

  case DBR_GR_LONG:    return newUnits(env, ((dbr_gr_long*)dbrid)->units);
  case DBR_CTRL_LONG:  return newUnits(env, ((dbr_ctrl_long*)dbrid)->units);

  case DBR_GR_FLOAT:   return newUnits(env, ((dbr_gr_float*)dbrid)->units);
  case DBR_CTRL_FLOAT: return newUnits(env, ((dbr_ctrl_float*)dbrid)->units);

  case DBR_GR_DOUBLE:   return newUnits(env, ((dbr_gr_double*)dbrid)->units);
  case DBR_CTRL_DOUBLE: return newUnits(env, ((dbr_ctrl_double*)dbrid)->units);

  }
  return NULL;
}

JNIEXPORT jint JNICALL Java_gov_aps_jca_jni_JNI__1dbr_1getAckT(JNIEnv *env, jclass, jlong dbrid, jint type) {
  if (type==DBR_STSACK_STRING) return((dbr_stsack_string*)dbrid)->ackt;
  return 0;
}

JNIEXPORT jint JNICALL Java_gov_aps_jca_jni_JNI__1dbr_1getAckS(JNIEnv *env, jclass, jlong dbrid, jint type) {
  if (type==DBR_STSACK_STRING) return((dbr_stsack_string*)dbrid)->acks;
  return 0;
}

/* **************************** Emacs Editing Sequences ***************** */
/* Local Variables: */
/* tab-width: 2 */
/* c-basic-offset: 2 */
/* c-comment-only-line-offset: 0 */
/* End: */
