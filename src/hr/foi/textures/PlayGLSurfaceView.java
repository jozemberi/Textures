package hr.foi.textures;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * A view container where OpenGL ES graphics can be drawn on screen. This view
 * can also be used to capture touch events, such as a user interacting with
 * drawn objects.
 */
public class PlayGLSurfaceView extends GLSurfaceView {

	private PlayGLRenderer mRenderer;

	// Offsets for touch events
	private float mPreviousX;
	private float mPreviousY;

	private float mDensity;

	public PlayGLSurfaceView(Context context) {
		super(context);
	}

	public PlayGLSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event != null) {
			float x = event.getX();
			float y = event.getY();

			if (event.getAction() == MotionEvent.ACTION_MOVE) {
				if (mRenderer != null) {
					float deltaX = (x - mPreviousX) / mDensity / 2f;
					float deltaY = (y - mPreviousY) / mDensity / 2f;

					mRenderer.mDeltaX += deltaX;
					mRenderer.mDeltaY += deltaY;
				}
			}

			mPreviousX = x;
			mPreviousY = y;

			return true;
		} else {
			return super.onTouchEvent(event);
		}
	}

	// Hides superclass method.
	public void setRenderer(PlayGLRenderer renderer, float density) {
		mRenderer = renderer;
		mDensity = density;
		super.setRenderer(renderer);
	}
}
