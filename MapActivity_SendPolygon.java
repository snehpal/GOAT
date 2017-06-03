package com.gotenna.sdk.sample.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.gotenna.sdk.bluetooth.GTConnectionManager;
import com.gotenna.sdk.commands.GTCommand;
import com.gotenna.sdk.commands.GTCommandCenter;
import com.gotenna.sdk.commands.GTError;
import com.gotenna.sdk.gids.GIDManager;
import com.gotenna.sdk.interfaces.GTErrorListener;
import com.gotenna.sdk.responses.GTResponse;
import com.gotenna.sdk.sample.R;
import com.gotenna.sdk.sample.Utils.CustomMapTileProvider;
import com.gotenna.sdk.sample.models.Message;
import com.gotenna.sdk.types.GTDataTypes;
import com.gotenna.sdk.user.User;
import com.gotenna.sdk.user.UserDataStore;
import com.gotenna.sdk.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MapActivity_SendPolygon extends  AppCompatActivity implements OnMapReadyCallback,GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener, View.OnClickListener {
    private GoogleMap mMap;
    private ArrayList<LatLng> arrayPoints = null;
    PolylineOptions polylineOptions;
    private boolean checkClick = false;

    boolean markerClicked;
    PolygonOptions polygonOptions;
    Polygon polygon;
    Button button_sendPolygon,button_clearPolygon;
    public String messageStatus;
    private static final String LOG_TAG = "MessageActivity";
    private static final int MESSAGE_RESEND_DELAY_MILLISECONDS = 5000;
    protected Handler messageResendHandler;
    protected boolean willEncryptMessages = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button_sendPolygon= (Button) findViewById(R.id.button_sendPolygon);
        button_clearPolygon= (Button) findViewById(R.id.button_clearPolygon);
        arrayPoints = new ArrayList<LatLng>();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        markerClicked = false;

        button_sendPolygon.setOnClickListener(this);
        button_clearPolygon.setOnClickListener(this);
    }

      private void setUpMap() {

          mMap.setOnMapClickListener(this);    //map click listner
          mMap.setOnMapLongClickListener(this); //map long click listner
          mMap.setOnMarkerClickListener(this); //marker click listner

          mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);  //set map type
          mMap.addTileOverlay(new TileOverlayOptions().tileProvider(new CustomMapTileProvider(getResources().getAssets())));//add tiles to map for offline use

          CameraUpdate upd = CameraUpdateFactory.newLatLngZoom(new LatLng(38.783059, -104.880377), 11);
          mMap.moveCamera(upd);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
         mMap = googleMap;
         setUpMap();   //now set up our offline map
    }

    @Override
    public void onMapLongClick(LatLng point) {

        mMap.addMarker(new MarkerOptions().position(point).title(point.toString()));  //add marker to particular position

        arrayPoints.add(point);  //add point to arraylist on map long press

        markerClicked = false;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        if(markerClicked){   //check marker click

            if(polygon != null){
                polygon.remove();
                polygon = null;
            }

            polygonOptions.add(marker.getPosition());  //set polygon options
            polygonOptions.strokeColor(Color.RED);
            polygonOptions.fillColor(Color.BLUE);
            polygonOptions.zIndex(4);

            polygon = mMap.addPolygon(polygonOptions);  //add polygon to map

        }else{
            if(polygon != null){
                polygon.remove();
                polygon = null;
            }

            polygonOptions = new PolygonOptions().add(marker.getPosition());  //add marker
            markerClicked = true;
        }

        return true;
    }


    @Override
    public void onMapClick(LatLng point) {
        // tvLocInfo.setText(point.toString());
        mMap.animateCamera(CameraUpdateFactory.newLatLng(point));

        markerClicked = false;
    }

    @Override
    public void onClick(View v) {
        if (v==button_sendPolygon){
            attemptToSendBroadcastPolygon(arrayPoints);  //send broadcast polygon
        }
        if (v==button_clearPolygon){
            mMap.clear();
            arrayPoints.clear();
            setUpMap();
        }
    }

    public void attemptToSendBroadcastPolygon(ArrayList<LatLng> arrayPoints)
    {
        messageStatus="BroadCastMessage"; //set message status to find which type of message send
        attemptToSendPolygon(GIDManager.SHOUT_GID, true,arrayPoints);

    }



    private void attemptToSendPolygon(long receiverGID, boolean isBroadcast, ArrayList<LatLng> arrayPoints)
    {
        User currentUser = UserDataStore.getInstance().getCurrentUser();

        if (currentUser == null)
        {
            Toast toast = Toast.makeText(getApplicationContext(), R.string.must_choose_user_toast_text, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();

            Intent intent = new Intent(this, SetGidActivity.class);
            startActivity(intent);
            return;
        }

        StringBuilder messages = new StringBuilder();  //string  message to send polygon coordinates
        messages.append("Received Polygon: ");
        messages.append(System.getProperty("line.separator"));
        for (int i=0;i<arrayPoints.size();i++){

            messages.append(String.valueOf(arrayPoints.get(i).latitude));
            messages.append(",");
            messages.append(String.valueOf(arrayPoints.get(i).longitude));
            messages.append(System.getProperty("line.separator"));

        }
        String messageText = String.valueOf(messages);
        Log.e("MessageText ",messageText);
        if (arrayPoints.size() < 2)
        {
            Toast.makeText(MapActivity_SendPolygon.this,"Select a Polygon",Toast.LENGTH_LONG).show();
            return;
        }

        Message messageToSend = Message.createReadyToSendMessage(currentUser.getGID(), receiverGID, messageText);
        boolean didSend = sendMessage(messageToSend, isBroadcast);

        if (didSend)
        {
            //messagesList.add(messageToSend);
            //updateMessagingUI();

        }

    }

    private boolean sendMessage(final Message message, final boolean isBroadcast)   //send messsage
    {
        if (message != null && message.toBytes() != null)
        {
            if (GTConnectionManager.getInstance().isConnected())
            {
                final GTCommand.GTCommandResponseListener responseListener = new GTCommand.GTCommandResponseListener()
                {
                    @Override
                    public void onResponse(GTResponse response)
                    {
                        // Parse the response we got about whether our message got through successfully
                        if (response.getResponseCode() == GTDataTypes.GTCommandResponseCode.POSITIVE)
                        {
                            message.setMessageStatus(Message.MessageStatus.SENT_SUCCESSFULLY);
                            //get message status and save message to directory according to which type of message send
                            if (messageStatus.equals("BroadCastMessage")){
                                String folder_main = "Broadcast Messsages";


                                File f = new File(Environment.getExternalStorageDirectory(), folder_main);
                                if (!f.exists()) {
                                    f.mkdirs();
                                }

                                File f1 = new File(Environment.getExternalStorageDirectory() + "/" + folder_main, "Send Messages");
                                if (!f1.exists()) {
                                    f1.mkdirs();
                                }

                                User currentUser = UserDataStore.getInstance().getCurrentUser();
                                String GID= String.valueOf(currentUser.getGID());


                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                String timeStamp = dateFormat.format(new Date()); // Find todays date

                                String date= String.valueOf(new Date());

                                //userDb.insertBroadCastSemdMessage(message.getSenderGID(),message.getReceiverGID(),date,message.getText(),message.getMessageStatus(),message.getDetailInfo(),"Send");

                                StringBuffer sBuffer = new StringBuffer(15);
                                sBuffer.append("GID:").append(GID).append("\n").append("TimeStamp:").append(timeStamp).append("\n").append("Message:").append(message.getText());
                                System.out.println(sBuffer.toString());


                                timeStamp = timeStamp.replace(" ", "");
                                String fileName="SendBroadcastMessages"+timeStamp+".csv";
                                File saveFilePath = new File (f1, fileName);

                                try {
                                    FileOutputStream os = new FileOutputStream(saveFilePath);
                                    String data = String.valueOf(sBuffer);
                                    os.write(data.getBytes());
                                    os.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }

                        }
                        else
                        {
                            message.setMessageStatus(Message.MessageStatus.ERROR_SENDING);
                        }

                  //      updateMessagingUI();
                    }
                };

                final GTErrorListener errorListener = new GTErrorListener()
                {
                    @Override
                    public void onError(GTError error)
                    {
                        if (error.getCode() == GTError.DATA_RATE_LIMIT_EXCEEDED)
                        {
                            Log.w(LOG_TAG, String.format("Data rate limit was exceeded. Resending message in %d seconds", MESSAGE_RESEND_DELAY_MILLISECONDS / Utils.MILLISECONDS_PER_SECOND));
                            final GTErrorListener localErrorListener = this;

                            // The goTenna SDK only allows you to send out so many messages within a 1 minute window.
                            // Try resending the message again later.
                            messageResendHandler.postDelayed(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    Log.i(LOG_TAG, "Resending message after data limit was exceeded");
                                    sendMessage(message, isBroadcast, responseListener, localErrorListener);
                                }
                            }, MESSAGE_RESEND_DELAY_MILLISECONDS);
                        }
                        else
                        {
                            message.setMessageStatus(Message.MessageStatus.ERROR_SENDING);
                          //  updateMessagingUI();

                            Log.w(LOG_TAG, error.toString());
                        }
                    }
                };

                // Actually send the message out
                sendMessage(message, isBroadcast, responseListener, errorListener);

                return true;
            }
            else
            {
                Toast toast = Toast.makeText(getApplicationContext(), R.string.gotenna_disconnected, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();

                return false;
            }
        }

        Toast toast = Toast.makeText(getApplicationContext(), R.string.error_occurred, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

        return false;
    }
    private void sendMessage(final Message message,
                             final boolean isBroadcast,
                             final GTCommand.GTCommandResponseListener responseListener,
                             final GTErrorListener errorListener)
    {
        // This is where we use the SDK to actually send the message out
        if (isBroadcast)
        {
            GTCommandCenter.getInstance().sendBroadcastMessage(message.toBytes(), responseListener, errorListener);
        }
        else
        {
            GTCommandCenter.getInstance().sendMessage(message.toBytes(),
                    message.getReceiverGID(),
                    responseListener,
                    errorListener,
                    willEncryptMessages);
        }
    }
}
