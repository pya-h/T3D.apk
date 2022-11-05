package com.thcplusplus.t3d;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class CustomGLSurfaceView extends GLSurfaceView {
    private final SceneRenderer mRenderer;

    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float mPreviousX;
    private float mPreviousY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        if (event != null) {
            float x = event.getX();
            float y = event.getY();

            if (event.getAction() == MotionEvent.ACTION_MOVE) {

                if (mRenderer != null) {
                    float deltaX = (x - mPreviousX) / 2f;
                    float deltaY = (y - mPreviousY) / 2f;

                    mRenderer.mDeltaX += deltaX;
                    mRenderer.mDeltaY += deltaY;
                    mRenderer.mTotalDeltaX += mRenderer.mDeltaX;
                    mRenderer.mTotalDeltaY += mRenderer.mDeltaY;
                    mRenderer.mTotalDeltaX = ( mRenderer.mTotalDeltaX + 360 ) % 360;
                    mRenderer.mTotalDeltaY = ( mRenderer.mTotalDeltaY + 360 ) % 360;

                }
                requestRender();
            }
            /*if(event.getAction() == MotionEvent.ACTION_UP){
                if(mRenderer.mTotalDeltaX > 0 && mRenderer.mTotalDeltaX < 90) {
                    long time = SystemClock.uptimeMillis() % 10000L;
                    float angleInDegrees = (360.0f / 10000.0f) * ((int) time);
                    while(mRenderer.mTotalDeltaX < 90)
                    {
                        mRenderer.mDeltaX = angleInDegrees;
                        requestRender();
                        mRenderer.mTotalDeltaX += mRenderer.mDeltaX;
                        mRenderer.mTotalDeltaY += mRenderer.mDeltaY;
                        mRenderer.mTotalDeltaX = ( mRenderer.mTotalDeltaX + 360 ) % 360;
                        mRenderer.mTotalDeltaY = ( mRenderer.mTotalDeltaY + 360 ) % 360;
                    }
                }
            }*/

            if (event.getAction() == MotionEvent.ACTION_DOWN) {


                    View view = getRootView();

                    mRenderer.mNormalX = (event.getX() / (float) view.getWidth())* 2 - 1;
                    mRenderer.mNormalY = -( (event.getY() / (float) view.getHeight()) * 2 - 1 );
                    Log.e("DEBUG", "normalX = " + Float.toString(mRenderer.mNormalX) +
                                "\tnormalY = " + Float.toString(mRenderer.mNormalY) );
                    mRenderer.mClicked = true;

            }


            mPreviousX = x;
            mPreviousY = y;

            return true;
        } else {
            return super.onTouchEvent(event);
        }
    }


    public CustomGLSurfaceView(Context context) {
        super(context);
        /*CustomGLSurfaceView v = (CustomGLSurfaceView) getRootView();
        v.setBackgroundResource(R.drawable.bumpy_bricks_public_domain);
        v.setZOrderMediaOverlay(true);*/
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);


        View view = getRootView();

        mRenderer = new SceneRenderer(context);
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);

        // Render the view only when there is a change in the drawing data.
        // To allow the Square to rotate automatically, this line is commented out:
        //setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }


}