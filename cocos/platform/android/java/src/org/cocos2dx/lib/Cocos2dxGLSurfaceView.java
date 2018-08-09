/****************************************************************************
Copyright (c) 2010-2013 cocos2d-x.org
Copyright (c) 2013-2016 Chukong Technologies Inc.

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
package org.cocos2dx.lib;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Cocos2dxGLSurfaceView extends GLSurfaceView {
    // ===========================================================
    // Constants
    // ===========================================================

    private static final String TAG = Cocos2dxGLSurfaceView.class.getSimpleName();

    private final static int HANDLER_OPEN_IME_KEYBOARD = 2;
    private final static int HANDLER_CLOSE_IME_KEYBOARD = 3;

    // ===========================================================
    // Fields
    // ===========================================================

    // TODO Static handler -> Potential leak!
    private static Handler sHandler;

    private static Cocos2dxGLSurfaceView mCocos2dxGLSurfaceView;
    private static Cocos2dxTextInputWrapper sCocos2dxTextInputwrapper;

    public Cocos2dxRenderer mCocos2dxRenderer;
    private Cocos2dxEditBox mCocos2dxEditText;

    public boolean isSoftKeyboardShown() {
        return mSoftKeyboardShown;
    }

    public void setSoftKeyboardShown(boolean softKeyboardShown) {
        this.mSoftKeyboardShown = softKeyboardShown;
    }

    private boolean mWaiteBuild = false;
    private boolean mSoftKeyboardShown = false;
    private WeakReference<ViewGroup> mContainer = null;
    private WeakReference<Handler> mRebuildHandle = null;


    // ===========================================================
    // Constructors
    // ===========================================================

    public Cocos2dxGLSurfaceView(final Context context, ViewGroup container) {
        super(context);
        this.mContainer = new WeakReference<>(container);
        this.initView();
    }

    public ViewGroup getContainer() {
        return this.mContainer != null ? this.mContainer.get() : null;
    }

    public void setRebuildHandle(Handler delay) {
        mRebuildHandle = new WeakReference<>(delay);
    }

    public Cocos2dxGLSurfaceView(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        this.initView();
    }

    protected void initView() {
        this.setEGLContextClientVersion(2);
        this.setFocusableInTouchMode(true);

        Cocos2dxGLSurfaceView.mCocos2dxGLSurfaceView = this;
        Cocos2dxGLSurfaceView.sCocos2dxTextInputwrapper = new Cocos2dxTextInputWrapper(this);

        Cocos2dxGLSurfaceView.sHandler = new Handler() {
            @Override
            public void handleMessage(final Message msg) {
                switch (msg.what) {
                    case HANDLER_OPEN_IME_KEYBOARD:
                        if (null != Cocos2dxGLSurfaceView.this.mCocos2dxEditText && Cocos2dxGLSurfaceView.this.mCocos2dxEditText.requestFocus()) {
                            Cocos2dxGLSurfaceView.this.mCocos2dxEditText.removeTextChangedListener(Cocos2dxGLSurfaceView.sCocos2dxTextInputwrapper);
                            Cocos2dxGLSurfaceView.this.mCocos2dxEditText.setText("");
                            final String text = (String) msg.obj;
                            Cocos2dxGLSurfaceView.this.mCocos2dxEditText.append(text);
                            Cocos2dxGLSurfaceView.sCocos2dxTextInputwrapper.setOriginText(text);
                            Cocos2dxGLSurfaceView.this.mCocos2dxEditText.addTextChangedListener(Cocos2dxGLSurfaceView.sCocos2dxTextInputwrapper);
                            final InputMethodManager imm = (InputMethodManager) Cocos2dxGLSurfaceView.mCocos2dxGLSurfaceView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.showSoftInput(Cocos2dxGLSurfaceView.this.mCocos2dxEditText, 0);
                            Log.d("GLSurfaceView", "showSoftInput");
                        }
                        break;

                    case HANDLER_CLOSE_IME_KEYBOARD:
                        if (null != Cocos2dxGLSurfaceView.this.mCocos2dxEditText) {
                            Cocos2dxGLSurfaceView.this.mCocos2dxEditText.removeTextChangedListener(Cocos2dxGLSurfaceView.sCocos2dxTextInputwrapper);
                            final InputMethodManager imm = (InputMethodManager) Cocos2dxGLSurfaceView.mCocos2dxGLSurfaceView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(Cocos2dxGLSurfaceView.this.mCocos2dxEditText.getWindowToken(), 0);
                            Cocos2dxGLSurfaceView.this.requestFocus();
                            // can take effect after GLSurfaceView has focus
                            ((Cocos2dxActivity)Cocos2dxGLSurfaceView.mCocos2dxGLSurfaceView.getContext()).hideVirtualButton();
                            Log.d("GLSurfaceView", "HideSoftInput");
                        }
                        break;
                }
            }
        };
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================


       public static Cocos2dxGLSurfaceView getInstance() {
       return mCocos2dxGLSurfaceView;
       }

       public static void queueAccelerometer(final float x, final float y, final float z, final long timestamp) {
       mCocos2dxGLSurfaceView.queueEvent(new Runnable() {
        @Override
            public void run() {
                Cocos2dxAccelerometer.onSensorChanged(x, y, z, timestamp);
        }
        });
    }

    public void setCocos2dxRenderer(final Cocos2dxRenderer renderer) {
        this.mCocos2dxRenderer = renderer;
        this.setRenderer(this.mCocos2dxRenderer);
    }

    public Cocos2dxRenderer getCocos2dxRenderer() {
         return this.mCocos2dxRenderer;
    }

    private String getContentText() {
        return this.mCocos2dxRenderer.getContentText();
    }

    public Cocos2dxEditBox getCocos2dxEditText() {
        return this.mCocos2dxEditText;
    }

    public void setCocos2dxEditText(final Cocos2dxEditBox pCocos2dxEditText) {
        this.mCocos2dxEditText = pCocos2dxEditText;
        if (null != this.mCocos2dxEditText && null != Cocos2dxGLSurfaceView.sCocos2dxTextInputwrapper) {
            this.mCocos2dxEditText.setOnEditorActionListener(Cocos2dxGLSurfaceView.sCocos2dxTextInputwrapper);
            this.requestFocus();
        }
    }

    public void onRebuild() {
        getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mWaiteBuild || Cocos2dxGLSurfaceView.this.mCocos2dxRenderer.mNativeInitCompleted)
                    return;

                mWaiteBuild = false;
                Cocos2dxGLSurfaceView.this.setVisibility(View.GONE);
                Cocos2dxGLSurfaceView.this.mContainer.get().removeView(Cocos2dxGLSurfaceView.mCocos2dxGLSurfaceView);

                mRebuildHandle.get().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Cocos2dxGLSurfaceView.this.setFocusableInTouchMode(true);
                        Cocos2dxGLSurfaceView.this.setZOrderMediaOverlay(true);
                        Cocos2dxGLSurfaceView.this.setVisibility(View.VISIBLE);
                        Cocos2dxGLSurfaceView.this.mContainer.get().addView(Cocos2dxGLSurfaceView.mCocos2dxGLSurfaceView);
                    }
                }, 200);
            }
        }, 2000);
    }

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
        Log.e(TAG, "xxxxxxx onMeasure" + widthMeasureSpec + ":" + heightMeasureSpec);

    }

    @Override
    public void onResume() {
        Log.d(TAG, "xxxxxxx onResume");
        super.onResume();
        this.setRenderMode(RENDERMODE_CONTINUOUSLY);
        this.queueEvent(new Runnable() {
            @Override
            public void run() {
                Cocos2dxGLSurfaceView.this.mCocos2dxRenderer.handleOnResume();
            }
        });

    }

    @Override
    public void onPause() {
        Log.d(TAG, "xxxxxxx onPause");
        this.queueEvent(new Runnable() {
            @Override
            public void run() {
                Cocos2dxGLSurfaceView.this.mCocos2dxRenderer.handleOnPause();
            }
        });
        this.setRenderMode(RENDERMODE_WHEN_DIRTY);
//        super.onPause();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(!this.isEnabled()) {
            return false;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        mWaiteBuild = true;
        Log.d(TAG, "xxxxxxx surfaceCreated super start");
        super.surfaceCreated(holder);
        Log.d(TAG, "xxxxxxx surfaceCreated super end");
        //this.onRebuild();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mWaiteBuild = false;
        // Surface will be destroyed when we return
        Log.d(TAG, "xxxxxxx surfaceDestroyed");
        Cocos2dxRenderer.nativeOnSurfaceDestroy();
        Cocos2dxGLSurfaceView.this.mCocos2dxRenderer.mNativeInitCompleted = false;
        super.surfaceDestroyed(holder);
    }

    private List<Runnable> queuedRunnable = Collections.synchronizedList(new ArrayList<Runnable>());

    @Override
    public void queueEvent(final Runnable r) {
        queuedRunnable.add(r);
        super.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (!(Cocos2dxGLSurfaceView.this.mCocos2dxRenderer.mNativeInitCompleted))
                        return;

                if(queuedRunnable.indexOf(r) != -1) {
                    queuedRunnable.remove(r);
                    r.run();
                }
            }
        });
    }

    public void runAllQueuedEvents() {
        while(queuedRunnable.size() > 0) {
            queuedRunnable.remove(0).run();
        }
    }

    @Override
    public boolean onTouchEvent(final MotionEvent pMotionEvent) {
        Log.d(TAG, "onTouchEvent " + this.isEnabled() + " " + (pMotionEvent.getAction() & MotionEvent.ACTION_MASK));
        if(!this.isEnabled()) {
            return false;
        }
        // these data are used in ACTION_MOVE and ACTION_CANCEL
        final int pointerNumber = pMotionEvent.getPointerCount();
        final int[] ids = new int[pointerNumber];
        final float[] xs = new float[pointerNumber];
        final float[] ys = new float[pointerNumber];

        if (mSoftKeyboardShown){
            InputMethodManager imm = (InputMethodManager)this.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            View view = ((Activity)this.getContext()).getCurrentFocus();
            imm.hideSoftInputFromWindow(view.getWindowToken(),0);
            this.requestFocus();
            mSoftKeyboardShown = false;
        }

        for (int i = 0; i < pointerNumber; i++) {
            ids[i] = pMotionEvent.getPointerId(i);
            xs[i] = pMotionEvent.getX(i);
            ys[i] = pMotionEvent.getY(i);
        }

        switch (pMotionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                final int indexPointerDown = pMotionEvent.getAction() >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int idPointerDown = pMotionEvent.getPointerId(indexPointerDown);
                final float xPointerDown = pMotionEvent.getX(indexPointerDown);
                final float yPointerDown = pMotionEvent.getY(indexPointerDown);

                this.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        Cocos2dxGLSurfaceView.this.mCocos2dxRenderer.handleActionDown(idPointerDown, xPointerDown, yPointerDown);
                    }
                });
                break;

            case MotionEvent.ACTION_DOWN:
                // there are only one finger on the screen
                final int idDown = pMotionEvent.getPointerId(0);
                final float xDown = xs[0];
                final float yDown = ys[0];

                this.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        Cocos2dxGLSurfaceView.this.mCocos2dxRenderer.handleActionDown(idDown, xDown, yDown);
                    }
                });
                break;

            case MotionEvent.ACTION_MOVE:
                this.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        Cocos2dxGLSurfaceView.this.mCocos2dxRenderer.handleActionMove(ids, xs, ys);
                    }
                });
                break;

            case MotionEvent.ACTION_POINTER_UP:
                final int indexPointUp = pMotionEvent.getAction() >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int idPointerUp = pMotionEvent.getPointerId(indexPointUp);
                final float xPointerUp = pMotionEvent.getX(indexPointUp);
                final float yPointerUp = pMotionEvent.getY(indexPointUp);

                this.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        Cocos2dxGLSurfaceView.this.mCocos2dxRenderer.handleActionUp(idPointerUp, xPointerUp, yPointerUp);
                    }
                });
                break;

            case MotionEvent.ACTION_UP:
                // there are only one finger on the screen
                final int idUp = pMotionEvent.getPointerId(0);
                final float xUp = xs[0];
                final float yUp = ys[0];

                this.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        Cocos2dxGLSurfaceView.this.mCocos2dxRenderer.handleActionUp(idUp, xUp, yUp);
                    }
                });
                break;

            case MotionEvent.ACTION_CANCEL:
                this.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        Cocos2dxGLSurfaceView.this.mCocos2dxRenderer.handleActionCancel(ids, xs, ys);
                    }
                });
                break;
        }

        /*
        if (BuildConfig.DEBUG) {
            Cocos2dxGLSurfaceView.dumpMotionEvent(pMotionEvent);
        }
        */
        return true;
    }

    /*
     * This function is called before Cocos2dxRenderer.nativeInit(), so the
     * width and height is correct.
     */
    @Override
    protected void onSizeChanged(final int pNewSurfaceWidth, final int pNewSurfaceHeight, final int pOldSurfaceWidth, final int pOldSurfaceHeight) {
        if(!this.isInEditMode()) {
            Log.e(TAG, "onSizeChanged: " + pNewSurfaceWidth + " : " + pNewSurfaceHeight);
            this.mCocos2dxRenderer.setScreenWidthAndHeight(pNewSurfaceWidth, pNewSurfaceHeight);
        }
    }

    @Override
    public boolean onKeyDown(final int pKeyCode, final KeyEvent pKeyEvent) {
        if(!this.isEnabled()) {
            return false;
        }
        switch (pKeyCode) {
            case KeyEvent.KEYCODE_BACK:
                Cocos2dxVideoHelper.mVideoHandler.sendEmptyMessage(Cocos2dxVideoHelper.KeyEventBack);
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                this.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        Cocos2dxGLSurfaceView.this.mCocos2dxRenderer.handleKeyDown(pKeyCode);
                    }
                });
                return true;
            default:
                return super.onKeyDown(pKeyCode, pKeyEvent);
        }
    }

    @Override
    public boolean onKeyUp(final int keyCode, KeyEvent event) {
        if(!this.isEnabled()) {
            return false;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                this.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        Cocos2dxGLSurfaceView.this.mCocos2dxRenderer.handleKeyUp(keyCode);
                    }
                });
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    // ===========================================================
    // Methods
    // ===========================================================

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    public static void openIMEKeyboard() {
        final Message msg = new Message();
        msg.what = Cocos2dxGLSurfaceView.HANDLER_OPEN_IME_KEYBOARD;
        msg.obj = Cocos2dxGLSurfaceView.mCocos2dxGLSurfaceView.getContentText();
        Cocos2dxGLSurfaceView.sHandler.sendMessage(msg);
    }

    public static void closeIMEKeyboard() {
        final Message msg = new Message();
        msg.what = Cocos2dxGLSurfaceView.HANDLER_CLOSE_IME_KEYBOARD;
        Cocos2dxGLSurfaceView.sHandler.sendMessage(msg);
    }

    public void insertText(final String pText) {
        this.queueEvent(new Runnable() {
            @Override
            public void run() {
                Cocos2dxGLSurfaceView.this.mCocos2dxRenderer.handleInsertText(pText);
            }
        });
    }

    public void deleteBackward() {
        this.queueEvent(new Runnable() {
            @Override
            public void run() {
                Cocos2dxGLSurfaceView.this.mCocos2dxRenderer.handleDeleteBackward();
            }
        });
    }

    private static void dumpMotionEvent(final MotionEvent event) {
        final String names[] = { "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE", "POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?" };
        final StringBuilder sb = new StringBuilder();
        final int action = event.getAction();
        final int actionCode = action & MotionEvent.ACTION_MASK;
        sb.append("event ACTION_").append(names[actionCode]);
        if (actionCode == MotionEvent.ACTION_POINTER_DOWN || actionCode == MotionEvent.ACTION_POINTER_UP) {
            sb.append("(pid ").append(action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            sb.append(")");
        }
        sb.append("[");
        for (int i = 0; i < event.getPointerCount(); i++) {
            sb.append("#").append(i);
            sb.append("(pid ").append(event.getPointerId(i));
            sb.append(")=").append((int) event.getX(i));
            sb.append(",").append((int) event.getY(i));
            if (i + 1 < event.getPointerCount()) {
                sb.append(";");
            }
        }
        sb.append("]");
        Log.d(Cocos2dxGLSurfaceView.TAG, sb.toString());
    }
}
