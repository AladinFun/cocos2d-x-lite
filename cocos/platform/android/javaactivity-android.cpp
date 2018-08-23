/****************************************************************************
Copyright (c) 2013-2017 Chukong Technologies Inc.

http://www.cocos2d-x.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
****************************************************************************/

#include "platform/CCPlatformConfig.h"
#if CC_TARGET_PLATFORM == CC_PLATFORM_ANDROID

#include "platform/android/CCApplication-android.h"
#include "platform/android/CCGLViewImpl-android.h"
#include "base/CCDirector.h"
#include "base/CCEventCustom.h"
#include "base/CCEventType.h"
#include "base/CCEventDispatcher.h"
#include "renderer/CCGLProgramCache.h"
#include "renderer/CCTextureCache.h"
#include "renderer/ccGLStateCache.h"
#include "2d/CCDrawingPrimitives.h"
#include "platform/android/jni/JniHelper.h"
#include "network/CCDownloader-android.h"
#include "network/HttpClient.h"
#include <android/log.h>
#include <android/api-level.h>
#include <jni.h>
#include <CCThread.h>
#include <scripting/js-bindings/manual/jsb_global.h>
#include <scripting/js-bindings/manual/ScriptingCore.h>

#define  LOG_TAG    "main"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

void cocos_android_app_init(JNIEnv* env) __attribute__((weak));

void cocos_audioengine_focus_change(int focusChange);

using namespace cocos2d;

extern "C"
{

// ndk break compatibility, refer to https://github.com/cocos2d/cocos2d-x/issues/16267 for detail information
// should remove it when using NDK r13 since NDK r13 will add back bsd_signal()
#if __ANDROID_API__ > 19
#include <signal.h>
#include <dlfcn.h>
    typedef __sighandler_t (*bsd_signal_func_t)(int, __sighandler_t);
    bsd_signal_func_t bsd_signal_func = NULL;

    __sighandler_t bsd_signal(int s, __sighandler_t f) {
        if (bsd_signal_func == NULL) {
            // For now (up to Android 7.0) this is always available
            bsd_signal_func = (bsd_signal_func_t) dlsym(RTLD_DEFAULT, "bsd_signal");

            if (bsd_signal_func == NULL) {
                __android_log_assert("", "bsd_signal_wrapper", "bsd_signal symbol not found!");
            }
        }
        return bsd_signal_func(s, f);
    }
#endif // __ANDROID_API__ > 19

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JniHelper::setJavaVM(vm);
    CCLOG("xxxxxxx native.JNI_Onload");
    cocos_android_app_init(JniHelper::getEnv());

    return JNI_VERSION_1_4;
}

JNIEXPORT void Java_org_cocos2dx_lib_Cocos2dxRenderer_nativeInit(JNIEnv*  env, jobject thiz, jint w, jint h)
{
    CCLOG("xxxxxxx native.Java_org_cocos2dx_lib_Cocos2dxRenderer_nativeInit");
    if(!cocos2d::Application::getInstance()) {
        cocos_android_app_init(env);
    }

    JniMethodInfo methodInfo;
    if (JniHelper::getStaticMethodInfo(methodInfo,
                                       "com/aladinfun/koalas/RuntimeModule",
                                       "onRenderReady",
                                       "()V"))
    {
        methodInfo.env->CallStaticVoidMethod(methodInfo.classID, methodInfo.methodID);
        methodInfo.env->DeleteLocalRef(methodInfo.classID);
    }

    auto director = cocos2d::Director::getInstance();
    auto glview = director->getOpenGLView();
    if (!glview)
    {
        glview = cocos2d::GLViewImpl::create("CocosRuntime");
        glview->setFrameSize(w, h);
        director->setOpenGLView(glview);

        cocos2d::Application::getInstance()->run();
    }
    else
    {
        cocos2d::GL::invalidateStateCache();
        cocos2d::GLProgramCache::getInstance()->reloadDefaultGLPrograms();
        cocos2d::DrawPrimitives::init();
        cocos2d::VolatileTextureMgr::reloadAllTextures();

        cocos2d::EventCustom* recreatedEvent = new (std::nothrow) cocos2d::EventCustom(EVENT_RENDERER_RECREATED);
        director->getEventDispatcher()->dispatchEvent(recreatedEvent);
        recreatedEvent->release();

        director->setGLDefaultValues();
    }
    cocos2d::network::_preloadJavaDownloaderClass();
}

