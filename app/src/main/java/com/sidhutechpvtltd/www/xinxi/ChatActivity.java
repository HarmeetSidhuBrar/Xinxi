package com.sidhutechpvtltd.www.xinxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.sidhutechpvtltd.www.xinxi.Chat.ChatObject;
import com.sidhutechpvtltd.www.xinxi.Chat.MediaAdapter;
import com.sidhutechpvtltd.www.xinxi.Chat.MessageAdapter;
import com.sidhutechpvtltd.www.xinxi.Chat.MessageObject;
import com.sidhutechpvtltd.www.xinxi.User.UserObject;
import com.sidhutechpvtltd.www.xinxi.utils.CountryToPhonePrefix;
import com.sidhutechpvtltd.www.xinxi.utils.SendNotification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView mChat , mMedia;
    private RecyclerView.Adapter mChatAdapter , mMediaAdapter;
    private RecyclerView.LayoutManager mChatLayoutManager , mMediaLayoutManager;

    ArrayList<MessageObject> messageList;

    ChatObject mChatObject;

    DatabaseReference mChatMessageDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mChatObject = (ChatObject) getIntent().getSerializableExtra("chatObject");

        mChatMessageDb = FirebaseDatabase.getInstance().getReference().child("chat").child(mChatObject.getUId()).child("messages");

        Button mSend = findViewById(R.id.send);
        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { sendMessage(); }});

        Button mAddMedia = findViewById(R.id.addMedia);
        mAddMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        initializeMessage();
        initializeMedia();
        getChatMessages();
    }

    private void getChatMessages() {
        mChatMessageDb.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if (dataSnapshot.exists()){
                    String text = "";
                    String  creatorID = "" ;
                    ArrayList<String> mediaUrlList = new ArrayList<>();

                    if (dataSnapshot.child("text").getValue() != null){
                        text = dataSnapshot.child("text").getValue().toString();
                    }
                    if (dataSnapshot.child("creator").getValue() != null){
                        creatorID = dataSnapshot.child("creator").getValue().toString();
                    }

                    if (dataSnapshot.child("media").getChildrenCount()>0)
                        for (DataSnapshot mediaSnapshot : dataSnapshot.child("media").getChildren())
                            mediaUrlList.add(mediaSnapshot.getValue().toString());

                    MessageObject mMessage = new MessageObject(dataSnapshot.getKey(),creatorID,text,mediaUrlList);
                    messageList.add(mMessage);
                    mChatLayoutManager.scrollToPosition(messageList.size()-1);
                    mChatAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    int totalMediaUploaded = 0;
    ArrayList<String> mediaIdList = new ArrayList<>();
    EditText mMessage ;
    private void sendMessage(){
        mMessage = findViewById(R.id.message2);

            String messageId = mChatMessageDb.push().getKey();
            final DatabaseReference newMessageDb = mChatMessageDb.child(messageId);

            final Map newMessageMap = new HashMap<>();

            newMessageMap.put("creator", FirebaseAuth.getInstance().getUid());

            if (!mMessage.getText().toString().isEmpty())
                newMessageMap.put("text",mMessage.getText().toString());


            if (!mediaUriList.isEmpty()){
                for (String mediaUri : mediaUriList){
                    String mediaId = newMessageDb.child("media").push().getKey();
                    mediaIdList.add(mediaId);
                    final StorageReference filePath = FirebaseStorage.getInstance().getReference().child("chat").child(mChatObject.getUId()).child(messageId).child(mediaId);

                    UploadTask uploadTask = filePath.putFile(Uri.parse(mediaUri));

                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    newMessageMap.put("/media/" + mediaIdList.get(totalMediaUploaded) + "/",uri.toString());

                                    totalMediaUploaded++;

                                    if (totalMediaUploaded==mediaUriList.size()){
                                        updateDatabaseWithNewMessage(newMessageDb , newMessageMap);
                                    }

                                }
                            });
                        }
                    });
                }
            }else{
                if(!mMessage.getText().toString().isEmpty())
                    updateDatabaseWithNewMessage(newMessageDb , newMessageMap);
            }
    }

    private void updateDatabaseWithNewMessage(DatabaseReference newMessageDb , Map newMessageMap){
        newMessageDb.updateChildren(newMessageMap);

        mMessage.setText(null);
        mediaUriList.clear();
        mediaIdList.clear();
        mMediaAdapter.notifyDataSetChanged();

        String message;
        if (newMessageMap.get("text") != null)
            message = newMessageMap.get("text").toString();
        else
            message = "Sent Media";

        for (UserObject mUser : mChatObject.getUserObjectArrayList()){
            if (!mUser.getUid().equals(FirebaseAuth.getInstance().getUid())){
                new SendNotification(message , "New Message" , mUser.getNotificationKey());
            }
        }
    }

    private void initializeMessage() {
        messageList = new ArrayList<>();
        mChat = findViewById(R.id.messageList);
        mChat.setNestedScrollingEnabled(false);
        mChat.setHasFixedSize(false);
        mChatLayoutManager = new LinearLayoutManager(getApplicationContext(), RecyclerView.VERTICAL,false);
        mChat.setLayoutManager(mChatLayoutManager);
        mChatAdapter = new MessageAdapter(messageList);
        mChat.setAdapter(mChatAdapter);
    }

    int PICK_IMAGE_INTENT = 1 ;
    ArrayList<String> mediaUriList = new ArrayList<>();
    private void initializeMedia() {
        mediaUriList = new ArrayList<>();
        mMedia = findViewById(R.id.mediaList);
        mMedia.setNestedScrollingEnabled(false);
        mMedia.setHasFixedSize(false);
        mMediaLayoutManager = new LinearLayoutManager(getApplicationContext(),LinearLayoutManager.HORIZONTAL,false);
        mMedia.setLayoutManager(mMediaLayoutManager);
        mMediaAdapter = new MediaAdapter(getApplicationContext(),mediaUriList);
        mMedia.setAdapter(mMediaAdapter);
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Picture(s)"),PICK_IMAGE_INTENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK){
            if (requestCode==PICK_IMAGE_INTENT){
                if (data.getClipData()==null) {
                    mediaUriList.add(data.getData().toString());
                }else {
                    for (int i = 0 ; i < data.getClipData().getItemCount(); i++){
                        mediaUriList.add(data.getClipData().getItemAt(i).getUri().toString());
                    }
                }
                mMediaAdapter.notifyDataSetChanged();
            }
        }
    }



    private String getCountryIso(){
        String iso = null;

        TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(getApplicationContext().TELEPHONY_SERVICE);
        if (telephonyManager.getNetworkCountryIso()!=null){
            if (!telephonyManager.getNetworkCountryIso().equals("")){
                iso = telephonyManager.getNetworkCountryIso();
            }
        }

        return  CountryToPhonePrefix.getPhone(iso);
    }
}
