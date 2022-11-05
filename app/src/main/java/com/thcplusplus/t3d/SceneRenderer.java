package com.thcplusplus.t3d;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renderers -- the static class GLES20 is used instead.
 */
public class  SceneRenderer implements GLSurfaceView.Renderer
{
    /** Store the accumulated rotation. */
    private final float[] mAccumulatedRotation = new float[16];

    /** Store the current rotation. */
    private final float[] mCurrentRotation = new float[16];
    /** Used for debug logs. */
    private static final String TAG = "SceneRenderer";

    private final Context mActivityContext;

    private GamePlay mGamePlay;
    /**
     * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
     * of being located at the center of the universe) to world space.
     */
    private float[] mModelMatrix = new float[16];

    /**
     * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
     * it positions things relative to our eye.
     */
    private float[] mViewMatrix = new float[16];

    /** Store the projection matrix. This is used to project the scene onto a 2D viewport. */
    private float[] mProjectionMatrix = new float[16];

    /** Allocate storage for the final combined matrix. This will be passed into the shader program. */
    private float[] mMVPMatrix = new float[16];

    /**
     * Stores a copy of the model matrix specifically for the light position.
     */
    private float[] mLightModelMatrix = new float[16];

    /** Store our model data in a float buffer. */
    private final FloatBuffer mCubePositions;
    private final FloatBuffer mCubeColors;
    private final FloatBuffer mCubeNormals;
    private final FloatBuffer mCubeTextureCoordinates;
    private ByteBuffer mIndexBuffer;

    /** This will be used to pass in the transformation matrix. */
    private int mMVPMatrixHandle;

    /** This will be used to pass in the modelview matrix. */
    private int mMVMatrixHandle;

    /** This will be used to pass in the light position. */
    private int mLightPosHandle;

    /** This will be used to pass in the texture. */
    private int mTextureUniformHandle;

    /** This will be used to pass in model position information. */
    private int mPositionHandle;

    /** This will be used to pass in model color information. */
    private int mColorHandle;

    /** This will be used to pass in model normal information. */
    private int mNormalHandle;

    /** This will be used to pass in model texture coordinate information. */
    private int mTextureCoordinateHandle;

    /** How many bytes per float. */
    private final int mBytesPerFloat = 4;

    /** Size of the position data in elements. */
    private final int mPositionDataSize = 3;

    /** Size of the color data in elements. */
    private final int mColorDataSize = 4;

    /** Size of the normal data in elements. */
    private final int mNormalDataSize = 3;

    /** Size of the texture coordinate data in elements. */
    private final int mTextureCoordinateDataSize = 2;

    /** Used to hold a light centered on the origin in model space. We need a 4th coordinate so we can get translations to work when
     *  we multiply this by our transformation matrices. */
    private final float[] mLightPosInModelSpace = new float[] {0.0f, 0.0f, 0.0f, 1.0f};

    /** Used to hold the current position of the light in world space (after transformation via model matrix). */
    private final float[] mLightPosInWorldSpace = new float[4];

    /** Used to hold the transformed position of the light in eye space (after transformation via modelview matrix) */
    private final float[] mLightPosInEyeSpace = new float[4];

    /** This is a handle to our cube shading program. */
    private int mProgramHandle;

    /** This is a handle to our light point program. */
    private int mPointProgramHandle;

    /** This is a handle to our texture data. */
    private final int[] mTextureDataHandle = new int[6];
    private final int[] TEXTURE_RESOURCES = {R.drawable.blankgrid,R.drawable.blankgrid2,R.drawable.blankgrid3,R.drawable.blankgrid4,R.drawable.bumpy_bricks_public_domain,R.drawable.bumpy_bricks_public_domain};
    private final int[] PLAYER_X_RESOURCES = { R.drawable.x_purple, R.drawable.x_gray, R.drawable.x_green, R.drawable.x_brown };
    private final int[] PLAYER_O_RESOURCES = { R.drawable.o_purple, R.drawable.o_gray, R.drawable.o_green, R.drawable.o_brown };
    /**
     * Initialize the model data.
     */
    public volatile float mDeltaX, mTotalDeltaX;
    public volatile float mDeltaY, mTotalDeltaY;
    public float mNormalX, mNormalY;
    public boolean mClicked = false, mOpponentStricken = false;
    private final float L = 2.0f;

