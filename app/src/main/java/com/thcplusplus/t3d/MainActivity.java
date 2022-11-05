package com.thcplusplus.t3d;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

// app compat activity*********************
public class MainActivity extends Activity
{
    /** Hold a reference to our GLSurfaceView */
    private CustomGLSurfaceView mGLSurfaceView;
    public static Button gameStatusButton;

    //private Player mPlayer;
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // UI initialize:
        super.onCreate(savedInstanceState);
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mGLSurfaceView = new CustomGLSurfaceView(this);
        setContentView(mGLSurfaceView);
        gameStatusButton = new Button(this);
        gameStatusButton.setText("Game Started");
        gameStatusButton.setEnabled(false);
        gameStatusButton.setTextSize(25.0f);
        //setMarginLeft(gameStatusButton, 100);
        this.addContentView(gameStatusButton, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        /* // Check if the system supports OpenGL ES 2.0.
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();

         final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;
        if (supportsEs2)
        {
            // Request an OpenGL ES 2.0 compatible context.
            mGLSurfaceView.setEGLContextClientVersion(2);
            // Set the renderer to our demo renderer, defined below.
            mGLSurfaceView.setRenderer(new SceneRenderer(this));
        }
        else
        {
            // This is where you could create an OpenGL ES 1.x compatible renderer if you wanted to support both ES 1 and ES 2.
            return;
        }*/

    }

    @Override
    protected void onResume()
    {
        // The activity must call the GL surface view's onResume() on activity onResume().
        super.onResume();
        mGLSurfaceView.onResume();

    }

    @Override
    protected void onPause()
    {
        // The activity must call the GL surface view's onPause() on activity onPause().
        super.onPause();
        mGLSurfaceView.onPause();
    }
}