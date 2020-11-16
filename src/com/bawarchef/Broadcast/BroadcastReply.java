package com.bawarchef.Broadcast;

public class BroadcastReply {
    public String chefID;
    public String chefName;
    public String message;

    BroadcastReply(String chefID, String chefName, String message){
        this.chefID = chefID;
        this.message = message;
        this.chefName = chefName;
    }
}
