package com.sidhutechpvtltd.www.xinxi.Chat;

import com.sidhutechpvtltd.www.xinxi.User.UserObject;

import java.io.Serializable;
import java.util.ArrayList;

public class ChatObject implements Serializable {

    String uId;

    private ArrayList<UserObject> userObjectArrayList = new ArrayList<>();

    public ChatObject(String uId){
       this.uId = uId;
    }

    public String getUId() {return uId;}


    public void setName(String phone) { this.uId = phone; }

    public ArrayList<UserObject> getUserObjectArrayList() {
        return userObjectArrayList;
    }

    public void addUserToArrayList (UserObject mUser){userObjectArrayList.add(mUser);}
}
