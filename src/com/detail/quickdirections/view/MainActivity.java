package com.detail.quickdirections.view;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.detail.quickdirections.R;
import com.detail.quickdirections.model.GetDirectionsAsyncTask;
import com.detail.quickdirections.model.QuickDirectionsApp;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


// show information about route (time, distance)
// draw circle on each direction
// start button to show turn by turn clicking next button(back button also)
// follow button function
// change bike, drive, walking button style, checkbox style
// reverse button(swap from/to)
// search addresses (both from to address -> click on map autofills search textedits)
// settings with default search settings, auto fit screen, remember last selections
// loading progress thing

public class MainActivity extends FragmentActivity implements OnMapClickListener,
OnMapLongClickListener, OnMarkerDragListener, OnCheckedChangeListener{

	public final String TAG = "MainActivity";

	private GoogleMap mapView;
	private CheckBox checkboxView;
	private RadioGroup radioGroupView;


	private String mode;

	private LatLng fromPosition;
	private LatLng toPosition;

	private Marker fromMarker;
	private Marker toMarker;

	Polyline polyLine;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// setup views
		mapView = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
		checkboxView = (CheckBox) findViewById(R.id.checkBox1);
		radioGroupView = (RadioGroup) findViewById(R.id.radioGroup1);

		radioGroupView.check(QuickDirectionsApp.modeId);

		// setup
		mapView.moveCamera(CameraUpdateFactory.newCameraPosition(QuickDirectionsApp.cp));
		setMode(QuickDirectionsApp.modeId);
		checkboxView.setChecked(QuickDirectionsApp.checked);


		// setup listeners
		mapView.setMyLocationEnabled(true);
		mapView.setOnMapClickListener(this);
		mapView.setOnMapLongClickListener(this);
		mapView.setOnMarkerDragListener(this);
		radioGroupView.setOnCheckedChangeListener(this);

	}

	public void setMode(int id) {
		switch (id) {
		case R.id.radio0:
			mode = "driving";
			break;
		case R.id.radio1:
			mode = "bicycling";
			break;
		case R.id.radio2:
			mode = "walking";
			break;
		case R.id.radio3:
			mode = "transit";
			break;
		}
	}

	public void refresh(){
		mapView.clear();
		if (fromPosition != null) {
			fromMarker = setFromMarker();
		}
		if (toPosition != null) {
			toMarker = setToMarker();
			findDirections();
		}
	}

	// called from layout
	public void changeCurrent(View view){
		if(checkboxView.isChecked()){
			clearAll();
		}
	}

	@Override
	public void onMapLongClick(LatLng point) {
		// TODO Auto-generated method stub

	}

	// needs refactoring
	@Override
	public void onMapClick(LatLng point) {
		Log.d(TAG, "Clicked"+point.latitude+","+point.longitude);

		if(checkboxView.isChecked()){
			clearAll();
			Location loc = mapView.getMyLocation();
			if(loc !=null) {
				fromPosition = new LatLng(loc.getLatitude(),loc.getLongitude());
				toPosition = point;
				fromMarker = setFromMarker();
				toMarker = setToMarker();
				findDirections();
			} else {
				// Toast
			}
		} else if(fromPosition != null && toPosition == null) {
			toPosition = point;
			toMarker = setToMarker();
			findDirections();
		}else{
			clearAll();
			fromPosition = point;
			fromMarker = setFromMarker();
		}

	}

	public Marker setToMarker(){
		return mapView.addMarker(new MarkerOptions().position(toPosition)
				.title("To").draggable(true).icon(BitmapDescriptorFactory
						.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
	}

	public Marker setFromMarker(){
		return mapView.addMarker(new MarkerOptions().position(fromPosition)
				.title("From").draggable(true).icon(BitmapDescriptorFactory
						.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
	}

	public void clearAll() {
		mapView.clear();
		fromPosition = null;
		toPosition = null;
		fromMarker = null;
		toMarker = null;
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		if(toPosition !=null) {
			toPosition = null;
			toMarker.remove();
			polyLine.remove();
		} else if(fromPosition != null) {
			fromPosition = null;
			fromMarker.remove();
		} else {
			QuickDirectionsApp.cp = mapView.getCameraPosition();
			QuickDirectionsApp.checked = checkboxView.isChecked();
			QuickDirectionsApp.modeId = radioGroupView.getCheckedRadioButtonId();
			super.onBackPressed();
		}
	}

	public void handleGetDirectionsResult(ArrayList<LatLng> directionPoints) {
		// make route fit on screen
		LatLngBounds.Builder builder = new LatLngBounds.Builder();
		builder.include(toPosition);
		builder.include(fromPosition);


		GoogleMap mMap = ((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
		PolylineOptions rectLine = new PolylineOptions().width(7).color(Color.BLUE);
		for(int i = 0 ; i < directionPoints.size() ; i++){
			LatLng temp = directionPoints.get(i);
			rectLine.add(temp);
			builder.include(temp);
		}

		//circleoption

		if(polyLine !=null) {
			polyLine.remove();
		}
		polyLine = mMap.addPolyline(rectLine);

		LatLngBounds bounds = builder.build();

		int padding = 100; // offset from edges of the map in pixels
		CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

		mapView.animateCamera(cu);
	}


	@SuppressWarnings("unchecked")
	public void findDirections() {

		Map<String, String> map = new HashMap<String, String>();
		map.put(GetDirectionsAsyncTask.USER_CURRENT_LAT, String.valueOf(fromPosition.latitude));
		map.put(GetDirectionsAsyncTask.USER_CURRENT_LONG, String.valueOf(fromPosition.longitude));
		map.put(GetDirectionsAsyncTask.DESTINATION_LAT, String.valueOf(toPosition.latitude));
		map.put(GetDirectionsAsyncTask.DESTINATION_LONG, String.valueOf(toPosition.longitude));
		map.put(GetDirectionsAsyncTask.DIRECTIONS_MODE, mode);

		GetDirectionsAsyncTask asyncTask = new GetDirectionsAsyncTask(this);
		asyncTask.execute(map);
	}

	@Override
	public void onMarkerDrag(Marker marker) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onMarkerDragEnd(Marker marker) {
		// TODO Auto-generated method stub
		if(marker.getTitle().equals("From")){
			fromPosition = marker.getPosition();
			checkboxView.setChecked(false);
		} else {
			toPosition = marker.getPosition();
		}
		refresh();
	}

	@Override
	public void onMarkerDragStart(Marker marker) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		// TODO Auto-generated method stub
		Log.d(TAG,""+checkedId);
		setMode(checkedId);
		refresh();
	}
}