    private FirebaseDatabase mDatabase;
    private DatabaseReference mMovementReference, mRotationReference;
    private String mPlayerRoleInRoom = "", mMovementUpdate = "",  mRotationUpdate = "0.0";
    public final String MOVEMENT_TAG = "movement", ROTATION_TAG = "rotation";

    private void setupDatabase() {
        mDatabase = FirebaseDatabase.getInstance();
        if( JoinRoomActivity.sRoomName.equals(LoginActivity.sUserName))
            mPlayerRoleInRoom = JoinRoomActivity.HOST_TAG;
        else
            mPlayerRoleInRoom = JoinRoomActivity.GUEST_TAG;
        mMovementReference = mDatabase.getReference( JoinRoomActivity.ALL_ROOMS_TAG + "/" + JoinRoomActivity.sRoomName + "/" + MOVEMENT_TAG);
        mMovementReference.setValue(mMovementUpdate);
        addMovementalEventListener();

        /*mRotationReference = mDatabase.getReference( JoinRoomActivity.ALL_ROOMS_TAG + "/" + JoinRoomActivity.sRoomName + "/" + ROTATION_TAG);
        mRotationReference.setValue(mRotationUpdate);*/
        //addRotationalEventListener();
    }

    /*private void addRotationalEventListener() {
        mRotationReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //mRotationUpdate = snapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                //error;
            }
        });
    }*/

