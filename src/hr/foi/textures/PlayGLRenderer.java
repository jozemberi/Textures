package hr.foi.textures;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import hr.foi.textures.helpers.RawResourceReader;
import hr.foi.textures.helpers.ShaderHelper;
import hr.foi.textures.helpers.TextureHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

/**
 * Provides drawing instructions for a GLSurfaceView object.
 */
public class PlayGLRenderer implements GLSurfaceView.Renderer {

	private final Context mActivityContext;

	/**
	 * Store the model matrix. This matrix is used to move models from object
	 * space (where each model can be thought of being located at the center of
	 * the universe) to world space.
	 */
	private float[] mModelMatrix = new float[16];

	/**
	 * Store the view matrix. This can be thought of as our camera. This matrix
	 * transforms world space to eye space; it positions things relative to our
	 * eye.
	 */
	private float[] mViewMatrix = new float[16];

	/**
	 * Store the projection matrix. This is used to project the scene onto a 2D
	 * viewport.
	 */
	private float[] mProjectionMatrix = new float[16];

	/**
	 * Allocate storage for the final combined matrix. This will be passed into
	 * the shader program.
	 */
	private float[] mMVPMatrix = new float[16];

	/** Store the accumulated rotation. */
	private final float[] mAccumulatedRotation = new float[16];

	/** Store the current rotation. */
	private final float[] mCurrentRotation = new float[16];

	/** A temporary matrix. */
	private float[] mTemporaryMatrix = new float[16];

	/** Store our model data in a float buffer. */
	private final FloatBuffer mCubePositions;
	private final FloatBuffer mCubeNormals;
	private final FloatBuffer mCubeTextureCoordinates;
	private final FloatBuffer mCubeTextureCoordinatesForPlane;

	/** This will be used to pass in the transformation matrix. */
	private int mMVPMatrixHandle;

	/** This will be used to pass in the modelview matrix. */
	private int mMVMatrixHandle;

	/** This will be used to pass in the texture. */
	private int mTextureUniformHandle;

	/** This will be used to pass in model position information. */
	private int mPositionHandle;

	/** This will be used to pass in model normal information. */
	private int mNormalHandle;

	/** This will be used to pass in model texture coordinate information. */
	private int mTextureCoordinateHandle;

	/** How many bytes per float. */
	private final int mBytesPerFloat = 4;

	/** Size of the position data in elements. */
	private final int mPositionDataSize = 3;

	/** Size of the normal data in elements. */
	private final int mNormalDataSize = 3;

	/** Size of the texture coordinate data in elements. */
	private final int mTextureCoordinateDataSize = 2;

	/** This is a handle to our cube shading program. */
	private int mProgramHandle;

	/** These are handles to our texture data. */
	private int mCubeDataHandle;
	private int mFloorDataHandle;

	public volatile float mDeltaX;
	public volatile float mDeltaY;

	private Bitmap cubeBitmap;

	private int selectedFloorTextureItem = 0;

