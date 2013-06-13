package com.detail.quickdirections.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.detail.quickdirections.R;
import com.detail.quickdirections.model.GetDirectionsAsyncTask;
import com.detail.quickdirections.model.QuickDirectionsApp;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


// show information about route (time, distance)
// draw circle on each direction
// start button to show turn by turn clicking next button(back button also)
// follow button function
// change bike, drive, walking button style, checkbox style
// reverse button(swap from/to)
// search addresses (both from to address -> click on map autofills search textedits)
// settings with default search settings, auto fit screen, remember last selections
// loading progress thing
// satellite view
// longclick -> bookmark
// cancel asnctask
// max returned number of address when searched
// highlight text when textedit clicked

public class MainActivity extends FragmentActivity implements OnMapClickListener,
OnMapLongClickListener, OnMarkerDragListener, OnMyLocationChangeListener, OnMarkerClickListener{

	public final String TAG = "MainActivity";

	private GoogleMap mapView;
	private EditText searchView;


	private int mode;
	private boolean currentLoc;

	private LatLng fromPosition;
	private LatLng toPosition;

	private Marker fromMarker;
	private Marker toMarker;
	private Set<Marker> searchMarkers;

	private Polyline polyLine;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// setup views
		mapView = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
		searchView = (EditText) findViewById(R.id.search_text);

		// Find ZoomControl view
		mapView.getUiSettings().setAllGesturesEnabled(true);

		// setup field
		searchMarkers = new HashSet<Marker>();

		// set button background
		setButtonBackgrounds();

		// setup
		mapView.moveCamera(CameraUpdateFactory.newCameraPosition(QuickDirectionsApp.cp));
		mode = QuickDirectionsApp.modeId;
		onModeClicked(findViewById(QuickDirectionsApp.modeId));
		currentLoc = !QuickDirectionsApp.useCurrentChecked;
		onCurrentClicked(findViewById(R.id.use_current_location));

		// setup listeners
		mapView.setMyLocationEnabled(true);
		mapView.setOnMapClickListener(this);
		mapView.setOnMapLongClickListener(this);
		mapView.setOnMarkerDragListener(this);
		mapView.setOnMyLocationChangeListener(this);
		mapView.setOnMarkerClickListener(this);

	}



	public void onSearch(View view){
		for(Marker m : searchMarkers){
			m.remove();
		}
		searchMarkers.clear();

		InputMethodManager imm = (InputMethodManager) getSystemService(
				Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);

		String address = searchView.getText().toString();
		Geocoder geoCoder = new Geocoder(getBaseContext(), Locale.getDefault());
		try {

			//List<Address> addresses = geoCoder.getFromLocation(toPosition.latitude, toPosition.longitude, 4);
			List<Address> addresses = geoCoder.getFromLocationName(address, 100);
			Collections.sort(addresses, new AddressComparator());
			if (addresses.size() > 0) {

				LatLngBounds.Builder builder = new LatLngBounds.Builder();

				Location myLoc = mapView.getMyLocation();
				builder.include(new LatLng(myLoc.getLatitude(),myLoc.getLongitude()));
				int size = Math.min(1, addresses.size());
				for (int i = 0; i<size; i++){
					String strCompleteAddress= "";
					for (int j=0; j<addresses.get(i).getMaxAddressLineIndex();j++) {
						strCompleteAddress+= addresses.get(i).getAddressLine(j) + "\n";
					}
					LatLng temp = new LatLng(addresses.get(i).getLatitude(), addresses.get(i).getLongitude());
					searchMarkers.add(setSearchMarker(temp, strCompleteAddress));
					builder.include(temp);
				}
				LatLngBounds bounds = builder.build();
				int padding = 200; // offset from edges of the map in pixels
				CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
				mapView.animateCamera(cu);

			}
//			Toast.makeText(getBaseContext(), strCompleteAddress, Toast.LENGTH_LONG).show();
		}
		catch (IOException e) {
			Log.i("MyLocTAG => ", "this is the exception part");
			e.printStackTrace();
		}
	}

	public void setSearchText(View view){
		searchView.setSelectAllOnFocus(true);
	}

	public void setButtonBackgrounds(){
		findViewById(R.id.driving).getBackground().setColorFilter(0x40000000, Mode.MULTIPLY);
		findViewById(R.id.transit).getBackground().setColorFilter(0x40000000, Mode.MULTIPLY);
		findViewById(R.id.walking).getBackground().setColorFilter(0x40000000, Mode.MULTIPLY);
		findViewById(R.id.bicycling).getBackground().setColorFilter(0x40000000, Mode.MULTIPLY);
		findViewById(R.id.use_current_location).getBackground().setColorFilter(0x40000000, Mode.MULTIPLY);
	}

	// also called from layout
	public void onModeClicked(View view) {
		findViewById(mode).getBackground().setColorFilter(0x40000000, Mode.MULTIPLY);
		view.getBackground().setColorFilter(0x8033bfe5, Mode.MULTIPLY);
		mode = view.getId();
		refresh();
	}

	public void onCurrentClicked(View view) {
		if(currentLoc) {
			view.getBackground().setColorFilter(0x40000000, Mode.MULTIPLY);
		} else {
			view.getBackground().setColorFilter(0x8033bfe5, Mode.MULTIPLY);
			clearAll();
		}
		currentLoc = !currentLoc;
	}

	public void refresh(){
		mapView.clear();
		if (fromPosition != null) {
			if(currentLoc){
				Location loc = mapView.getMyLocation();
				fromPosition = new LatLng(loc.getLatitude(),loc.getLongitude());
			}
			fromMarker = setFromMarker();
		}
		if (toPosition != null) {
			toMarker = setToMarker();
			findDirections();
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

		if(currentLoc){
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

	public Marker setSearchMarker(LatLng ll, String title){
		return mapView.addMarker(new MarkerOptions().position(ll)
				.title(title).icon(BitmapDescriptorFactory
						.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
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
			QuickDirectionsApp.useCurrentChecked = currentLoc;
			QuickDirectionsApp.modeId = mode;
			super.onBackPressed();
		}
	}

	public void handleGetDirectionsResult(ArrayList<LatLng> directionPoints) {
		// make route fit on screen
		LatLngBounds.Builder builder = new LatLngBounds.Builder();
		builder.include(toPosition);
		builder.include(fromPosition);

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
		polyLine = mapView.addPolyline(rectLine);

		LatLngBounds bounds = builder.build();

		int padding = 200; // offset from edges of the map in pixels
		mapView.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
	}


	@SuppressWarnings("unchecked")
	public void findDirections() {

		Map<String, String> map = new HashMap<String, String>();
		map.put(GetDirectionsAsyncTask.USER_CURRENT_LAT, String.valueOf(fromPosition.latitude));
		map.put(GetDirectionsAsyncTask.USER_CURRENT_LONG, String.valueOf(fromPosition.longitude));
		map.put(GetDirectionsAsyncTask.DESTINATION_LAT, String.valueOf(toPosition.latitude));
		map.put(GetDirectionsAsyncTask.DESTINATION_LONG, String.valueOf(toPosition.longitude));
		map.put(GetDirectionsAsyncTask.DIRECTIONS_MODE, modeToString());

		GetDirectionsAsyncTask asyncTask = new GetDirectionsAsyncTask(this);
		asyncTask.execute(map);
	}

	public String modeToString(){
		if(mode == R.id.driving) {
			return "driving";
		}
		if(mode == R.id.transit) {
			return "transit";
		}
		if(mode == R.id.walking) {
			return "walking";
		}
		return "bicycling";
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
			if(currentLoc) {
				onCurrentClicked(findViewById(R.id.use_current_location));
			}
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
	public void onMyLocationChange(Location location) {

	}

	public class AddressComparator implements Comparator<Address> {

		@Override
		public int compare(Address a1, Address a2) {
			LatLng cll = mapView.getCameraPosition().target;
			double myLat = cll.latitude;
			double myLng = cll.longitude;

			double a1Lat = a1.getLatitude();
			double a1Lng = a1.getLongitude();

			double a1Distance = ((myLat-a1Lat)*(myLat-a1Lat)) + ((myLng-a1Lng)*(myLng-a1Lng));

			double a2Lat = a2.getLatitude();
			double a2Lng = a2.getLongitude();

			double a2Distance = ((myLat-a2Lat)*(myLat-a2Lat)) + ((myLng-a2Lng)*(myLng-a2Lng));

			return Double.compare(a1Distance, a2Distance);
		}
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		if(searchMarkers.contains(marker)) {
			onMapClick(marker.getPosition());
		}
		return false;
	}
}
