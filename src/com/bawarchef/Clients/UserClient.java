package com.bawarchef.Clients;

import com.bawarchef.Communication.EncryptedPayload;
import com.bawarchef.Communication.Message;
import com.bawarchef.Communication.ObjectByteCode;
import com.bawarchef.Containers.ChefAdvertMajorContainer;
import com.bawarchef.Containers.ChefAdvertMinorContainer;
import com.bawarchef.Containers.GeoLocationCircle;
import com.bawarchef.DBConnect;
import com.bawarchef.android.Hierarchy.DataStructure.Tree;

import java.io.*;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Base64;


public class UserClient{

    Client parentClient;
    public UserClient(Client u) {
        this.parentClient = u;
        u.setMessageProcessor(processor);
    }

    Client.MessageProcessor processor = new Client.MessageProcessor() {
        @Override

        public void process(Message m) {
            if (m.getMsg_type().equals("GEOLOC_QUERY")) {

                double lat = (double) m.getProperty("LAT");
                double lng = (double) m.getProperty("LNG");

                double latmin = lat - 0.1, latmax = lat + 0.1;
                double lngmin = lng - 0.1, lngmax = lng + 0.1;

                String query = "SELECT * from circles where lat between " + latmin + " and " + latmax + " and lng between " + lngmin + " and " + lngmax + ";";
                DBConnect dbConnect = DBConnect.getInstance();
                ResultSet rs = dbConnect.runFetchQuery(query);
                ArrayList<GeoLocationCircle> geoLocationCircles = new ArrayList<GeoLocationCircle>();
                try {
                    while (rs.next()) {
                        GeoLocationCircle r = new GeoLocationCircle(rs.getInt("circleID"), rs.getString("circleName"), rs.getDouble("lat"), rs.getDouble("lng"));
                        geoLocationCircles.add(r);
                    }
                } catch (Exception e) {
                }

                Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT, "GEOLOC_RESP");
                new_m.putProperty("NEARBY_P", geoLocationCircles);

                try {
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                } catch (Exception e) {
                }

            }

            else if(m.getMsg_type().equals("FETCH_CHEF")){

                DBConnect dbConnect = DBConnect.getInstance();
                ResultSet rs = dbConnect.runFetchQuery("select chef_main_table.chefID,\n" +
                        "chef_main_table.f_name,\n" +
                        "chef_main_table.l_name,\n" +
                        "chef_profile_table.dp,\n" +
                        "chef_profile_table.lat,\n" +
                        "chef_profile_table.lng\n" +
                        "from chef_main_table \n" +
                        "left join chef_profile_table using (chefID) \n" +
                        "left join chef_circle using (chefID) where circleID = "+m.getProperty("CIRCLE")+";");

                ArrayList<ChefAdvertMinorContainer> chefs = new ArrayList<ChefAdvertMinorContainer>() ;

                try {
                    while (rs.next()) {
                        ChefAdvertMinorContainer chef = new ChefAdvertMinorContainer(rs.getString("chefID"), rs.getString("f_name"), rs.getString("l_name"), 4.5f);
                        chef.setLocation(new ChefAdvertMinorContainer.LatLng(rs.getDouble("lat"), rs.getDouble("lng")));
                        chef.setDp(rs.getString("dp"));
                        chefs.add(chef);
                    }
                }catch (Exception e){}

                Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT, "FETCH_CHEF_RESULT");
                new_m.putProperty("CHEFS", chefs);

                try {
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                } catch (Exception e) {
                }
            }

            else if(m.getMsg_type().equals("FETCH_CHEF_IND")){
                String chefID = (String)m.getProperty("chefID");

                DBConnect dbConnect = DBConnect.getInstance();

                ResultSet rs = dbConnect.runFetchQuery("select chef_main_table.chefID,\n" +
                        "chef_main_table.f_name,\n" +
                        "chef_main_table.l_name,\n" +
                        "chef_profile_table.dp,\n" +
                        "chef_profile_table.bio\n" +
                        "from chef_main_table \n" +
                        "left join chef_profile_table using (chefID) where chef_main_table.chefID = '"+chefID+"';");

                try {
                    rs.next();
                    ChefAdvertMajorContainer c = new ChefAdvertMajorContainer(chefID,rs.getString("f_name"),rs.getString("l_name"),4.5f, rs.getString("bio"));
                    c.setDp(rs.getString("dp"));

                    Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT, "RESP_IND_CHEF");
                    new_m.putProperty("CHEF", c);
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}
            }

            else if(m.getMsg_type().equals("FETCH_CHEF_MENU")){
                String chefID = (String)m.getProperty("chefID");

                DBConnect dbConnect = DBConnect.getInstance();
                ResultSet rs = dbConnect.runFetchQuery("select menuSerial from chef_menu where chefID = '"+chefID+"';");

                Tree t = null;

                try {
                    rs.next();
                    byte [] bytarr = Base64.getDecoder().decode(rs.getString("menuSerial"));
                    ByteArrayInputStream bais = new ByteArrayInputStream(bytarr);
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    t = (Tree) ois.readObject();

                    Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT, "RESP_CHEF_MENU");
                    new_m.putProperty("CHEF_MENU", t);
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}
            }
        }

    };


}
