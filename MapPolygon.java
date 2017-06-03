package com.gotenna.sdk.sample.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.gotenna.sdk.sample.R;
import com.gotenna.sdk.sample.Utils.CustomMapTileProvider;

import java.util.ArrayList;
import java.util.List;

public class MapPolygon extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    String latlang;
     Double Latitude,Longitude;
    private ArrayList<LatLng> arrayPoints = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        arrayPoints = new ArrayList<LatLng>();

        String mapstr=getIntent().getStringExtra("latlng");   //get String of latitude and longitude

        String[] items = mapstr.split(System.getProperty("line.separator"));  //split the string  with line seperator


        for (int i=1;i<items.length;i++){
            String latlang=items[i];
            Log.e("latlng ",latlang);

            String[] LatLong = latlang.split(","); //split latitude and longitude from string
            String Latitude=LatLong[0];  //now get latitude from string
            String Longitude=LatLong[1];  //now get longitude from string

            Double lat= Double.valueOf(Latitude);  //convert string into double
            Double longi= Double.valueOf(Longitude);

            LatLng latLong_new=new LatLng(lat,longi);
            arrayPoints.add(latLong_new);   //add latitude and longitude into arraylist
            //  }
            Toast.makeText(MapPolygon.this,"arraypoints "+arrayPoints.size(),Toast.LENGTH_LONG).show();
        }


         SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map); //get fragment map

         mapFragment.getMapAsync(this);

    }

      private void setUpMap() {

            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);   //set map type which you want to use
           //add tiles to map for offline map from assets folder using CustomMapTileProvider class
             mMap.addTileOverlay(new TileOverlayOptions().tileProvider(new CustomMapTileProvider(getResources().getAssets())));
             drawpolygon(arrayPoints); //Draw Polygon

    }

    private void drawpolygon(ArrayList<LatLng> arrayPoints) {
        int length = arrayPoints.size();

        // Optional length checks. Modify yourself.
        if (length == 0) {
            // Do whatever you like then get out. Do not run the following.
            return;
        }

        // We have a length of not 0 so...
        PolygonOptions poly = new PolygonOptions();
        poly.strokeColor(Color.RED);
        poly.fillColor(Color.BLUE);
        poly.zIndex(4);

        // Initial point
        poly.add(arrayPoints.get(0));

        // ... then the rest.
        for (int i = 0; i < length; i++) {
            poly.add(arrayPoints.get(i));
            mMap.addMarker(new MarkerOptions().position(arrayPoints.get(i)));
            CameraUpdate upd = CameraUpdateFactory.newLatLngZoom(arrayPoints.get(i), 12);
            mMap.moveCamera(upd);
        }

        // Done! Add to map.
        mMap.addPolygon(poly);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
         mMap = googleMap;
         setUpMap();   //now set up our offline map
    }
}
