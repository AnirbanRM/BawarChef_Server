package com.bawarchef.Broadcast;

import java.util.ArrayList;
import java.util.Iterator;

public class BroadcastCircle {

    String circleID;
    ArrayList<BroadcastItem> broadcastItems;

    BroadcastCircle(String circleID){
        this.circleID = circleID;
        broadcastItems = new ArrayList<BroadcastItem>();
    }

    public String newBroadcast(String userID, String message){
        BroadcastItem broadcastItem = new BroadcastItem(userID,message);
        broadcastItems.add(broadcastItem);
        return broadcastItem.id;
    }

    public void addBroadcastReply(String id, String chefID, String name, String message){
        for(BroadcastItem broadcastItem : broadcastItems)
            if(broadcastItem.id.equals(id))
                broadcastItem.addReply(chefID,name,message);
    }

    public void removeOld(){
        long currentSecond = System.currentTimeMillis()/1000;
        broadcastItems.removeIf(item -> currentSecond - item.timestamp > 15 * 60);
    }

}
