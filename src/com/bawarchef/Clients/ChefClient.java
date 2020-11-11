package com.bawarchef.Clients;

import com.bawarchef.Communication.EncryptedPayload;
import com.bawarchef.Communication.Message;
import com.bawarchef.Communication.ObjectByteCode;
import com.bawarchef.Containers.ChefIdentity;
import com.bawarchef.Containers.ChefLogin;
import com.bawarchef.Containers.GeoLocationCircle;
import com.bawarchef.Containers.ChefProfileContainer;
import com.bawarchef.DBConnect;
import com.bawarchef.TrackEngine;
import com.bawarchef.android.Hierarchy.DataStructure.Tree;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Base64;

public class ChefClient{

    Client parentClient;
    public ChefClient(Client c) {
        this.parentClient = c;
        c.setMessageProcessor(processor);
    }

    Client.MessageProcessor processor = new Client.MessageProcessor() {
        @Override
        public void process(Message m) {


            if(m.getMsg_type().equals("LOC_UPD")){
                TrackEngine te = TrackEngine.getInstance();
                te.updateLocation(parentClient.userID,(double)m.getProperty("LAT"),(double)m.getProperty("LNG"));
            }



            else if(m.getMsg_type().equals("UPD_PDET")){
                boolean result = updatePDetails(m);
                Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT,"UPD_PDET_RESP");
                if(result) new_m.putProperty("RESULT","SUCCESS");
                else new_m.putProperty("RESULT","FAILURE");
                try {
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}
            }



            else if(m.getMsg_type().equals("UPD_L_DET")){
                boolean result = updateLDetails(m);
                Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT,"UPD_L_DET_RESP");
                if(result) new_m.putProperty("RESULT","SUCCESS");
                else new_m.putProperty("RESULT","FAILURE");
                try {
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}

            }




            else if(m.getMsg_type().equals("GEOLOC_QUERY")) {
                double lat = (double) m.getProperty("LAT");
                double lng = (double) m.getProperty("LNG");

                double latmin = lat - 0.1, latmax = lat + 0.1;
                double lngmin = lng - 0.1, lngmax = lng + 0.1;

                String query = "SELECT * from circles where lat between " + latmin + " and " + latmax + " and lng between " + lngmin + " and " + lngmax + ";";
                DBConnect dbConnect = DBConnect.getInstance();
                ResultSet rs = dbConnect.runFetchQuery(query);
                ArrayList<GeoLocationCircle> geoLocationCircles = new ArrayList<GeoLocationCircle>();
                try {
                    while (rs.next()){
                        GeoLocationCircle r = new GeoLocationCircle(rs.getInt("circleID"),rs.getString("circleName"),rs.getDouble("lat"),rs.getDouble("lng"));
                        geoLocationCircles.add(r);
                    }
                } catch (Exception e) { }

                String chefID = parentClient.getUserID();

                String regCircleID=null;
                rs = dbConnect.runFetchQuery("SELECT circleID from chef_circle where chefID = '"+chefID+"';");
                try {
                    while(rs.next())
                        regCircleID = rs.getString("circleID");
                }catch (Exception e){}


                Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT,"GEOLOC_RESP");
                new_m.putProperty("NEARBY_P",geoLocationCircles);
                new_m.putProperty("REG_CIRCLE",regCircleID);