    private void addMovementalEventListener() {
        mMovementReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(mPlayerRoleInRoom.equals(JoinRoomActivity.HOST_TAG)){
                    if(snapshot.getValue(String.class).contains(JoinRoomActivity.GUEST_TAG)){
                        mMovementUpdate = snapshot.getValue(String.class);
                        mOpponentStricken = true;
                    }
                }
                else{
                    if(snapshot.getValue(String.class).contains(JoinRoomActivity.HOST_TAG)){
                        mMovementUpdate = snapshot.getValue(String.class);
                        mOpponentStricken = true;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                //error
            }
        });
    }
    public SceneRenderer(final Context activityContext)
    {
        mActivityContext = activityContext;
        mGamePlay = new GamePlay();
        setupDatabase();

        // Define points for a cube.
        // X, Y, Z
        final float[] cubePositionData =
                {
                        // In OpenGL counter-clockwise winding is default. This means that when we look at a triangle,
                        // if the points are counter-clockwise we are looking at the "front". If not we are looking at
                        // the back. OpenGL has an optimization where all back-facing triangles are culled, since they
                        // usually represent the backside of an object and aren't visible anyways.
                        //Vertices according to faces
                        -L, -L, L, //Vertex 0
                        L, -L, L,  //v1
                        -L, L, L,  //v2
                        L, L, L,   //v3

                        L, -L, L,  //...
                        L, -L, -L,
                        L, L, L,
                        L, L, -L,

                        L, -L, -L,
                        -L, -L, -L,
                        L, L, -L,
                        -L, L, -L,

                        -L, -L, -L,
                        -L, -L, L,
                        -L, L, -L,
                        -L, L, L,

                        -L, -L, -L,
                        L, -L, -L,
                        -L, -L, L,
                        L, -L, L,

                        -L, L, L,
                        L, L, L,
                        -L, L, -L,
                        L, L, -L,

                };

        // R, G, B, A
        final float[] cubeColorData =
                {
                        // Front face (red)
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,

                        // Right face (green)
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,

                        // Back face (blue)
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        // Left face (yellow)
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,

                        // Top face (cyan)
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,

                        // Bottom face (magenta)
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f,

                };

        // X, Y, Z
        // The normal is used in light calculations and is a vector which points
        // orthogonal to the plane of the surface. For a cube model, the normals
        // should be orthogonal to the points of each face.
        final float[] cubeNormalData =
                {
                        // Front face
                        0.0f, 0.0f, 1.0f,
                        0.0f, 0.0f, 1.0f,
                        0.0f, 0.0f, 1.0f,
                        0.0f, 0.0f, 1.0f,
                        0.0f, 0.0f, 1.0f,
                        0.0f, 0.0f, 1.0f,

                        // Right face
                        1.0f, 0.0f, 0.0f,
                        1.0f, 0.0f, 0.0f,
                        1.0f, 0.0f, 0.0f,
                        1.0f, 0.0f, 0.0f,
                        1.0f, 0.0f, 0.0f,
                        1.0f, 0.0f, 0.0f,

                        // Back face
                        0.0f, 0.0f, -1.0f,
                        0.0f, 0.0f, -1.0f,
                        0.0f, 0.0f, -1.0f,
                        0.0f, 0.0f, -1.0f,
                        0.0f, 0.0f, -1.0f,
                        0.0f, 0.0f, -1.0f,

                        // Left face
                        -1.0f, 0.0f, 0.0f,
                        -1.0f, 0.0f, 0.0f,
                        -1.0f, 0.0f, 0.0f,
                        -1.0f, 0.0f, 0.0f,
                        -1.0f, 0.0f, 0.0f,
                        -1.0f, 0.0f, 0.0f,

                        // Top face
                        0.0f, 1.0f, 0.0f,
                        0.0f, 1.0f, 0.0f,
                        0.0f, 1.0f, 0.0f,
                        0.0f, 1.0f, 0.0f,
                        0.0f, 1.0f, 0.0f,
                        0.0f, 1.0f, 0.0f,

                        // Bottom face
                        0.0f, -1.0f, 0.0f,
                        0.0f, -1.0f, 0.0f,
                        0.0f, -1.0f, 0.0f,
                        0.0f, -1.0f, 0.0f,
                        0.0f, -1.0f, 0.0f,
                        0.0f, -1.0f, 0.0f
                };

        // S, T (or X, Y)
        // Texture coordinate data.
        // Because images have a Y axis pointing downward (values increase as you move down the image) while
        // OpenGL has a Y axis pointing upward, we adjust for that here by flipping the Y axis.
        // What's more is that the texture coordinates are the same for every face.
        final float[] cubeTextureCoordinateData =
                {

                        0.0f, 1.0f,
                        1.0f, 1.0f,
                        0.0f, 0.0f,
                        1.0f, 0.0f,

                        0.0f, 1.0f,
                        1.0f, 1.0f,
                        0.0f, 0.0f,
                        1.0f, 0.0f,

                        0.0f, 1.0f,
                        1.0f, 1.0f,
                        0.0f, 0.0f,
                        1.0f, 0.0f,

                        0.0f, 1.0f,
                        1.0f, 1.0f,
                        0.0f, 0.0f,
                        1.0f, 0.0f,

                        0.0f, 1.0f,
                        1.0f, 1.0f,
                        0.0f, 0.0f,
                        1.0f, 0.0f,

                        0.0f, 1.0f,
                        1.0f, 1.0f,
                        0.0f, 0.0f,
                        1.0f, 0.0f,
                };
        byte indices[] =
                {
                        //Faces definition
                        0,1,3, 0,3,2,           //Face front
                        4,5,7, 4,7,6,           //Face right
                        8,9,11, 8,11,10,        //...
                        12,13,15, 12,15,14,
                        16,17,19, 16,19,18,
                        20,21,23, 20,23,22,
                };
        // Initialize the buffers.
        mCubePositions = ByteBuffer.allocateDirect(cubePositionData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubePositions.put(cubePositionData).position(0);

        mCubeColors = ByteBuffer.allocateDirect(cubeColorData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeColors.put(cubeColorData).position(0);

        mCubeNormals = ByteBuffer.allocateDirect(cubeNormalData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeNormals.put(cubeNormalData).position(0);

        mCubeTextureCoordinates = ByteBuffer.allocateDirect(cubeTextureCoordinateData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeTextureCoordinates.put(cubeTextureCoordinateData).position(0);
        mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
        mIndexBuffer.put(indices);
        mIndexBuffer.position(0);
    }

    protected String getVertexShader()
    {
        return CppReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_vertex_shader);
    }

    protected String getFragmentShader()
    {
        return CppReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_fragment_shader);
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config)
    {
        // Set the background clear color to black.
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // Use culling to remove back faces.
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // The below glEnable() call is a holdover from OpenGL ES 1, and is not needed in OpenGL ES 2.
        // Enable texture mapping
        // GLES20.glEnable(GLES20.GL_TEXTURE_2D);

        // Position the eye in front of the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = -0.5f;

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = -5.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        final String vertexShader = getVertexShader();
        final String fragmentShader = getFragmentShader();

        final int vertexShaderHandle = ShaderLoader.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = ShaderLoader.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        mProgramHandle = ShaderLoader.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[] {"a_Position",  "a_Color", "a_Normal", "a_TexCoordinate"});

        // Define a simple shader program for our point.
        final String pointVertexShader = CppReader.readTextFileFromRawResource(mActivityContext, R.raw.point_vertex_shader);
        final String pointFragmentShader = CppReader.readTextFileFromRawResource(mActivityContext, R.raw.point_fragment_shader);

        final int pointVertexShaderHandle = ShaderLoader.compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
        final int pointFragmentShaderHandle = ShaderLoader.compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);
        mPointProgramHandle = ShaderLoader.createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle,
                new String[] {"a_Position"});

        // Load the texture
        for(int i = 0;i < 6;i++)
            mTextureDataHandle[i] = TextureLoader.loadTexture(mActivityContext, TEXTURE_RESOURCES[i]);
        Log.e("DEBUG", "Texture width is: " + Float.toString(TextureLoader.sBitmapWidth));
        // Initialize the accumulated rotation matrix
        Matrix.setIdentityM(mAccumulatedRotation, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height)
    {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float ratio = (float) width / height;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 1.0f;
        final float far = 10.0f;

        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }

    @Override
    public void onDrawFrame(GL10 glUnused)
    {
        /*if(mGamePlay.isPlayerInTurn(mPlayerRoleInRoom)){
            mRotationUpdate = Float.toString( mDeltaX );
            mRotationReference.setValue(mRotationUpdate);
        }
        else{
            mDeltaX = Float.parseFloat(mRotationUpdate);
        }*/
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        // Set our per-vertex lighting program.
        GLES20.glUseProgram(mProgramHandle);

        // Set program handles for cube drawing.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix");
        mLightPosHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos");
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");
        mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);


        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        // Calculate position of the light. Rotate and then push into the distance.
        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, -5.0f);
        Matrix.rotateM(mLightModelMatrix, 0, mDeltaX, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 2.0f);

        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -5.0f);
        // Set a matrix that contains the current rotation.
        Matrix.setIdentityM(mCurrentRotation, 0);
        Matrix.rotateM(mCurrentRotation, 0, mDeltaX, 0.0f, 1.0f, 0.0f);
        //Matrix.rotateM(mCurrentRotation, 0, mDeltaY, 1.0f, 0.0f, 0.0f);

        mDeltaX = 0.0f;
        mDeltaY = 0.0f;

        // Multiply the current rotation by the accumulated rotation, and then set the accumulated
        // rotation to the result.
        final float[] mTemporaryMatrix = new float[16];
        Matrix.multiplyMM(mTemporaryMatrix, 0, mCurrentRotation, 0, mAccumulatedRotation, 0);
        System.arraycopy(mTemporaryMatrix, 0, mAccumulatedRotation, 0, 16);

