package com.detail.quickdirections.model;

import android.os.AsyncTask;
import android.util.Log;

import com.detail.quickdirections.view.MainActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

public class GetPlacesAsyncTask extends AsyncTask<String, Void, ArrayList<String>>{
	private final String s;
	private final MainActivity activity;

	public GetPlacesAsyncTask(MainActivity activity, String s){
		this.s = s;
		this.activity = activity;
	}
	@Override
	// three dots is java for an array of strings
	protected ArrayList<String> doInBackground(String... args){

		Log.d("gottaGo", "doInBackground");

		ArrayList<String> predictionsArr = new ArrayList<String>();

		try{

			URL googlePlaces = new URL(
					// URLEncoder.encode(url,"UTF-8");
					"https://maps.googleapis.com/maps/api/place/autocomplete/json?input="+ URLEncoder.encode(s.toString(), "UTF-8") +"&types=geocode&language=en&sensor=true&key=<yourapikeygoeshere>");
			Log.d("gottaGo", "https://maps.googleapis.com/maps/api/place/autocomplete/json?input="+ URLEncoder.encode(s.toString(), "UTF-8") +"&types=geocode&language=en&sensor=true&key=<yourapikeygoeshere>");


			URLConnection tc = googlePlaces.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(
					tc.getInputStream()));

			String line;
			StringBuffer sb = new StringBuffer();
			//take Google's legible JSON and turn it into one big string.
			while ((line = in.readLine()) != null) {
				sb.append(line);
			}
			//turn that string into a JSON object
			JSONObject predictions = new JSONObject(sb.toString());
			//now get the JSON array that's inside that object
			JSONArray ja = new JSONArray(predictions.getString("predictions"));

			for (int i = 0; i < ja.length(); i++) {
				JSONObject jo = (JSONObject) ja.get(i);
				//add each entry to our array
				predictionsArr.add(jo.getString("description"));
			}
		} catch (IOException e){
			Log.e("YourApp", "GetPlaces : doInBackground", e);

		} catch (JSONException e){
			Log.e("YourApp", "GetPlaces : doInBackground", e);

		}

		return predictionsArr;

	}

	//then our post

	@Override
	protected void onPostExecute(ArrayList<String> result){


		Log.d("YourApp", "onPostExecute : " + result.size());

		activity.handleGetPlacesResult(result);

	}

}
