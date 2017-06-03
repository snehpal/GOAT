package com.gotenna.sdk.sample.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.gotenna.sdk.bluetooth.GTConnectionManager;
import com.gotenna.sdk.commands.GTCommand.GTCommandResponseListener;
import com.gotenna.sdk.commands.GTCommandCenter;
import com.gotenna.sdk.commands.GTError;
import com.gotenna.sdk.gids.GIDManager;
import com.gotenna.sdk.interfaces.GTErrorListener;
import com.gotenna.sdk.responses.GTResponse;
import com.gotenna.sdk.sample.R;
import com.gotenna.sdk.sample.Utils.SampleSchedulingService;
import com.gotenna.sdk.sample.Utils.UserDBUtil;
import com.gotenna.sdk.sample.Utils.Utility;
import com.gotenna.sdk.sample.adapters.MessagesArrayAdapter;
import com.gotenna.sdk.sample.managers.IncomingMessagesManager;
import com.gotenna.sdk.sample.managers.IncomingMessagesManager.IncomingMessageListener;
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

/**
 * An abstract activity that contains most of the logic for sending out messages
 * and updating the necessary UI.
 *
 * Created on 2/10/16
 *
 * @author ThomasColligan
 */
public abstract class MessageActivity extends ChildActivity implements IncomingMessageListener, AdapterView.OnItemClickListener {
    // ================================================================================
    // Class Properties
    // ================================================================================

    private static final int MESSAGE_RESEND_DELAY_MILLISECONDS = 5000;
    private static final String LOG_TAG = "MessageActivity";

    protected boolean willEncryptMessages;
    protected boolean willDisplayMessageStatus;
    protected Handler messageResendHandler;
    protected ArrayList<Message> messagesList;
    protected MessagesArrayAdapter messagesArrayAdapter;

    protected ListView messagesListView;
    protected EditText sendMessageEditText;

    public String messageStatus;

    // ================================================================================
    // Overridden Activity Methods
    // ================================================================================

    @Override
    protected void onPostCreate (Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);

        messageResendHandler = new Handler();
        messagesList = new ArrayList<>();

