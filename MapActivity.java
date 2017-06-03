package com.gotenna.sdk.sample.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.gotenna.sdk.sample.R;
import com.gotenna.sdk.sample.Utils.CustomMapTileProvider;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    String latlang;
     Double Latitude,Longitude;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent in=getIntent();
        latlang=in.getStringExtra("LatLang");   //get latitude and longitude from message activity

        final String[] LatLong = latlang.split(",");   //split latitude and longitude from string
        String  lat= LatLong[0];
        String  longi= LatLong[1];

         Latitude= Double.valueOf(lat);            //convert string into double
         Longitude= Double.valueOf(longi);
         Log.e("LatLong ",lat);

         SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map); //get fragment map

         mapFragment.getMapAsync(this);

    }

      private void setUpMap() {

            mMap.setMapType(GoogleMap.MAP_TYPE_NONE );   //set map type which you want to use

           //add tiles to map for offline map from assets folder using CustomMapTileProvider class
            mMap.addTileOverlay(new TileOverlayOptions().tileProvider(new CustomMapTileProvider(getResources().getAssets())));
            int zoom=10;
            CameraUpdate upd = CameraUpdateFactory.newLatLngZoom(new LatLng(Latitude, Longitude), zoom);//CameraUpdate move the camera of map to a particular latitude and longitude and aslo set the zoom level
            LatLng colorado = new LatLng(Latitude, Longitude);

            mMap.addMarker(new MarkerOptions().position(colorado).title("").snippet(latlang)); //add marker to the particuar latitude and longitude
            mMap.moveCamera(upd);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
         mMap = googleMap;
         setUpMap();   //now set up our offline map
    }
}
