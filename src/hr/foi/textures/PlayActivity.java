package hr.foi.textures;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;

public class PlayActivity extends Activity {

	// Holds a reference to SurfaceView
	private PlayGLSurfaceView mGLSurfaceView;
	private PlayGLRenderer mRenderer;

	private static final int FLOOR_TEXTURE_DIALOG = 1;

	private int mFloorTextureSetting = 0;

	private static final String FLOOR_TEXTURE_SETTING = "floor_texture_setting";

	public static Bitmap bm = null;

	private AlertDialog floorTextureDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_play);

		mGLSurfaceView = (PlayGLSurfaceView) findViewById(R.id.gl_surface_view);

		// Check if the system supports OpenGL ES 2.0.
		final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		final ConfigurationInfo configurationInfo = activityManager
				.getDeviceConfigurationInfo();
		final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

		if (supportsEs2) {
			// Request an OpenGL ES 2.0 compatible context.
			mGLSurfaceView.setEGLContextClientVersion(2);

			final DisplayMetrics displayMetrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

			// Set the renderer to our demo renderer, defined below.
			mRenderer = new PlayGLRenderer(this);
			mGLSurfaceView.setRenderer(mRenderer, displayMetrics.density);
		} else {
			// Here can go OpenGL ES 1.x compatible renderer so that app
			// supports both ES 1 and ES 2.
			return;
		}

		findViewById(R.id.button_choose_floor_texture).setOnClickListener(
				new OnClickListener() {
					@SuppressWarnings("deprecation")
					@Override
					public void onClick(View v) {
						showDialog(FLOOR_TEXTURE_DIALOG);
					}
				});

		findViewById(R.id.button_camera).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent takePicture = new Intent(
								MediaStore.ACTION_IMAGE_CAPTURE);
						startActivityForResult(takePicture, 0);
					}
				});

		findViewById(R.id.button_gallery).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent pickPhoto = new Intent(
								Intent.ACTION_PICK,
								android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
						startActivityForResult(pickPhoto, 1);
					}
				});

		// Restore previous settings
		if (savedInstanceState != null) {
			mFloorTextureSetting = savedInstanceState.getInt(
					FLOOR_TEXTURE_SETTING, 0);
			if (mFloorTextureSetting != 0) {
				setFloorTextureSetting(mFloorTextureSetting);
			}
		}
	}

	@Override
	protected void onResume() {
		// The activity must call the GL surface view's onResume()
		super.onResume();
		mGLSurfaceView.onResume();
	}

	@Override
	protected void onPause() {
		// The activity must call the GL surface view's onPause()
		super.onPause();
		mGLSurfaceView.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(FLOOR_TEXTURE_SETTING, mFloorTextureSetting);
	}

	private void setFloorTextureSetting(final int item) {
		mFloorTextureSetting = item;

		mGLSurfaceView.queueEvent(new Runnable() {
			@Override
			public void run() {
				mRenderer.setFloorTexture(item);
			}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;

		switch (id) {
		case FLOOR_TEXTURE_DIALOG: {
			String[] names = getResources().getStringArray(
					R.array.floor_textures);

			AlertDialog.Builder builderFloorTexture = new AlertDialog.Builder(
					this);
			builderFloorTexture.setTitle(getResources().getString(
					R.string.choose_floor_texture));
			builderFloorTexture.setSingleChoiceItems(names,
					mFloorTextureSetting,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {

							if (item != mFloorTextureSetting) {
								setFloorTextureSetting(item);
								mFloorTextureSetting = item;
							}
							floorTextureDialog.dismiss();
						}
					});
			floorTextureDialog = builderFloorTexture.create();
			floorTextureDialog.show();
		}
			break;
		default:
			dialog = null;
			break;
		}

		return dialog;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case 0:
			if (resultCode == RESULT_OK) { // camera case
				Bundle extras = data.getExtras();
				bm = (Bitmap) extras.get("data");
				mRenderer.setCubeBitmap(getResizedBitmap(bm, 128, 128));
			}

			break;
		case 1: // gallery case
			if (resultCode == RESULT_OK) {
				Uri selectedImage = data.getData();
				String[] filePathColumn = { MediaStore.Images.Media.DATA };
				Cursor cursor = getContentResolver().query(selectedImage,
						filePathColumn, null, null, null);
				cursor.moveToFirst();
				int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
				String picturePath = cursor.getString(columnIndex);
				cursor.close();

				bm = BitmapFactory.decodeFile(picturePath);
				mRenderer.setCubeBitmap(getResizedBitmap(bm, 128, 128));
			}
			break;
		}
	}

	/**
	 * Returns resized bitmap.
	 * 
	 * @param bm
	 *            Bitmap for resizing
	 * @param newHeight
	 *            Wanted height of resized bitmap
	 * @param newWidth
	 *            Wanted width of resized bitmap
	 * @return Resized Bitmap
	 */
	protected Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
		int width = bm.getWidth();
		int height = bm.getHeight();
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;

		// CREATE A MATRIX FOR THE MANIPULATION
		Matrix matrix = new Matrix();
		// RESIZE THE BITMAP
		matrix.postScale(scaleWidth, scaleHeight);

		// "RECREATE" THE NEW BITMAP
		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height,
				matrix, false);
		return resizedBitmap;
	}

}