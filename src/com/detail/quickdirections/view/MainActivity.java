package com.detail.quickdirections.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.detail.quickdirections.R;
import com.detail.quickdirections.model.GetDirectionsAsyncTask;
import com.detail.quickdirections.model.GetPlacesAsyncTask;
import com.detail.quickdirections.model.QuickDirectionsApp;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
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
// undo, redo button (one query(find direction in certain mode) is one "do")

public class MainActivity extends FragmentActivity implements OnMapClickListener,
OnMapLongClickListener, OnMarkerDragListener, OnInfoWindowClickListener,
OnMarkerClickListener, OnEditorActionListener, LocationListener{

	public final String TAG = "MainActivity";
	public GetDirectionsAsyncTask asyncTask = new GetDirectionsAsyncTask(this);


	public final int MAX_RESULT = 100;

	// Views
	private GoogleMap mapView;
	private AutoCompleteTextView searchView;
	private View summaryView;
	private TextView summaryTextView;


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
		searchView = (AutoCompleteTextView) findViewById(R.id.search_text);
		summaryView = findViewById(R.id.summary);
		summaryTextView = (TextView) findViewById(R.id.summary_text);

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
		mapView.setOnMarkerClickListener(this);
		mapView.setOnInfoWindowClickListener(this);
		searchView.setOnEditorActionListener(this);

		// autocomplete
		searchView.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (count%3 == 1) {
					//we don't want to make an insanely large array, so we clear it each time
					//					adapter.clear();
					//create the task
					GetPlacesAsyncTask task = new GetPlacesAsyncTask(MainActivity.this, s.toString());
					//now pass the argument in the textview to the task
					task.execute(searchView.getText().toString());
				}



			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub

			}
		});




		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Criteria crit = new Criteria();
		crit.setAccuracy(Criteria.ACCURACY_FINE);
		String provider = lm.getBestProvider(crit, true);
		Location loc = lm.getLastKnownLocation(provider);
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200000, 100, this);

	}


	public void setSearchText(View view){
		searchView.setSelectAllOnFocus(true);
	}

	public void setButtonBackgrounds(){
		findViewById(R.id.driving).getBackground().setColorFilter(0x40000000, Mode.MULTIPLY);
		//		findViewById(R.id.transit).getBackground().setColorFilter(0x40000000, Mode.MULTIPLY);
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

	public void onSummaryClick(View view) {
		// TODO Auto-generated method stub
	}

	public void onCurrentClicked(View view) {
		currentLoc = !currentLoc;

		if(currentLoc) {
			view.getBackground().setColorFilter(0x8033bfe5, Mode.MULTIPLY);
			if(toPosition == null && fromPosition != null){
				onMapClick(fromPosition);
			}else{
				refresh();
			}
		} else {
			view.getBackground().setColorFilter(0x40000000, Mode.MULTIPLY);
		}
	}

	public void refresh(){
		if(currentLoc && fromPosition != null) {
			Location loc = mapView.getMyLocation();
			fromPosition = new LatLng(loc.getLatitude(),loc.getLongitude());
			setFromMarker("From");
		}
		if(toPosition != null) {
			findDirections();
		}
	}

	@Override
	public void onMapLongClick(LatLng point) {

		LatLng temp = new LatLng(point.latitude, point.longitude);
		Marker marker = setClickMarker(temp, getAddress(point));
		marker.showInfoWindow();
	}

	// needs refactoring
	@Override
	public void onMapClick(LatLng point) {
		Log.d(TAG, "Clicked"+point.latitude+","+point.longitude);
		if (currentLoc) {
			Location loc = mapView.getMyLocation();
			if(loc == null){
				Toast.makeText(this, "Current location is not ready yet.", Toast.LENGTH_SHORT).show();
			} else {
				fromPosition = new LatLng(loc.getLatitude(),loc.getLongitude());
				toPosition = point;
				setFromMarker("From");
				setToMarker("To");
				findDirections();
			}
		} else {
			if (fromPosition == null) {
				fromPosition = point;
				setFromMarker("From");
			} else if (toPosition == null) {
				toPosition = point;
				setToMarker("To");
				findDirections();
			} else {
				removeTo(true);
				fromPosition = point;
				setFromMarker("From");
			}
		}

	}

	public String getAddress(LatLng point) {

		Geocoder geoCoder = new Geocoder(getBaseContext(), Locale.getDefault());
		String strCompleteAddress = "";
		try {
			List<Address> addresses = geoCoder.getFromLocation(point.latitude,  point.longitude, 1);
			for (int j=0; j<addresses.get(0).getMaxAddressLineIndex();j++) {
				strCompleteAddress+= addresses.get(0).getAddressLine(j) + "\n";
			}
		} catch (IOException e) {
			Log.i("MyLocTAG => ", "this is the exception part");
			e.printStackTrace();
		}
		return strCompleteAddress;
	}

	//.snippet("Population: 4,137,400") is gray text below title on marker info windows

	public void setToMarker(String s){
		removeTo(false);
		toMarker = mapView.addMarker(new MarkerOptions()
		.position(toPosition).title(s)
		.draggable(true).icon(BitmapDescriptorFactory
				.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
	}

	public void setFromMarker(String s){
		removeFrom(false);
		fromMarker =  mapView.addMarker(new MarkerOptions()
		.position(fromPosition).title(s)
		.draggable(true).icon(BitmapDescriptorFactory
				.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

	}

	public Marker setSearchMarker(LatLng ll, String title){
		return mapView.addMarker(new MarkerOptions().position(ll)
				.title(title).icon(BitmapDescriptorFactory
						.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
	}

	public Marker setClickMarker(LatLng ll, String address){
		return mapView.addMarker(new MarkerOptions().position(ll)
				.title(address).icon(BitmapDescriptorFactory
						.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
	}

	private void removeDirection(){
		polyLine.remove();
		summaryView.setVisibility(View.INVISIBLE);
	}


	public void removeTo(boolean position) {

		// double check
		if(polyLine != null) {
			removeDirection();
		}
		if (toMarker != null){
			toMarker.remove();
			toMarker = null;
		}
		if(position && toPosition != null) {
			//	setClickMarker(toPosition, "");
			toPosition = null;
		}
	}

	public void removeFrom(boolean position) {
		// double check
		if(polyLine != null) {
			removeDirection();
		}
		if (fromMarker != null){
			fromMarker.remove();
			fromMarker = null;
		}
		if(position) {
			fromPosition = null;
		}
	}


	@Override
	public void onBackPressed() {
		if(toPosition !=null) {
			removeTo(true);
		} else if(fromPosition != null) {
			removeFrom(true);
		} else {
			QuickDirectionsApp.cp = mapView.getCameraPosition();
			QuickDirectionsApp.useCurrentChecked = currentLoc;
			QuickDirectionsApp.modeId = mode;
			super.onBackPressed();
		}
	}

	public void handleGetDirectionsResult(ArrayList<LatLng> result) {
		// make route fit on screen
		LatLngBounds.Builder builder = new LatLngBounds.Builder();
		builder.include(toPosition);
		builder.include(fromPosition);

		PolylineOptions rectLine = new PolylineOptions().width(7).color(Color.BLUE);
		for(int i = 0 ; i < result.size() ; i++){
			LatLng temp = result.get(i);
			rectLine.add(temp);
			builder.include(temp);
		}

		polyLine = mapView.addPolyline(rectLine);
		Log.d(TAG, "Polyline drawn");


		summaryTextView.setText("@@@@@@@@@@@@@@@");
		summaryView.setVisibility(View.VISIBLE);


		LatLngBounds bounds = builder.build();

		int padding = 200; // offset from edges of the map in pixels
		mapView.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
	}


	public void handleGetPlacesResult(ArrayList<String> result) {
		//update the adapter
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(), R.layout.list_item);
		adapter.setNotifyOnChange(true);
		//attach the adapter to textview
		searchView.setAdapter(adapter);

		for (String string : result){

			Log.d("YourApp", "onPostExecute : result = " + string);
			adapter.add(string);
			adapter.notifyDataSetChanged();

		}
	}


	@SuppressWarnings("unchecked")
	public void findDirections() {
		if(polyLine !=null) {
			removeDirection();
		}

		if(asyncTask != null){
			asyncTask.cancel(true);
		}

		Map<String, String> map = new HashMap<String, String>();
		map.put(GetDirectionsAsyncTask.USER_CURRENT_LAT, String.valueOf(fromPosition.latitude));
		map.put(GetDirectionsAsyncTask.USER_CURRENT_LONG, String.valueOf(fromPosition.longitude));
		map.put(GetDirectionsAsyncTask.DESTINATION_LAT, String.valueOf(toPosition.latitude));
		map.put(GetDirectionsAsyncTask.DESTINATION_LONG, String.valueOf(toPosition.longitude));
		map.put(GetDirectionsAsyncTask.DIRECTIONS_MODE, modeToString());

		asyncTask = new GetDirectionsAsyncTask(this);
		asyncTask.execute(map);

		Log.d(TAG, "AsyncTask called");
	}

	public String modeToString(){
		if(mode == R.id.driving) {
			return "driving";
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
		if(marker.equals(fromMarker)){
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

	public class AddressCameraComparator implements Comparator<Address> {

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

	public class AddressCurrentComparator implements Comparator<Address> {

		@Override
		public int compare(Address a1, Address a2) {
			Location myLoc = mapView.getMyLocation();
			double myLat = myLoc.getLatitude();
			double myLng = myLoc.getLongitude();

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


	@Override
	public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
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
			List<Address> addresses = geoCoder.getFromLocationName(address, MAX_RESULT);
			int size = Math.min(1, addresses.size());

			if (size > 0) {
				LatLngBounds.Builder builder = new LatLngBounds.Builder();

				Location myLoc = mapView.getMyLocation();
				builder.include(new LatLng(myLoc.getLatitude(),myLoc.getLongitude()));

				Collections.sort(addresses, new AddressCameraComparator());

				for (int i = 0; i<addresses.size(); i++){
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
		}
		catch (IOException e) {
			Log.i("MyLocTAG => ", "this is the exception part");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		Log.d(TAG, "info window clicked");
		onMapClick(marker.getPosition());
	}


	@Override
	public void onLocationChanged(Location arg0) {
		// TODO Auto-generated method stub

	}


	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub

	}


	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub

	}


	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub

	}
}
