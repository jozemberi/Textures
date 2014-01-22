package hr.foi.textures;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
	
	/**
	 * Starts PlayActivity
	 */
	public void startPlayActivity(View metu) {
		Intent i = new Intent(this, PlayActivity.class);
		startActivity(i);
	}

}