// Rotate the cube taking the overall rotation into account.
        Matrix.multiplyMM(mTemporaryMatrix, 0, mModelMatrix, 0, mAccumulatedRotation, 0);
        System.arraycopy(mTemporaryMatrix, 0, mModelMatrix, 0, 16);
        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, L, 0f, 0f, 0f, 0f, L, 0.0f);

        drawCube();


        // Draw a point to indicate the light.
        GLES20.glUseProgram(mPointProgramHandle);
        drawLight();

    }

    private void drawCube()
    {
        // Pass in the position information
        mCubePositions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                0, mCubePositions);

        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Pass in the color information
        mCubeColors.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                0, mCubeColors);

        GLES20.glEnableVertexAttribArray(mColorHandle);

        // Pass in the normal information
        mCubeNormals.position(0);
        GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false,
                0, mCubeNormals);

        GLES20.glEnableVertexAttribArray(mNormalHandle);

        // Pass in the texture coordinate information
        mCubeTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false,
                0, mCubeTextureCoordinates);

        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Pass in the light position in eye space.
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

        // Draw the cube.
        for(int i = 0;i < 6;i++) {
            // Bind the texture to this unit.
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle[i]);
            mIndexBuffer.position(6*i);
            //Draw the vertices as triangles, based on the Index Buffer information
            GLES20.glDrawElements(GL10.GL_TRIANGLES, 6, GL10.GL_UNSIGNED_BYTE, mIndexBuffer);
        }

        // determine coordinates of the new move
        Boolean inRangeTouch = false;
        byte x = 0, y = 0, currentFace = 0;

        if(mClicked && mGamePlay.isPlayerInTurn(mPlayerRoleInRoom)) {
            inRangeTouch = true;
            Log.d("DEBUG", Float.toString(mTotalDeltaX) + " " + Float.toString(mTotalDeltaY));
            if (mTotalDeltaX >= 350 || mTotalDeltaX <= 10)
                currentFace = 0;
            else if (mTotalDeltaX >= 260 && mTotalDeltaX <= 280)
                currentFace = 1;
            else if (mTotalDeltaX >= 170 && mTotalDeltaX <= 190)
                currentFace = 2;
            else if (mTotalDeltaX >= 80 && mTotalDeltaX <= 100)
                currentFace = 3;
            else
                currentFace = -1;

            if (currentFace >= 0) {
                if (mNormalX >= 0.45f && mNormalX <= 0.8f)
                    x = 3;
                else if (mNormalX >= 0.05f && mNormalX <= 0.35f)
                    x = 2;
                else if (mNormalX >= -0.35f && mNormalX <= -0.05f)
                    x = 1;
                else if (mNormalX >= -0.8f && mNormalX <= -0.45f)
                    x = 0;
                else
                    inRangeTouch = false;

                if (mNormalY >= 0.25f && mNormalY <= 0.4f)
                    y = 0;
                else if (mNormalY >= 0.05f && mNormalY <= 0.2f)
                    y = 1;
                else if (mNormalY >= -0.2f && mNormalY <= -0.05f)
                    y = 2;
                else if (mNormalY >= -0.4f && mNormalY <= -0.25f)
                    y = 3;
                else
                    inRangeTouch = false;
            }
        }
        if(mOpponentStricken){
            String[] splt = mMovementUpdate.split(" ");
            currentFace = Byte.parseByte(splt[1]);
            y = Byte.parseByte(splt[2]);
            x = Byte.parseByte(splt[3]);
            inRangeTouch = true;

        }

        // apply the move
        if (currentFace >= 0 && inRangeTouch) {
            final int SQUARE_WIDTH = (int) TextureLoader.sBitmapWidth / 4, SQUARE_HEIGHT = (int) TextureLoader.sBitmapHeight / 4;
            int xoffset = (int) ((x * SQUARE_WIDTH) + SQUARE_WIDTH / 5);
            int yoffset = (int) ((y * SQUARE_HEIGHT) + SQUARE_HEIGHT / 5);
            int resource;
            if (mGamePlay.getTurn() == 1)
                resource = PLAYER_X_RESOURCES[currentFace];
            else
                resource = PLAYER_O_RESOURCES[currentFace];

            if (mGamePlay.setNewMove(currentFace, y, x)) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle[currentFace]);
                final Bitmap bitmap = TextureLoader.getBitmapFromDrawable(mActivityContext, resource);
                GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, xoffset, yoffset, bitmap);
                bitmap.recycle();
                /*d******* database update ********/
                if(!mOpponentStricken) {
                    mMovementUpdate = mPlayerRoleInRoom + " " + Byte.toString(currentFace) + " " + Byte.toString(y) + " " + Byte.toString(x);
                    mMovementReference.setValue(mMovementUpdate);
                }
                if (mGamePlay.checkGameGrid(currentFace, y, x)) {
                    MainActivity.gameStatusButton.setText(mGamePlay.getBillboardText());
                    //draw game board( results )
                }
                if (mGamePlay.mCellFilled >= GamePlay.NUMBER_OF_CELLS) {
                    byte winner = mGamePlay.declareWinner();
                    // ... UI
                }
            }
        }
        mClicked = inRangeTouch = mOpponentStricken = false;
        // Free memory
    }

    /**
     * Draws a point representing the position of the light.
     */
    private void drawLight()
    {
        final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(mPointProgramHandle, "u_MVPMatrix");
        final int pointPositionHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_Position");

        // Pass in the position.
        GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2]);

        // Since we are not using a buffer object, disable vertex arrays for this attribute.
        GLES20.glDisableVertexAttribArray(pointPositionHandle);

        // Pass in the transformation matrix.
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mLightModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Draw the point.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
    }


}
