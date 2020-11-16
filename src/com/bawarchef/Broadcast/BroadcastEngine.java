package com.bawarchef.Broadcast;

import java.util.ArrayList;
import java.util.TreeMap;

public class BroadcastEngine {

    static BroadcastEngine bcastEngine = new BroadcastEngine();
    private TreeMap<String,BroadcastCircle> circles = new TreeMap<String,BroadcastCircle>();

    {
        activateDeletion();
    }

    private BroadcastEngine(){
    }

    public static BroadcastEngine getInstance() {
        return bcastEngine;
    }

    public void activateDeletion(){
        System.out.println("Broadcast Deletions activated");
        new Thread(() -> {
            while(true){
                System.out.println("Running Broadcast cleanup");
                for(String s : circles.keySet())
                    circles.get(s).removeOld();

                try {
                    Thread.sleep(5 * 60 * 1000);
                }catch (Exception e){}
            }
        }).start();
    }

    public ArrayList<BroadcastItem> getBroadcasts(String circleID){
        if(circles.get(circleID)==null)
            return new ArrayList<BroadcastItem>();
        else
            return circles.get(circleID).broadcastItems;
    }

    public String newBroadcast(String circleID, String userID, String message){
        if(!circles.containsKey(circleID))
            circles.put(circleID,new BroadcastCircle(circleID));

        return circles.get(circleID).newBroadcast(userID,message);
    }

    public ArrayList<BroadcastItem> getBroadcasts(String circleID, String userID){
        if(circles.get(circleID)==null)
            return new ArrayList<BroadcastItem>();
        else {
            ArrayList<BroadcastItem> items = new ArrayList<BroadcastItem>();
            for(BroadcastItem bitem : circles.get(circleID).broadcastItems)
                if(bitem.userID.equals(userID))
                    items.add(bitem);
            return items;
        }
    }

    public void newReply(String circleID, String id, String chefID, String chefName, String message){
        if(circles.get(circleID)==null)
            return;
        else {
            circles.get(circleID).addBroadcastReply(id,chefID,chefName,message);
        }
    }
}
