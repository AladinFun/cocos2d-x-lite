package org.cocos2dx.lib;

import android.app.Activity;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class Cocos2dxContext {

    private static final String TAG = Cocos2dxContext.class.getSimpleName();

    public static interface ICocos2dxContext extends Cocos2dxHelper.Cocos2dxHelperListener {
        Activity getActivity();
        void runOnUiThread(Runnable run);

        Cocos2dxGLSurfaceView getGLSurfaceView();
        void hideVirtualButton();
        void setKeepScreenOn(boolean isOn);
        void runOnResumed(Runnable run);
    }

    public static ICocos2dxContext delegator = null;

    public static Activity getActivity() {
        if(delegator != null) {
            return delegator.getActivity();
        }
        return null;
    }

    public static void runOnUiThread(Runnable run) {
        if(delegator != null) {
            delegator.runOnUiThread(run);
        }
    }

    public static void runOnGLThread(Runnable run){
        if(delegator != null) {
            delegator.runOnGLThread(run);
        }
    }

    public static Cocos2dxGLSurfaceView getGLSurfaceView() {
        if(delegator != null) {
            return delegator.getGLSurfaceView();
        }
        return null;
    }

    public static void showDialog(final String pTitle, final String pMessage) {
        if(delegator != null) {
            delegator.showDialog(pTitle, pMessage);
        }
    }

    public static void hideVirtualButton() {
        if(delegator != null) {
            delegator.hideVirtualButton();
        }
    }

    public static void setKeepScreenOn(boolean isOn) {
        if(delegator != null) {
            delegator.setKeepScreenOn(isOn);
        }
    }

    public static void runOnResumed(Runnable run) {
        if(delegator != null) {
            delegator.runOnResumed(run);
        }
    }

    public static void dispatchEvent(String eventName, Map<String, Object> params) {
        JSONObject json = new JSONObject();
        if(params != null && !params.isEmpty()) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                try {
                    json.putOpt(entry.getKey(), entry.getValue());
                } catch (JSONException e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("cc.eventManager.dispatchCustomEvent(\"")
                    .append(eventName)
                    .append("\", JSON.parse(decodeURIComponent(\"")
                    .append(URLEncoder.encode(json.toString(), "utf-8"))
                    .append("\")));");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        final String script = sb.toString();
        Log.d(TAG, "evalString -> " + script);
        runOnResumed(new Runnable() {
            @Override
            public void run() {
                runOnGLThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "execute eval string -> " + script);
                        Cocos2dxJavascriptJavaBridge.evalString(script);
                    }
                });
            }
        });
    }

}
