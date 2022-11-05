package com.thcplusplus.t3d;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

public class TextureLoader
{
	public static float sBitmapWidth = 0.0f,sBitmapHeight = 0.0f;

	public static Bitmap getBitmapFromDrawable(Context context , final int resourceId)
	{
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inScaled = false;	// No pre-scaling

		// Read in the resource
		// Bind to the texture in OpenGL
		final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
		return bitmap;
	}

	public static int loadTexture(final Context context,  int resourceId)
	{
		final int[] textureHandle = new int[1];

		GLES20.glGenTextures(1, textureHandle, 0);

		if (textureHandle[0] == 0)
		{
			throw new RuntimeException("Error generating texture name.");
		}

		// Bind to the texture in OpenGL
		final Bitmap bitmap = getBitmapFromDrawable(context, resourceId);

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

			// Set filtering
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
			//Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
			/*GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);*/

			// Load the bitmap into the bound texture.
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		if( sBitmapHeight == 0.0f && sBitmapWidth == 0.0f) {
			sBitmapHeight = bitmap.getHeight();
			sBitmapWidth = bitmap.getWidth();
		}
		bitmap.recycle();

		// Recycle the bitmap, since its data has been loaded into OpenGL.


		return textureHandle[0];
	}
}
