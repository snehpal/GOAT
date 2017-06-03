package com.gotenna.sdk.sample.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gotenna.sdk.sample.R;
import com.gotenna.sdk.sample.models.Message;
import com.gotenna.sdk.user.User;
import com.gotenna.sdk.user.UserDataStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class GroupMessageActivity extends MessageActivity
{

    // ================================================================================
    // Class Properties
    // ================================================================================

    private TextView selectedGroupTextView;
    private Button selectGroupButton;

    private long selectedGroupGID;

    // ================================================================================
    // Life-Cycle Methods
    // ================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_message);

        selectedGroupTextView = (TextView) findViewById(R.id.selectedGroupTextView);
        selectGroupButton = (Button) findViewById(R.id.selectGroupButton);

        // We want to encrypt group messages
        willEncryptMessages = true;
        willDisplayMessageStatus = false;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        updateGroupSelectionUI();
    }

    // ================================================================================
    // Button Click Methods
    // ================================================================================

    public void onSelectGroupButtonClicked(View v)
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

        // Make sure there are groups to choose from, otherwise show the create group activity
        final ArrayList<Long> groupGIDs = currentUser.getGroupGIDs();

        if (groupGIDs.size() == 0)
        {
            Toast toast = Toast.makeText(getApplicationContext(), R.string.no_groups_toast_text, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();

            Intent intent = new Intent(this, CreateGroupActivity.class);
            startActivity(intent);
            return;
        }

        // Show a dialog that allows the user to choose which group they want to talk to
        CharSequence[] choicesArray = new CharSequence[groupGIDs.size()];

        for (int i = 0; i < groupGIDs.size(); i++)
        {
            Long groupGID = groupGIDs.get(i);
            choicesArray[i] = groupGID.toString();
        }

        // Build and show the alert
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_group_dialog_title);

        builder.setSingleChoiceItems(choicesArray, -1, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // The user selected a group to display
                selectedGroupGID = groupGIDs.get(which);
                updateGroupSelectionUI();

                dialog.cancel();
            }
        });

        builder.show();
    }

    public void onSendMessageButtonClicked(View v)
    {
        if (selectedGroupGID == 0)
        {
            Toast toast = Toast.makeText(getApplicationContext(), R.string.invalid_group_toast_text, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        attemptToSendGroupMessage(selectedGroupGID);
    }

    // ================================================================================
    // Class Instance Methods
    // ================================================================================

    private void updateGroupSelectionUI()
    {
        if (selectedGroupGID == 0)
        {
            selectedGroupTextView.setVisibility(View.GONE);
            selectGroupButton.setVisibility(View.VISIBLE);
        }
        else
        {
            selectedGroupTextView.setText(getString(R.string.selected_group_text, selectedGroupGID));
            selectedGroupTextView.setVisibility(View.VISIBLE);
            selectGroupButton.setVisibility(View.GONE);
        }
    }

    // ================================================================================
    // IncomingMessageListener Implementation
    // ================================================================================

    @Override
    public void onIncomingMessage(Message incomingMessage)
    {
        // Only display messages sent by the contact we selected as the receiver
        if (incomingMessage.getReceiverGID() == selectedGroupGID)
        {
            setToDirectory(incomingMessage);  //save messages to directory

            messagesList.add(incomingMessage);
            updateMessagingUI();
        }
    }


    private void setToDirectory(Message incomingMessage) {

        String folder_main = "Group Messages";

        File f = new File(Environment.getExternalStorageDirectory(), folder_main);   //create an external storage directory
        if (!f.exists()) {
            f.mkdirs();
        }

        File f1 = new File(Environment.getExternalStorageDirectory() + "/" + folder_main, "Receive Messages");  //create a folder inside folder_main
        if (!f1.exists()) {
            f1.mkdirs();
        }

        User currentUser = UserDataStore.getInstance().getCurrentUser();
        String GID= String.valueOf(currentUser.getGID());

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeStamp = dateFormat.format(new Date()); // Find todays date

        StringBuffer sBuffer = new StringBuffer(15);
        sBuffer.append("Sender GID:").append(incomingMessage.getSenderGID()).append("GID:").append(GID).append("\n").append("TimeStamp:").append(timeStamp).append("\n").append("Message:").append(incomingMessage.getText());
        System.out.println(sBuffer.toString());


        timeStamp = timeStamp.replace(" ", "");
        String fileName="ReceiveMessages"+timeStamp+".csv";  //save file in csv format
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
