package com.gotenna.sdk.sample.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.gotenna.sdk.sample.R;
import com.gotenna.sdk.sample.Utils.GatLatitudeAndLongitude;
import com.gotenna.sdk.sample.managers.ContactsManager;
import com.gotenna.sdk.sample.models.Contact;
import com.gotenna.sdk.sample.models.Message;
import com.gotenna.sdk.user.User;
import com.gotenna.sdk.user.UserDataStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PrivateMessageActivity extends MessageActivity
{
    // ================================================================================
    // Class Properties
    // ================================================================================

    private TextView receiverTextView;
    private Contact receiverContact;
    String send;
    // ================================================================================
    // Life-Cycle Methods
    // ================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_message);

        receiverTextView = (TextView) findViewById(R.id.receiverTextView);

        // We want to encrypt private messages
        willEncryptMessages = true;
        willDisplayMessageStatus = true;

        Intent in =getIntent();
        send= in.getStringExtra("send");  //get intent
    }

    @Override
    public void onResume()
    {
        super.onResume();
        refreshReceiverUI();
    }

    // ================================================================================
    // Button Click Methods
    // ================================================================================

    public void onChooseReceiverButtonClicked(View v)
    {
        // Show a dialog that allows the user to choose which pre-defined Contact they want to talk to.
        final List<Contact> contacts = ContactsManager.getInstance().getDemoContactsExcludingSelf();
        CharSequence[] choicesArray = new CharSequence[contacts.size()];

        for (int i = 0; i < contacts.size(); i++)
        {
            Contact contact = contacts.get(i);
            choicesArray[i] = String.format("%s - %d", contact.getName(), contact.getGid());
        }

        // Build and show the alert
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_user_dialog_title);

        builder.setSingleChoiceItems(choicesArray, -1, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                receiverContact = contacts.get(which);
                refreshReceiverUI();

                dialog.cancel();
            }
        });

        builder.show();
    }

    public void onSendMessageButtonClicked(View v)
    {
        if (receiverContact == null)
        {
            Toast toast = Toast.makeText(getApplicationContext(), R.string.invalid_receiver_toast_text, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

       if (send!=null){
           if (send.equals("coodinates")){   //check user want to send coordinates or text
               attemptToSendPrivateCoordinates(receiverContact.getGid());
           }else {
               attemptToSendPrivateMessage(receiverContact.getGid());
           }
       }else {
           attemptToSendPrivateMessage(receiverContact.getGid());
       }


    }

    // ================================================================================
    // Class Instance Methods
    // ================================================================================

    private void refreshReceiverUI()
    {
        String receiverName = receiverContact == null ? getString(R.string.none) : receiverContact.getName();
        receiverTextView.setText(getString(R.string.receiver_text, receiverName));
    }

    // ================================================================================
    // IncomingMessageListener Implementation
    // ================================================================================

    @Override
    public void onIncomingMessage(Message incomingMessage)
    {
        User currentUser = UserDataStore.getInstance().getCurrentUser();

        // Only display messages sent by the contact we selected as the receiver
        // and should be received directly by us
        if (receiverContact != null &&
                currentUser != null &&
                incomingMessage.getSenderGID() == receiverContact.getGid() &&
                incomingMessage.getReceiverGID() == currentUser.getGID())
        {

            setToDirectory(incomingMessage);  //save messages to directory

            messagesList.add(incomingMessage);
            updateMessagingUI();

            String text = incomingMessage.getText().toString();
            Boolean b=  Patterns.WEB_URL.matcher(text).matches();  //check weather the message is text or web link
            long ReceivedGID=  incomingMessage.getSenderGID();
            if (b){

                GatLatitudeAndLongitude getLatitudeAndLongitude=new GatLatitudeAndLongitude();
                getLatitudeAndLongitude.getLatitudeAndLongitudeCoordinates(PrivateMessageActivity.this,text,ReceivedGID);

            }

        }
    }


    private void setToDirectory(Message incomingMessage) {

        String folder_main = "Private Messages";

        File f = new File(Environment.getExternalStorageDirectory(), folder_main); //create an external storage directory
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

        StringBuffer sBuffer = new StringBuffer(15);  //create string buffer to store string and append multiple string
        sBuffer.append("Sender GID:").append(incomingMessage.getSenderGID()).append("GID:").append(GID).append("\n").append("TimeStamp:").append(timeStamp).append("\n").append("Message:").append(incomingMessage.getText());
        System.out.println(sBuffer.toString());


        timeStamp = timeStamp.replace(" ", "");
        String fileName="ReceiveMessages"+timeStamp+".csv"; //save file in csv format
        File saveFilePath = new File (f1, fileName);   //create file inside folder f1

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
