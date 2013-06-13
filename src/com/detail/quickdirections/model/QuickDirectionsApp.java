package com.detail.quickdirections.model;
import android.app.Application;
import android.content.Context;

import com.detail.quickdirections.R;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;


public class QuickDirectionsApp extends Application{
	private static Context context;

	// change position depending on locale
	public static CameraPosition cp = new CameraPosition(new LatLng(39.0, -96.0), 1, 0, 0);
	public static int modeId = R.id.driving;
	public static boolean curLoc = true;

	@Override
	public void onCreate() {
		super.onCreate();
		QuickDirectionsApp.context = getApplicationContext();
	}

	/**
	 * Provides a static way for classes to access the main application
	 * Context.
	 *
	 * @return the main application Context.
	 */
	public static Context getAppContext() {
		return QuickDirectionsApp.context;
	}
}
