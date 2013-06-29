package com.detail.quickdirections.model;

import com.google.android.gms.maps.model.LatLng;

public class Direction {
	public LatLng latLng;
	public String mode;
	public String direction;
	public String distance;
	public Direction(LatLng latLng, String mode, String direction, String distance){
		this.latLng = latLng;
		this.mode = mode;
		this.direction = direction;
		this.distance = distance;
	}
}
