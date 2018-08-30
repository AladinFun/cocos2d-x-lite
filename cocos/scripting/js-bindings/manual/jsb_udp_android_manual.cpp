#include "jsb_udp_android_manual.hpp"
#include "cocos/platform/android/jni/JniHelper.h"
#include "cocos/scripting/js-bindings/manual/jsb_conversions.hpp"

using namespace cocos2d;

std::function<void(uint8_t*, size_t)> on_receive_message;

bool jsb_udp_initialize(se::State& state) {
    const auto& args = state.args();
    int argc = (int)args.size();
    bool isOK = false;
    if(argc == 1) {
        se::Value func = args[0];
        assert(func.isObject() && func.toObject()->isFunction());
        func.toObject()->root();

        on_receive_message = [func](uint8_t* data, size_t byteLength){
            se::ScriptEngine::getInstance()->clearException();
            se::AutoHandleScope hs;

            se::ValueArray args;
            se::Object* dataArr = se::Object::createTypedArray(se::Object::TypedArrayType::UINT8 , data,  byteLength);
            args.insert(args.begin(), se::Value(dataArr));
            return func.toObject()->call(args, nullptr);
        };

        JniMethodInfo methodInfo;
        if(JniHelper::getStaticMethodInfo(methodInfo,
        "org/cocos2dx/javascript/UDPSocket",
        "initialize",
        "()V"))
        {
            methodInfo.env->CallStaticVoidMethod(methodInfo.classID, methodInfo.methodID);
            methodInfo.env->DeleteLocalRef(methodInfo.classID);
        }
    }
    if(!isOK) {
        state.rval().setUndefined();
    }
    return true;
}
SE_BIND_FUNC(jsb_udp_initialize)

bool jsb_udp_send(se::State& state) {
    const auto& args = state.args();
    int argc = (int)args.size();
    bool isOK = false;
    if(argc == 3) {
        size_t srcDataLen = 0;
        uint8_t* srcDataBuf = nullptr;
        while(true) {
            std::string host;
            isOK = seval_to_std_string(args[0], &host);
            SE_PRECONDITION2(isOK, false, "jsb_udp_send : invalid host");
            if(!isOK) break;

            int port;
            isOK = seval_to_int32(args[1], &port);
            SE_PRECONDITION2(isOK, false, "jsb_udp_send : invalid port");
            if(!isOK) break;

            if(se::Value::Type::Object == args[2].getType()) {
                se::Object* uint8ArrObj = args[2].toObject();
                if (uint8ArrObj->isTypedArray()) {
                    isOK = uint8ArrObj->getTypedArrayData(&srcDataBuf, &srcDataLen);
                    SE_PRECONDITION2(isOK, false, "jsb_udp_send : getTypedArrayData failed!");
                    if(!isOK) break;
                }
                else if (uint8ArrObj->isArrayBuffer())
                {
                    isOK = uint8ArrObj->getArrayBufferData(&srcDataBuf, &srcDataLen);
                    SE_PRECONDITION2(isOK, false, "jsb_udp_send : getArrayBufferData failed!");
                    if(!isOK) break;
                }
                else {
                    assert(false);
                }
            }
            isOK = (srcDataLen > 0 && srcDataBuf != nullptr);
            SE_PRECONDITION2(isOK, false, "jsb_udp_send : invalid data");
            if(!isOK) break;

            break;
        }
        if(isOK) {
            JniMethodInfo methodInfo;
            if(JniHelper::getStaticMethodInfo(methodInfo,
            "org/cocos2dx/javascript/UDPSocket",
            "sendMessage",
            "([B)V"))
            {
                jbyte *buf = (jbyte*)(srcDataBuf);
                jbyteArray jbyteArr;
                jbyteArr = methodInfo.env->NewByteArray(srcDataLen);
                methodInfo.env->SetByteArrayRegion(jbyteArr, 0, srcDataLen, buf);
                methodInfo.env->CallStaticVoidMethod(methodInfo.classID, methodInfo.methodID,jbyteArr);
                methodInfo.env->DeleteLocalRef(methodInfo.classID);
            }
        }
    }
    if(!isOK) {
        state.rval().setUndefined();
    }
    return true;
}
SE_BIND_FUNC(jsb_udp_send)

extern "C"
{
    /*
     * Class:     org_cocos2dx_javascript_UDPSocket
     * Method:    nativeReceiveMessage
     * Signature: ([Ljava/lang/String;)V
     */
    JNIEXPORT void JNICALL Java_org_cocos2dx_javascript_UDPSocket_nativeReceiveMessage(JNIEnv *env, jclass clazz, jbyteArray data, jint length) {
        unsigned char *byteData = nullptr;
        size_t byteDataLen = 0;
        if (data != NULL) {
            byteData = (unsigned char *) env->GetByteArrayElements(data, 0);
            byteDataLen = (size_t)env->GetArrayLength(data);
        } else {
            __android_log_print(ANDROID_LOG_WARN, "NativeBride", "encode data can not be null");
            return;
        }
        on_receive_message(byteData,length);
    }
}


// static bool register_util_func(se::Object* global) {
//     global->defineFunction("udpInitialize",_SE(jsb_udp_initialize));
//     global->defineFunction("udpSend",_SE(jsb_udp_send));
//     se::ScriptEngine::getInstance()->clearException();
//     return true;
// }

bool register_all_udp_android_manual(se::Object* obj) {
    //register_util_func(obj);
    obj->defineFunction("udpInitialize",_SE(jsb_udp_initialize));
    obj->defineFunction("udpSend",_SE(jsb_udp_send));
    se::ScriptEngine::getInstance()->clearException();
    return true;
}