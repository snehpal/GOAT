package com.gotenna.sdk.sample.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.gotenna.sdk.sample.R;
import com.gotenna.sdk.sample.Utils.CustomMapTileProvider;
import com.gotenna.sdk.sample.Utils.UserDBUtil;
import com.gotenna.sdk.sample.Utils.Utility;
import com.gotenna.sdk.sample.models.Message;
import com.google.android.gms.maps.model.LatLngBounds.Builder;

import java.util.ArrayList;

public class MapActivity_new extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    UserDBUtil userDb;
    Utility util;
    ArrayList<Message> broadcastList=new ArrayList<Message>();
    private ArrayList<LatLng>listLatLng;
    private RelativeLayout rlMapLayout;
    Polyline line;
    String selectedName="";
    //String latlang;
     Double Latitude,Longitude;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rlMapLayout=(RelativeLayout) findViewById(R.id.rlMapLayout);


        util = new Utility();
        util.copyUserDataBase(getApplicationContext());
        userDb = new UserDBUtil(getApplicationContext());

        Intent in=getIntent();
        String Location=in.getStringExtra("Location");
        if (Location.equals("MyLocation")){
            broadcastList= userDb.CoordinatesList();
            Log.e("BroadCastList ", String.valueOf(broadcastList.size()));
            Toast.makeText(getApplicationContext(),"BroadCastList "+broadcastList.size(),Toast.LENGTH_LONG).show();
        }else if (Location.equals("Received")){
            String selectedGID= in.getStringExtra("selectedContactGID");
            selectedName=in.getStringExtra("selectedContactName");
            broadcastList= userDb.ReceivedCoordinatesList(selectedGID);
            Toast.makeText(getApplicationContext(),"ListReceived "+broadcastList.size(),Toast.LENGTH_LONG).show();
            Log.e("BroadCastList ", String.valueOf(broadcastList.size()));
        }

        if (broadcastList.size() == 0) {

            Toast.makeText(MapActivity_new.this,"No Coordinates available",Toast.LENGTH_LONG).show();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map); //get fragment map

        mapFragment.getMapAsync(this);

    }

      private void setUpMap() {

            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);   //set map type which you want to use
            listLatLng=new ArrayList<LatLng>();
           //add tiles to map for offline map from assets folder using CustomMapTileProvider class
            mMap.addTileOverlay(new TileOverlayOptions().tileProvider(new CustomMapTileProvider(getResources().getAssets())));

            for(int i=0;i<broadcastList.size();i++){
              LatLng ll = new LatLng(broadcastList.get(i).getLatitude(), broadcastList.get(i).getLongitude());
              // Getting the latitude of the i-th location
              Latitude=broadcastList.get(i).getLatitude();
              Longitude=broadcastList.get(i).getLongitude();
              // Drawing marker on the map
              drawMarker(new LatLng(Latitude,Longitude));
              listLatLng.add(ll);
            }

          // Moving CameraPosition to last clicked position
         // mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Latitude,Longitude),zoom));

          // Setting the zoom level in the map on last position  is clicked
        //  mMap.animateCamera(CameraUpdateFactory.zoomTo(zoom));

         /*try {

         }catch (Exception e){
             Log.e("Exception ",e.toString());
         }*/


          if (listLatLng != null && listLatLng.size() > 1) {

              PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
              for (int i = 0; i < listLatLng.size(); i++) {

                  LatLng point = listLatLng.get(i);
                  options.add(point);
              }

              line = mMap.addPolyline(options); //add Polyline
          }


    }

    private void drawMarker(LatLng point){
        // Creating an instance of MarkerOptions
        int zoom=12;
        CameraUpdate upd = CameraUpdateFactory.newLatLngZoom(point, zoom);//CameraUpdate move the camera of map to a particular latitude and longitude and aslo set the zoom level
      //  LatLng colorado = new LatLng(Latitude, Longitude);
        String Latlng= String.valueOf(point);
        mMap.addMarker(new MarkerOptions().position(point).title("selectedName").snippet(Latlng)); //add marker to the particuar latitude and longitude
        mMap.moveCamera(upd);




      /*  MarkerOptions markerOptions = new MarkerOptions();
         String Latlng= String.valueOf(point);
        // Setting latitude and longitude for the marker
        *//*LatLng colorado = new LatLng(Latitude, Longitude);*//*
        markerOptions.position(point);
        markerOptions.title(selectedName);
        markerOptions.snippet(Latlng);
        // Adding marker on the Google Map
        mMap.addMarker(markerOptions);
        int zoom=10;
        CameraUpdate upd = CameraUpdateFactory.newLatLngZoom(point, zoom);//CameraUpdate move the camera of map to a particular latitude and longitude and aslo set the zoom level
        mMap.moveCamera(upd);
*/
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
         mMap = googleMap;
         setUpMap();   //now set up our offline map
    }
}
