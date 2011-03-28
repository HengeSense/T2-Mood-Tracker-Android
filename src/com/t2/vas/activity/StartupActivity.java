package com.t2.vas.activity;

import android.content.Intent;
import android.os.Bundle;

import com.t2.vas.AppSecurityManager;
import com.t2.vas.Global;
import com.t2.vas.ReminderService;
import com.t2.vas.SharedPref;

public class StartupActivity extends ABSActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// update the database if the original update didn't take.
		// NOt sure why this happens, but this is a hack to fix the problem.
		try {
			this.onUpgrade(this.dbAdapter.getDatabase(), Global.Database.version - 1, Global.Database.version);
		} catch (Exception e) {
		}
		
		// Start the reminder service if it isn't already.
		ReminderService.startRunning(this);
		
		Intent i;
		// show the main activity, unlock screen will appear if necessary.
		if(SharedPref.Security.isEnabled(sharedPref)) {
			i = new Intent(this, MainActivity.class);
		
		/*// show the startup tips
		} else if(SharedPref.getShowStartupTips(sharedPref)) {
			i = new Intent(this, StartupTipActivity.class);*/
		
		// show the plain splash page.
		} else {
			i = new Intent(this, SplashScreenActivity.class);
		}
		
		this.startActivity(i);
		this.finish();
	}

}