	/**
	 * Initialize the model data.
	 */
	public PlayGLRenderer(final Context activityContext) {
		mActivityContext = activityContext;

		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inScaled = false; // No pre-scaling

		// Read in the resource
		cubeBitmap = BitmapFactory.decodeResource(
				mActivityContext.getResources(), R.drawable.foi_texture,
				options);

		// Define points for a cube.

		// X, Y, Z
		final float[] cubePositionData = {
				// In OpenGL counter-clockwise winding is default. This means
				// that when we look at a triangle,
				// if the points are counter-clockwise we are looking at the
				// "front". If not we are looking at
				// the back. OpenGL has an optimization where all back-facing
				// triangles are culled, since they
				// usually represent the backside of an object and aren't
				// visible anyways.

				// Front face
				-1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f,
				-1.0f, 1.0f, 1.0f, -1.0f,
				1.0f,
				1.0f,
				1.0f,
				1.0f,

				// Right face
				1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f,
				-1.0f, 1.0f, 1.0f, -1.0f, -1.0f,
				1.0f,
				1.0f,
				-1.0f,

				// Back face
				1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f,
				1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,
				1.0f,
				-1.0f,

				// Left face
				-1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f,
				-1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f,
				1.0f,

				// Top face
				-1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f,
				-1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f,

				// Bottom face
				1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f,
				1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, };

		// X, Y, Z
		// The normal is used in light calculations and is a vector which points
		// orthogonal to the plane of the surface. For a cube model, the normals
		// should be orthogonal to the points of each face.
		final float[] cubeNormalData = {
				// Front face
				0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f, 0.0f,
				1.0f,
				0.0f,
				0.0f,
				1.0f,

				// Right face
				1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
				1.0f,
				0.0f,
				0.0f,

				// Back face
				0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
				0.0f,
				-1.0f,

				// Left face
				-1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
				0.0f,

				// Top face
				0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,

				// Bottom face
				0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f };

		// S, T (or X, Y)
		// Texture coordinate data.
		// Because images have a Y axis pointing downward (values increase as
		// you move down the image) while
		// OpenGL has a Y axis pointing upward, we adjust for that here by
		// flipping the Y axis.
		// What's more is that the texture coordinates are the same for every
		// face.
		final float[] cubeTextureCoordinateData = {
				// Front face
				0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f,
				1.0f,
				1.0f,
				1.0f,
				1.0f,
				0.0f,

				// Right face
				0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
				1.0f,
				1.0f,
				1.0f,
				0.0f,

				// Back face
				0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f,
				1.0f,
				1.0f,
				0.0f,

				// Left face
				0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
				1.0f,
				0.0f,

				// Top face
				0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
				1.0f, 0.0f,

				// Bottom face
				0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
				1.0f, 0.0f };

		// S, T (or X, Y)
		// Texture coordinate data.
		// Because images have a Y axis pointing downward (values increase as
		// you move down the image) while
		// OpenGL has a Y axis pointing upward, we adjust for that here by
		// flipping the Y axis.
		// What's more is that the texture coordinates are the same for every
		// face.
		final float[] cubeTextureCoordinateDataForPlane = {
				// Front face
				0.0f, 0.0f, 0.0f, 25.0f, 25.0f, 0.0f, 0.0f,
				25.0f,
				25.0f,
				25.0f,
				25.0f,
				0.0f,

				// Right face
				0.0f, 0.0f, 0.0f, 25.0f, 25.0f, 0.0f, 0.0f, 25.0f,
				25.0f,
				25.0f,
				25.0f,
				0.0f,

				// Back face
				0.0f, 0.0f, 0.0f, 25.0f, 25.0f, 0.0f, 0.0f, 25.0f, 25.0f,
				25.0f,
				25.0f,
				0.0f,

				// Left face
				0.0f, 0.0f, 0.0f, 25.0f, 25.0f, 0.0f, 0.0f, 25.0f, 25.0f,
				25.0f, 25.0f,
				0.0f,

				// Top face
				0.0f, 0.0f, 0.0f, 25.0f, 25.0f, 0.0f, 0.0f, 25.0f, 25.0f,
				25.0f, 25.0f, 0.0f,

				// Bottom face
				0.0f, 0.0f, 0.0f, 25.0f, 25.0f, 0.0f, 0.0f, 25.0f, 25.0f,
				25.0f, 25.0f, 0.0f };

		// Initialize the buffers.
		mCubePositions = ByteBuffer
				.allocateDirect(cubePositionData.length * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mCubePositions.put(cubePositionData).position(0);

		mCubeNormals = ByteBuffer
				.allocateDirect(cubeNormalData.length * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mCubeNormals.put(cubeNormalData).position(0);

		mCubeTextureCoordinates = ByteBuffer
				.allocateDirect(
						cubeTextureCoordinateData.length * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mCubeTextureCoordinates.put(cubeTextureCoordinateData).position(0);

		mCubeTextureCoordinatesForPlane = ByteBuffer
				.allocateDirect(
						cubeTextureCoordinateDataForPlane.length
								* mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mCubeTextureCoordinatesForPlane.put(cubeTextureCoordinateDataForPlane)
				.position(0);
	}

	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		// Set the background clear color to black.
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		// Use culling to remove back faces.
		GLES20.glEnable(GLES20.GL_CULL_FACE);

		// Enable depth testing
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		// The below glEnable() call is a holdover from OpenGL ES 1, and is not
		// needed in OpenGL ES 2.
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

		// Set our up vector. This is where our head would be pointing were we
		// holding the camera.
		final float upX = 0.0f;
		final float upY = 1.0f;
		final float upZ = 0.0f;

		// Set the view matrix. This matrix can be said to represent the camera
		// position.
		// NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination
		// of a model and
		// view matrix. In OpenGL 2, we can keep track of these matrices
		// separately if we choose.
		Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY,
				lookZ, upX, upY, upZ);

		final String vertexShader = RawResourceReader
				.readTextFileFromRawResource(mActivityContext,
						R.raw.per_pixel_vertex_shader_tex_and_light);
		final String fragmentShader = RawResourceReader
				.readTextFileFromRawResource(mActivityContext,
						R.raw.per_pixel_fragment_shader_tex_and_light);

		final int vertexShaderHandle = ShaderHelper.compileShader(
				GLES20.GL_VERTEX_SHADER, vertexShader);
		final int fragmentShaderHandle = ShaderHelper.compileShader(
				GLES20.GL_FRAGMENT_SHADER, fragmentShader);

		mProgramHandle = ShaderHelper.createAndLinkProgram(vertexShaderHandle,
				fragmentShaderHandle, new String[] { "a_Position", "a_Normal",
						"a_TexCoordinate" });

		mCubeDataHandle = TextureHelper.loadTexture(cubeBitmap);
		GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

		setFloorTexture(selectedFloorTextureItem);

		// Initialize the accumulated rotation matrix
		Matrix.setIdentityM(mAccumulatedRotation, 0);
	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		// Set the OpenGL viewport to the same size as the surface.
		GLES20.glViewport(0, 0, width, height);

		// Create a new perspective projection matrix. The height will stay the
		// same
		// while the width will vary as per aspect ratio.
		final float ratio = (float) width / height;
		final float left = -ratio;
		final float right = ratio;
		final float bottom = -1.0f;
		final float top = 1.0f;
		final float near = 1.0f;
		final float far = 1000.0f;

		Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near,
				far);
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		// Set our per-vertex lighting program.
		GLES20.glUseProgram(mProgramHandle);

		// Set program handles for cube drawing.
		mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle,
				"u_MVPMatrix");
		mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle,
				"u_MVMatrix");
		mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle,
				"u_Texture");
		mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle,
				"a_Position");
		mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal");
		mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle,
				"a_TexCoordinate");

		// Set the active texture unit to texture unit 0.
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

		// Bind the texture to this unit.
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mCubeDataHandle);

		// Tell the texture uniform sampler to use this texture in the shader by
		// binding to texture unit 0.
		GLES20.glUniform1i(mTextureUniformHandle, 0);

		// Pass in the texture coordinate information
		mCubeTextureCoordinates.position(0);
		GLES20.glVertexAttribPointer(mTextureCoordinateHandle,
				mTextureCoordinateDataSize, GLES20.GL_FLOAT, false, 0,
				mCubeTextureCoordinates);

		letterF();
		letterO();
		letterI();

		// Draw a plane
		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, 0.0f, -2.0f, -5.0f);
		Matrix.scaleM(mModelMatrix, 0, 25.0f, 1.0f, 25.0f);

		// Set the active texture unit to texture unit 0.
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

		// Bind the texture to this unit.
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFloorDataHandle);

		// Tell the texture uniform sampler to use this texture in the shader by
		// binding to texture unit 0.
		GLES20.glUniform1i(mTextureUniformHandle, 0);

		// Pass in the texture coordinate information
		mCubeTextureCoordinatesForPlane.position(0);
		GLES20.glVertexAttribPointer(mTextureCoordinateHandle,
				mTextureCoordinateDataSize, GLES20.GL_FLOAT, false, 0,
				mCubeTextureCoordinatesForPlane);

		GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

		drawCube();
	}

	public void letterF() {
		// first cube from down
		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, -8.0f, 0.0f, -13.0f);
		touchRotation();
		drawCube();

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, -8.0f, 2.0f, -13.0f);
		touchRotation();
		drawCube();

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, -8.0f, 4.0f, -13.0f);
		touchRotation();
		drawCube();

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, -6.0f, 4.0f, -13.0f);
		touchRotation();
		drawCube();

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, -4.0f, 4.0f, -13.0f);
		touchRotation();
		drawCube();

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, -8.0f, 6.0f, -13.0f);
		touchRotation();
		drawCube();

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, -8.0f, 8.0f, -13.0f);
		touchRotation();
		drawCube();

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, -6.0f, 8.0f, -13.0f);
		touchRotation();
		drawCube();

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, -4.0f, 8.0f, -13.0f);
		touchRotation();
		drawCube();

	}

	public void letterO() {

		// first left down cube
		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -13.0f);
		touchRotation();
		drawCube();

		// second left cube
		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, 0.0f, 2.0f, -13.0f);
		touchRotation();
		drawCube();

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, 0.0f, 4.0f, -13.0f);
		touchRotation();
		drawCube();

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, 0.0f, 6.0f, -13.0f);
		touchRotation();
		drawCube();

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, 2.0f, 6.0f, -13.0f);
		touchRotation();
		drawCube();

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, 4.0f, 6.0f, -13.0f);
		touchRotation();
		drawCube();

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, 4.0f, 4.0f, -13.0f);
		touchRotation();
		drawCube();

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, 4.0f, 2.0f, -13.0f);
		touchRotation();
		drawCube();

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, 4.0f, 0.0f, -13.0f);
		touchRotation();
		drawCube();

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, 2.0f, 0.0f, -13.0f);
		touchRotation();
		drawCube();

	}

	public void letterI() {
		// first down cube
		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, 9.0f, 0.0f, -13.0f);
		touchRotation();
		drawCube();

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, 9.0f, 2.0f, -13.0f);
		touchRotation();
		drawCube();

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, 9.0f, 4.0f, -13.0f);
		touchRotation();
		drawCube();

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, 9.0f, 8.0f, -13.0f);
		touchRotation();
		drawCube();
	}

	public void touchRotation() {
		// Set a matrix that contains the current rotation.
		Matrix.setIdentityM(mCurrentRotation, 0);
		Matrix.rotateM(mCurrentRotation, 0, mDeltaX, 0.0f, 1.0f, 0.0f);
		Matrix.rotateM(mCurrentRotation, 0, mDeltaY, 1.0f, 0.0f, 0.0f);
		mDeltaX = 0.0f;
		mDeltaY = 0.0f;

		// Multiply the current rotation by the accumulated rotation, and then
		// set the accumulated rotation to the result.
		Matrix.multiplyMM(mTemporaryMatrix, 0, mCurrentRotation, 0,
				mAccumulatedRotation, 0);
		System.arraycopy(mTemporaryMatrix, 0, mAccumulatedRotation, 0, 16);

		// Rotate the cube taking the overall rotation into account.
		Matrix.multiplyMM(mTemporaryMatrix, 0, mModelMatrix, 0,
				mAccumulatedRotation, 0);
		System.arraycopy(mTemporaryMatrix, 0, mModelMatrix, 0, 16);

	}

	public void setFloorTexture(final int item) {
		// if (mFloorDataHandle != 0) {
		final int floorTextureId;
		if (item == 0) {
			floorTextureId = R.drawable.floor_texture_0;
		} else if (item == 1) {
			floorTextureId = R.drawable.floor_texture_1;
		} else if (item == 2) {
			floorTextureId = R.drawable.floor_texture_2;
		} else if (item == 3) {
			floorTextureId = R.drawable.floor_texture_3;
		} else if (item == 4) {
			floorTextureId = R.drawable.floor_texture_4;
		} else { // if (item == 5)
			floorTextureId = R.drawable.floor_texture_5;
		}

		mFloorDataHandle = TextureHelper.loadTexture(mActivityContext,
				floorTextureId);
		GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

		selectedFloorTextureItem = item;
	}

	/**
	 * Draws a cube.
	 */
	private void drawCube() {
		// Pass in the position information
		mCubePositions.position(0);
		GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize,
				GLES20.GL_FLOAT, false, 0, mCubePositions);

		GLES20.glEnableVertexAttribArray(mPositionHandle);

		// Pass in the normal information
		mCubeNormals.position(0);
		GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize,
				GLES20.GL_FLOAT, false, 0, mCubeNormals);

		GLES20.glEnableVertexAttribArray(mNormalHandle);

		// This multiplies the view matrix by the model matrix, and stores the
		// result in the MVP matrix
		// (which currently contains model * view).
		Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

		// Pass in the modelview matrix.
		GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

		// This multiplies the modelview matrix by the projection matrix, and
		// stores the result in the MVP matrix
		// (which now contains model * view * projection).
		Matrix.multiplyMM(mTemporaryMatrix, 0, mProjectionMatrix, 0,
				mMVPMatrix, 0);
		System.arraycopy(mTemporaryMatrix, 0, mMVPMatrix, 0, 16);

		// Pass in the combined matrix.
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

		// Draw the cube.
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
	}

	public void setCubeBitmap(Bitmap b) {
		this.cubeBitmap = b;
	}

}