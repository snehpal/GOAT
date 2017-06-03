package com.gotenna.sdk.sample.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.gotenna.sdk.gids.GIDManager;
import com.gotenna.sdk.sample.R;
import com.gotenna.sdk.sample.Utils.GatLatitudeAndLongitude;
import com.gotenna.sdk.sample.Utils.UserDBUtil;
import com.gotenna.sdk.sample.Utils.Utility;
import com.gotenna.sdk.sample.models.Message;
import com.gotenna.sdk.user.User;
import com.gotenna.sdk.user.UserDataStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class BroadcastMessageActivity extends MessageActivity
{
    // ================================================================================
    // Life-cycle Methods
    // ================================================================================
    UserDBUtil userDb;
    Utility util;
    public ArrayList<Message> broadCastList = new ArrayList<Message>();
    ArrayList<String> latlng;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast_message);

        // A broadcast cannot be encrypted since it is sent to everyone
        // who is listening on the broadcast channel
        willEncryptMessages = false;
        willDisplayMessageStatus = false;

        util = new Utility();
        util.copyUserDataBase(getApplicationContext());
        userDb = new UserDBUtil(getApplicationContext());


    }

    // ================================================================================
    // Button Click Methods
    // ================================================================================

    public void onSendMessageButtonClicked(View v)
    {
        attemptToSendBroadcastMessage();
    }

    // ================================================================================
    // IncomingMessageListener Implementation
    // ================================================================================

    @Override
    public void onIncomingMessage(Message incomingMessage)
    {
        // Only display messages sent to the broadcasts/shouts channel
        if (incomingMessage.getReceiverGID() == GIDManager.SHOUT_GID)
        {
            setToDirectory(incomingMessage); //save messages to directory
            messagesList.add(incomingMessage);

            updateMessagingUI();

            String text = incomingMessage.getText().toString();


            Boolean b=  Patterns.WEB_URL.matcher(text).matches();  //check weather the message is text or web link
              long ReceivedGID=  incomingMessage.getSenderGID();
            if (b){
                GatLatitudeAndLongitude getLatitudeAndLongitude=new GatLatitudeAndLongitude();
                getLatitudeAndLongitude.getLatitudeAndLongitudeCoordinates(BroadcastMessageActivity.this,text,ReceivedGID);
               // getLatitudeAndLongitudeCoordinates(text);
            }

              //if received text is polygon
            try{
                String[] items = text.split(System.getProperty("line.separator")); //split text with line seperator
                    String first=items[0];  //get first item from list

                    if (first.equals("Received Polygon: ")){     //check  first string is received polygon
                        Intent in=new Intent(BroadcastMessageActivity.this,MapPolygon.class);
                        in.putExtra("latlng",text);
                        startActivity(in);
                    }

            }catch (Exception e){
               Log.e("Exception ",e.toString());
            }
        }
    }


    private void setToDirectory(Message incomingMessage) {

        String folder_main = "Broadcast Messsages";

        File f = new File(Environment.getExternalStorageDirectory(), folder_main);  //create an external storage directory
        if (!f.exists()) {
            f.mkdirs();
        }
        File f1 = new File(Environment.getExternalStorageDirectory() + "/" + folder_main, "Receive Messages"); //create a folder inside folder_main
        if (!f1.exists()) {
            f1.mkdirs();
        }

        User currentUser = UserDataStore.getInstance().getCurrentUser();
        String GID= String.valueOf(currentUser.getGID());

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeStamp = dateFormat.format(new Date()); // Find todays date

        StringBuffer sBuffer = new StringBuffer(15);
        sBuffer.append("Sender GID:").append(incomingMessage.getSenderGID()).append("\n").append("GID:").append(GID).append("\n").append("TimeStamp:").append(timeStamp).append("\n").append("Message:").append(incomingMessage.getText());
        System.out.println(sBuffer.toString());


        timeStamp = timeStamp.replace(" ", "");
        String fileName="ReceiveMessages"+timeStamp+".csv";   //save file in csv format
        File saveFilePath = new File (f1, fileName);           //create file inside folder f1

        try {
            FileOutputStream os = new FileOutputStream(saveFilePath);
            String data = String.valueOf(sBuffer);
            os.write(data.getBytes());
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }
}
