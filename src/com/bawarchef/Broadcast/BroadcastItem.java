package com.bawarchef.Broadcast;

import java.util.ArrayList;
import java.util.UUID;

public class BroadcastItem{

    public String userID;
    public String message;
    public String id;
    public long timestamp=0;

    public ArrayList<BroadcastReply> broadcast_replies;

    BroadcastItem(String userID, String message){
        this.id = UUID.randomUUID().toString();
        this.userID = userID;
        this.message = message;
        broadcast_replies = new ArrayList<BroadcastReply>();
        timestamp = System.currentTimeMillis()/1000;
    }

    public void addReply(String chefID,String chefName, String message){
        broadcast_replies.add(new BroadcastReply(chefID,chefName,message));
    }

}
