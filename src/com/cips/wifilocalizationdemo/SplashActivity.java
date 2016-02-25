package com.cips.wifilocalizationdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;

public class SplashActivity extends Activity {
	private final long SPLASH_LENGTH = 500;
	Handler handler = new Handler();
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.splash);
        
        handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				Intent intent = new Intent(SplashActivity.this, MainActivity.class);
				SplashActivity.this.startActivity(intent);
				SplashActivity.this.finish();
			}
		}, SPLASH_LENGTH);//3ÃëºóÌø×ª
    }	
}