                try {
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}

            }





            else if(m.getMsg_type().equals("UPD_PROFILE_CHEF")){
                boolean success = false;

                Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT,"UPD_PROFILE_RESP");
                ChefProfileContainer chefProfileContainer = (ChefProfileContainer) m.getProperty("DATA");
                String base64DP = "";
                if(chefProfileContainer.dp!=null)
                    base64DP = Base64.getEncoder().encodeToString(chefProfileContainer.dp);
                DBConnect dbConnect = DBConnect.getInstance();

                try{
                    dbConnect.runManipulationQuery("UPDATE chef_login set loginID = '"+ chefProfileContainer.uName+"' where chefID = '"+parentClient.getUserID()+"';");
                    ResultSet rs2 = dbConnect.runFetchQuery("select chefID from chef_profile_table where chefID = '"+parentClient.getUserID()+"';");
                    while(rs2.next()) {
                        success = dbConnect.runManipulationQuery("UPDATE chef_profile_table set bio='" + chefProfileContainer.bio + "',lat=" + chefProfileContainer.resiLat + ",lng=" + chefProfileContainer.resiLng + ",dp='" + base64DP + "' where chefID = '" + parentClient.getUserID() + "';");
                    }

                    if(!success){
                        success = dbConnect.runInsertQuery("INSERT INTO chef_profile_table value('"+ parentClient.getUserID() +"','"+base64DP+"',"+ chefProfileContainer.resiLat+","+ chefProfileContainer.resiLng+",'"+ chefProfileContainer.bio+"');");
                    }
                }catch (Exception e){}

                if(success)new_m.putProperty("RESULT","SUCCESS");
                else new_m.putProperty("RESULT","FAILURE");

                try {
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}
            }





            else if(m.getMsg_type().equals("PROFILE_FETCH")){
                Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT,"PROFILE_FETCH_RESP");
                ChefProfileContainer chefProfileContainer = new ChefProfileContainer();
                DBConnect dbConnect = DBConnect.getInstance();
                ResultSet rs = dbConnect.runFetchQuery("SELECT * from chef_profile_table,chef_login where chefID = '"+parentClient.getUserID()+"' and chef_profile_table.chefID = chef_login.chefID;");

                try {
                    while (rs.next()) {
                        chefProfileContainer.resiLat = (float)rs.getDouble("lat");
                        chefProfileContainer.resiLng = (float)rs.getDouble("lng");
                        chefProfileContainer.bio = rs.getString("bio");
                        chefProfileContainer.dp = Base64.getDecoder().decode(rs.getString("dp"));
                    }
                }catch (Exception e){}

                new_m.putProperty("DATA", chefProfileContainer);
                try {
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}

            }






            else if(m.getMsg_type().equals("UPD_CHEF_MENU")){
                boolean success=false;

                Tree menu = (Tree)m.getProperty("MENU_DATA");
                String objstr=null;
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(menu);
                    oos.flush();
                    objstr = Base64.getEncoder().encodeToString(baos.toByteArray());
                }catch (Exception e){}

                DBConnect dbConnect = DBConnect.getInstance();

                ResultSet rs  = dbConnect.runFetchQuery("SELECT * from chef_menu where chefID = '"+parentClient.getUserID()+"';");
                try {
                    while (rs.next()) {
                        success = dbConnect.runManipulationQuery("UPDATE chef_menu set menuSerial = '"+objstr+"' where chefID = '"+parentClient.getUserID()+"';");
                    }

                    if(!success)
                        success = dbConnect.runInsertQuery("INSERT into chef_menu value('"+parentClient.getUserID()+"','"+objstr+"');");

                }catch (Exception e){}

                Message newm = new Message(Message.Direction.SERVER_TO_CLIENT,"UPD_MENU_RESP");
                if(success)newm.putProperty("RESULT","SUCCESS");
                else newm.putProperty("RESULT","FAILURE");

                try {
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(newm), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}
            }







            else if(m.getMsg_type().equals("FETCH_CHEF_MENU")){
                DBConnect dbConnect = DBConnect.getInstance();

                ResultSet rs = dbConnect.runFetchQuery("SELECT * from chef_menu where chefID = '"+parentClient.getUserID()+"';");

                Tree t =null;

                try {
                    while (rs.next()) {
                        byte[] bytarr = Base64.getDecoder().decode(rs.getString("menuSerial"));
                        ByteArrayInputStream bais = new ByteArrayInputStream(bytarr);
                        ObjectInputStream ois = new ObjectInputStream(bais);
                        t = (Tree) ois.readObject();
                    }
                }catch (Exception e){e.printStackTrace();}

                Message newm = new Message(Message.Direction.SERVER_TO_CLIENT,"RESP_CHEF_MENU");
                newm.putProperty("MENU_TREE",t);

                try {
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(newm), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}
            }






            else if(m.getMsg_type().equals("CIRCLE_REGISTRATION")){
                boolean success = false;

                DBConnect dbConnect = DBConnect.getInstance();

                ResultSet rs  = dbConnect.runFetchQuery("SELECT * from chef_circle where chefID = '"+parentClient.getUserID()+"';");
                try {
                    while (rs.next())
                        success = dbConnect.runManipulationQuery("UPDATE chef_circle set circleID = "+m.getProperty("CIRCLE_ID")+" where chefID = '"+parentClient.getUserID()+"';");

                    if(!success)
                        success = dbConnect.runInsertQuery("INSERT into chef_circle value("+m.getProperty("CIRCLE_ID")+",'"+parentClient.getUserID()+"');");

                }catch (Exception e){}

                Message newm = new Message(Message.Direction.SERVER_TO_CLIENT,"GEOLOC_REG_RESP");
                if(success){
                    newm.putProperty("RESULT","OK");
                    newm.putProperty("CIRCLE_ID",m.getProperty("CIRCLE_ID"));

                    try {
                        rs = dbConnect.runFetchQuery("SELECT circleName from circles where circleID = '" + m.getProperty("CIRCLE_ID") + "';");
                        rs.next();
                        newm.putProperty("CIRCLE_NAME",rs.getString("circleName"));
                    }catch (Exception e){}

                }
                else newm.putProperty("RESULT","N_OK");
                try {
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(newm), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}

            }

            else if(m.getMsg_type().equals("")){

            }

            else if(m.getMsg_type().equals("")){

            }

            else if(m.getMsg_type().equals("")){

            }

            else if(m.getMsg_type().equals("")){

            }

            else if(m.getMsg_type().equals("")){

            }

            else if(m.getMsg_type().equals("")){

            }

            //.....
        }

        private boolean updateLDetails(Message m) {
            ChefLogin cl = (ChefLogin) m.getProperty("IDENTITY");
            if(cl!=null){
                DBConnect con = DBConnect.getInstance();
                String regNo = cl.regNo;
                if(regNo!=null){
                    ResultSet rs = con.runFetchQuery("SELECT * from chef_login where chefID = '"+regNo+"';");
                    try {
                        while (rs.next()) {
                            String exec = "UPDATE chef_login set loginID = '" + cl.uName + "', password = '" + cl.pwd + "' where chefID = '" + cl.regNo + "';";
                            return (con.runManipulationQuery(exec));
                        }
                        String exec = "INSERT into chef_login value('" + cl.regNo + "','" + cl.uName + "','" + cl.pwd + "');";
                        return (con.runInsertQuery(exec));
                    }catch (Exception e){e.printStackTrace(); return false;}
                }
            }
            return false;
        }

        private boolean updatePDetails(Message m) {
            ChefIdentity ci = (ChefIdentity) m.getProperty("IDENTITY");
            if(ci!=null){
                DBConnect con = DBConnect.getInstance();
                String regNo = ci.regNo;
                if(regNo!=null){
                    String manip = "UPDATE chef_main_table set resAddr = '"+ci.resAddr.address+"',resCity = '"+ci.resAddr.city+"', resState = '"+ci.resAddr.state+"', resPin = '"+ci.resAddr.pinNo+"', mailAddr = '"+ci.mailAddr.address+"',mailCity =  '"+ci.mailAddr.city+"', mailState = '"+ci.mailAddr.state+"', mailPin = '"+ci.mailAddr.pinNo+"', mobNo = '"+ci.mob+"', altNo = '"+ci.altmob+"', emerNo = '"+ci.emermob+"', email = '"+ci.email+"' where chefID = '"+regNo+"';";
                    return con.runManipulationQuery(manip);
                }
            }
            return false;
        }
    };


}