JNIEXPORT void Java_org_cocos2dx_lib_Cocos2dxRenderer_nativeOnSurfaceDestroy(JNIEnv*  env, jobject thiz)
{
    CCLOG("xxxxxxx native.Java_org_cocos2dx_lib_Cocos2dxRenderer_nativeOnSurfaceDestroy");
#if SCRIPT_ENGINE_TYPE == SCRIPT_ENGINE_V8
    jsb_on_surface_destroy();
#endif
#if CC_TARGET_PLATFORM == CC_PLATFORM_ANDROID
    JniMethodInfo methodInfo;
    if (JniHelper::getStaticMethodInfo(methodInfo,
                                       "com/aladinfun/koalas/RuntimeModule",
                                       "onRenderDestroy",
                                       "()V"))
    {
        methodInfo.env->CallStaticVoidMethod(methodInfo.classID, methodInfo.methodID);
        methodInfo.env->DeleteLocalRef(methodInfo.classID);
    }
#endif
}

JNIEXPORT jintArray Java_org_cocos2dx_lib_Cocos2dxActivity_getGLContextAttrs(JNIEnv*  env, jobject thiz)
{
    if(!cocos2d::Application::getInstance()) {
        cocos_android_app_init(env);
    }
    cocos2d::Application::getInstance()->initGLContextAttrs();
    GLContextAttrs _glContextAttrs = GLView::getGLContextAttrs();

    int tmp[6] = {_glContextAttrs.redBits, _glContextAttrs.greenBits, _glContextAttrs.blueBits,
                           _glContextAttrs.alphaBits, _glContextAttrs.depthBits, _glContextAttrs.stencilBits};


    jintArray glContextAttrsJava = env->NewIntArray(6);
        env->SetIntArrayRegion(glContextAttrsJava, 0, 6, tmp);

    return glContextAttrsJava;
}

//already called from gl thread
JNIEXPORT jintArray Java_org_cocos2dx_lib_Cocos2dxActivity_startRuntime(JNIEnv*  env, jobject thiz, jstring data)
{
    CCLOG("xxxxxxx native.start runtime");
    cocos2d::Application::isRunning = true;
    std::string dataStr = JniHelper::jstring2string(data);
    std::string startScript = "window.startGame(" + dataStr + ");";

    CCLOG("xxxxxxx native.run script: main.js");
    jsb_run_script("main.js");
    CCLOG("xxxxxxx native.run script: %s", startScript.c_str());
    jsb_run_script_string(startScript.c_str());
}

//already called from gl thread
JNIEXPORT jintArray Java_org_cocos2dx_lib_Cocos2dxActivity_restartRuntime(JNIEnv*  env, jobject thiz) {
    CCLOG("xxxxxxx Java_org_cocos2dx_lib_Cocos2dxActivity_restartRuntime");
    cocos2d::Director::getInstance()->restart();
    cocos2d::Director::getInstance()->mainLoop();
    cocos2d::Application::isRunning = false;
}

JNIEXPORT jintArray Java_org_cocos2dx_lib_Cocos2dxActivity_stopRuntime(JNIEnv*  env, jobject thiz)
{
    CCLOG("xxxxxxx Java_org_cocos2dx_lib_Cocos2dxActivity_stopRuntime");
    cocos2d::Director::getInstance()->getScheduler()->performFunctionInCocosThread([](){
        cocos2d::Application::getInstance()->isRunning = false;
        cocos2d::Director::getInstance()->restart();
        cocos2d::Director::getInstance()->mainLoop();
    });
}


JNIEXPORT void Java_org_cocos2dx_lib_Cocos2dxAudioFocusManager_nativeOnAudioFocusChange(JNIEnv* env, jobject thiz, jint focusChange)
{
    cocos_audioengine_focus_change(focusChange);
}

JNIEXPORT void Java_org_cocos2dx_lib_Cocos2dxRenderer_nativeOnSurfaceChanged(JNIEnv*  env, jobject thiz, jint w, jint h)
{
    cocos2d::Application::getInstance()->applicationScreenSizeChanged(w, h);
}

}

#endif // CC_TARGET_PLATFORM == CC_PLATFORM_ANDROID

