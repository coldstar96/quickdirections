package com.detail.quickdirections.model;

import android.os.AsyncTask;

import com.detail.quickdirections.view.MainActivity;
import com.google.android.gms.maps.model.LatLng;

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.Map;

public class GetDirectionsAsyncTask extends AsyncTask<Map<String, String>, Object,   ArrayList<LatLng>> {

	public static final String USER_CURRENT_LAT = "user_current_lat";
	public static final String USER_CURRENT_LONG = "user_current_long";
	public static final String DESTINATION_LAT = "destination_lat";
	public static final String DESTINATION_LONG = "destination_long";
	public static final String DIRECTIONS_MODE = "directions_mode";
	private final MainActivity activity;

	private Exception exception;

	public GetDirectionsAsyncTask(MainActivity activity /*String url*/) {
		super();
		this.activity = activity;
	}

	@Override
	public void onPostExecute(ArrayList<LatLng> result) {
		if (exception == null) {
			activity.handleGetDirectionsResult(result);
		} else {
			processException();
		}
	}


	@Override
	protected ArrayList<LatLng> doInBackground(Map<String, String>... params) {
		Map<String, String> paramMap = params[0];
		try{
			LatLng fromPosition = new LatLng(Double.valueOf(paramMap.get(USER_CURRENT_LAT)) , Double.valueOf(paramMap.get(USER_CURRENT_LONG)));
			LatLng toPosition = new LatLng(Double.valueOf(paramMap.get(DESTINATION_LAT)) , Double.valueOf(paramMap.get(DESTINATION_LONG)));
			GMapV2Direction md = new GMapV2Direction();
			Document doc = md.getDocument(fromPosition, toPosition, paramMap.get(DIRECTIONS_MODE));
			ArrayList<LatLng> directionPoints = md.getDirection(doc);
			return directionPoints;

		}
		catch (Exception e) {
			exception = e;
			return null;
		}
	}

	private void processException(){
		// Toast.makeText(activity, activity.getString(R.string.error_when_retrieving_data), 3000).show();
	}
}