        messagesListView = (ListView) findViewById(R.id.messagesListView);
        messagesListView.setOnItemClickListener(this);   //apply onclick on the listview of messages
        sendMessageEditText = (EditText) findViewById(R.id.sendMessageEditText);


    }

    @Override
    public void onResume()
    {
        super.onResume();
        updateMessagingUI();
        IncomingMessagesManager.getInstance().addIncomingMessageListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        IncomingMessagesManager.getInstance().removeIncomingMessageListener(this);
    }

    // ================================================================================
    // Class Instance Methods
    // ================================================================================

    protected void updateMessagingUI()
    {
        if (messagesArrayAdapter == null)
        {
            messagesArrayAdapter = new MessagesArrayAdapter(this, messagesList);
            messagesArrayAdapter.setWillDisplayMessageStatus(willDisplayMessageStatus);
            messagesListView.setAdapter(messagesArrayAdapter);
        }
        else
        {
            messagesArrayAdapter.notifyDataSetChanged();
        }
    }


    private void getLatitudeAndLongitude(String s) {

        //add code in try catch because there is a possibility that web link does not have latitude and longitude so if exception occurs
        //then try catch will handle that

        try {
            String[] LatLong = s.split("="); //split the link to get latitude and longitude
            String  latlong= LatLong[1];
            Log.e("LatLong ",latlong);

            if (latlong!=null){
                Toast.makeText(MessageActivity.this,"Open Map",Toast.LENGTH_LONG).show();

                Intent in=new Intent(MessageActivity.this,MapActivity.class); //open map activity
                in.putExtra("LatLang",latlong); //pass latitude and longitude to map activityy
                startActivity(in);     //activity starts
               // finish();
            }else {
                Toast.makeText(MessageActivity.this,"Latitude and Longitude is null",Toast.LENGTH_LONG).show();
            }

        }catch (Exception e){
            Log.e("Exception",e.toString());
        }

    }

    public void attemptToSendPrivateMessage(long receiverGID)
    {
        messageStatus="PrivateMessage";  //set message status to find which type of message send
        attemptToSendMessage(receiverGID, false);

    }

    public void attemptToSendPrivateCoordinates(long receiverGID)  //send Private coordinates
    {
        messageStatus="PrivateCoordinate";  //set message status to find which type of message send
        attemptToSendCoordinate(receiverGID, false);

    }

    public void attemptToSendBroadcastMessage()
    {
        messageStatus="BroadCastMessage"; //set message status to find which type of message send
        attemptToSendMessage(GIDManager.SHOUT_GID, true);

    }


    public void attemptToSendBroadcastPolygon(ArrayList<LatLng> arrayPoints)
    {
        messageStatus="BroadCastMessage"; //set message status to find which type of message send
        attemptToSendPolygon(GIDManager.SHOUT_GID, true,arrayPoints);

    }

    public void attemptToSendGroupMessage(long groupGID)
    {
        messageStatus="GroupMessage"; //set message status to find which type of message send
        attemptToSendMessage(groupGID, false);

    }



    private void attemptToSendCoordinate(long receiverGID, boolean isBroadcast)
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

        String uri = "https://maps.google.com/?q=" + SampleSchedulingService.latitude+","+SampleSchedulingService.longitude;
        StringBuffer smsBody = new StringBuffer();
        smsBody.append(Uri.parse(uri));
        String messageText = smsBody.toString();

        if (messageText.length() == 0)
        {
            return;
        }

        Message messageToSend = Message.createReadyToSendMessage(currentUser.getGID(), receiverGID, messageText);
        boolean didSend = sendMessage(messageToSend, isBroadcast);

        if (didSend)
        {

            sendMessageEditText.setText("");
            messagesList.add(messageToSend);
            updateMessagingUI();

            Toast.makeText(MessageActivity.this,"Message Send Successfully",Toast.LENGTH_LONG).show();
        }
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

        StringBuilder messages = new StringBuilder();
        messages.append("Received Polygon");
        for (int i=0;i<arrayPoints.size();i++){

            messages.append(String.valueOf(arrayPoints.get(i).latitude));
            messages.append(" ");
            messages.append(String.valueOf(arrayPoints.get(i).longitude));
            messages.append(System.getProperty("line.separator"));
        }
       String messageText = String.valueOf(messages);

        if (arrayPoints.size() < 2)
        {
            return;
        }else{
            Toast.makeText(MessageActivity.this,"Select a Polygon",Toast.LENGTH_LONG).show();
        }

        Message messageToSend = Message.createReadyToSendMessage(currentUser.getGID(), receiverGID, messageText);
        boolean didSend = sendMessage(messageToSend, isBroadcast);

        if (didSend)
        {
         //   sendMessageEditText.setText("");
            messagesList.add(messageToSend);
            updateMessagingUI();
        }
    }

    private void attemptToSendMessage(long receiverGID, boolean isBroadcast)
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

        String messageText = sendMessageEditText.getText().toString();

        if (messageText.length() == 0)
        {
            return;
        }

        Message messageToSend = Message.createReadyToSendMessage(currentUser.getGID(), receiverGID, messageText);
        boolean didSend = sendMessage(messageToSend, isBroadcast);

        if (didSend)
        {
            sendMessageEditText.setText("");
            messagesList.add(messageToSend);
            updateMessagingUI();
        }
    }

    private boolean sendMessage(final Message message, final boolean isBroadcast)
    {
        if (message != null && message.toBytes() != null)
        {
            if (GTConnectionManager.getInstance().isConnected())
            {
                final GTCommandResponseListener responseListener = new GTCommandResponseListener()
                {
                    @Override
                    public void onResponse(GTResponse response)
                    {
                        // Parse the response we got about whether our message got through successfully
                        if (response.getResponseCode() == GTDataTypes.GTCommandResponseCode.POSITIVE)
                        {
                            message.setMessageStatus(Message.MessageStatus.SENT_SUCCESSFULLY);
                                //get message status and save message to directory according to which type of message send
                            if (messageStatus.equals("PrivateMessage")){

                                String folder_main = "Private Messages";

                                File f = new File(Environment.getExternalStorageDirectory(), folder_main);  //create an external storage directory
                                if (!f.exists()) {
                                    f.mkdirs();
                                }

                                     File f1 = new File(Environment.getExternalStorageDirectory() + "/" + folder_main, "Send Messages"); //create a folder inside folder_main
                                      if (!f1.exists()) {
                                      f1.mkdirs();
                                     }

                                User currentUser = UserDataStore.getInstance().getCurrentUser();
                                String GID= String.valueOf(currentUser.getGID());

                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                String timeStamp = dateFormat.format(new Date()); // Find todays date

                                StringBuffer sBuffer = new StringBuffer(15);
                                sBuffer.append("GID:").append(GID).append("\n").append("TimeStamp:").append(timeStamp).append("\n").append("Message:").append(message.getText());
                                System.out.println(sBuffer.toString());


                                timeStamp = timeStamp.replace(" ", "");
                                String fileName="SendPrivateMessages"+timeStamp+".csv";  //save file in csv format
                                File saveFilePath = new File (f1, fileName); //create file inside folder f1

                                try {
                                    FileOutputStream os = new FileOutputStream(saveFilePath);
                                    String data = String.valueOf(sBuffer);
                                    os.write(data.getBytes());
                                    os.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }else if (messageStatus.equals("PrivateCoordinate")){
                                String folder_main = "Private Messages";

                                File f = new File(Environment.getExternalStorageDirectory(), folder_main);
                                if (!f.exists()) {
                                    f.mkdirs();
                                }
                                User currentUser = UserDataStore.getInstance().getCurrentUser();
                                String GID= String.valueOf(currentUser.getGID());

                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                String timeStamp = dateFormat.format(new Date()); // Find todays date

                                StringBuffer sBuffer = new StringBuffer(15);
                                sBuffer.append("GID:").append(GID).append("\n").append("TimeStamp:").append(timeStamp).append("\n").append("Message:").append(message.getText());
                                System.out.println(sBuffer.toString());


                                timeStamp = timeStamp.replace(" ", "");
                                String fileName="SendPrivateCoordinates"+timeStamp+".csv";
                                File saveFilePath = new File (f, fileName);

                                try {
                                    FileOutputStream os = new FileOutputStream(saveFilePath);
                                    String data = String.valueOf(sBuffer);
                                    os.write(data.getBytes());
                                    os.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }else if (messageStatus.equals("BroadCastMessage")){
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

                            }else if (messageStatus.equals("GroupMessage")){
                                String folder_main = "Group Messages";

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

                                StringBuffer sBuffer = new StringBuffer(15);
                                sBuffer.append("GID:").append(GID).append("\n").append("TimeStamp:").append(timeStamp).append("\n").append("Message:").append(message.getText());
                                System.out.println(sBuffer.toString());


                                timeStamp = timeStamp.replace(" ", "");
                                String fileName="SendGroupMessages"+timeStamp+".csv";
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

                        updateMessagingUI();
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
                            updateMessagingUI();

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
                             final GTCommandResponseListener responseListener,
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

    @Override
    public void onIncomingMessage(Message incomingMessage) {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(MessageActivity.this,"Link Clicked",Toast.LENGTH_LONG).show();

        TextView tv = (TextView) view.findViewById(R.id.cellMessageTextView);  //find the textview from view
        String text = tv.getText().toString();  //get text from particular textview which is clicked

        Boolean b=  Patterns.WEB_URL.matcher(text).matches();  //check weather the message is text or web link
        if (b){
            Toast.makeText(MessageActivity.this,"Link Clicked, Pattern Match"+text,Toast.LENGTH_LONG).show();
            Log.e("Value ","Matches");
            getLatitudeAndLongitude(text);  //if text is web link then get latitude and longitude from link

        }else {
            Log.e("Value ","NotMatches");
            Toast.makeText(MessageActivity.this,"Link Clicked Pattern NotMatch",Toast.LENGTH_LONG).show();
        }
    }
}
