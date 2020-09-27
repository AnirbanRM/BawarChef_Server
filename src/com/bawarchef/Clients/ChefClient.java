package com.bawarchef.Clients;

import com.bawarchef.Communication.EncryptedPayload;
import com.bawarchef.Communication.Message;
import com.bawarchef.Communication.ObjectByteCode;
import com.bawarchef.Containers.ChefIdentity;
import com.bawarchef.Containers.ChefLogin;
import com.bawarchef.Containers.GeoLocationCircle;
import com.bawarchef.Containers.ProfileContainer;
import com.bawarchef.DBConnect;

import java.net.Socket;
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
            if(m.getMsg_type().equals("UPD_PDET")){
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

                Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT,"GEOLOC_RESP");
                new_m.putProperty("NEARBY_P",geoLocationCircles);

                try {
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}

            }
            else if(m.getMsg_type().equals("UPD_PROFILE_CHEF")){
                boolean success = false;

                Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT,"UPD_PROFILE_RESP");
                ProfileContainer profileContainer = (ProfileContainer) m.getProperty("DATA");
                String base64DP = "";
                if(profileContainer.dp!=null)
                    base64DP = Base64.getEncoder().encodeToString(profileContainer.dp);
                DBConnect dbConnect = DBConnect.getInstance();

                ResultSet rs = dbConnect.runFetchQuery("select chefID from chef_login where loginID = '"+parentClient.getUserID()+"';");
                try{
                    rs.next();
                    dbConnect.runManipulationQuery("UPDATE chef_login set loginID = '"+profileContainer.uName+"' where chefID = '"+rs.getString("chefID")+"';");
                    ResultSet rs2 = dbConnect.runFetchQuery("select chefID from chef_profile_table where chefID = '"+rs.getString("chefID")+"';");
                    while(rs2.next()) {
                        success = dbConnect.runManipulationQuery("UPDATE chef_profile_table set bio='" + profileContainer.bio + "',lat=" + profileContainer.resiLat + ",lng=" + profileContainer.resiLng + ",dp='" + base64DP + "' where chefID = '" + rs.getString("chefID") + "';");
                    }

                    if(!success){
                        success = dbConnect.runInsertQuery("INSERT INTO chef_profile_table value('"+rs.getString("chefID")+"','"+base64DP+"',"+profileContainer.resiLat+","+profileContainer.resiLng+",'"+profileContainer.bio+"');");
                    }

                }catch (Exception e){}

                if(success)new_m.putProperty("RESULT","SUCCESS");
                else new_m.putProperty("RESULT","FAILURE");

                try {
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}

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
                            parentClient.setUserID(cl.uName);
                            return (con.runManipulationQuery(exec));
                        }
                        String exec = "INSERT into chef_login value('" + cl.regNo + "','" + cl.uName + "','" + cl.pwd + "');";
                        parentClient.setUserID(cl.uName);
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
