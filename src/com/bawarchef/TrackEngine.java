package com.bawarchef;

import java.io.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;

public class TrackEngine {

    public static class LatLngPair{
        double latitude,longitude;
        LatLngPair(double latitude, double longitude){
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude(){
            return latitude;
        }

        public double getLongitude(){
            return longitude;
        }

        public String toString(){
            return  "["+String.valueOf(latitude)+","+String.valueOf(longitude)+"]";
        }
    }

    private HashMap<String,LatLngPair> mapper = new HashMap<String, LatLngPair>();
    private static TrackEngine trackEngine = new TrackEngine();

    private TrackEngine(){
    }

    public static TrackEngine getInstance(){
        return trackEngine;
    }

    public int getEntryCount(){
        return mapper.size();
    }

    public void showBinding(){
        System.out.println(mapper.toString());
    }

    public void updateLocation(String identifier,LatLngPair latLngPair){
        System.out.println("LOCATION UPDATE: "+identifier+" : "+latLngPair.toString());
        mapper.put(identifier,latLngPair);
    }

    public void updateLocation(String identifier,double latitude, double longitude){
        updateLocation(identifier,new LatLngPair(latitude, longitude));
    }

    public LatLngPair getLocation(String identifier){
        if(mapper.containsKey(identifier))
            return mapper.get(identifier);
        else
            return null;
    }






}
