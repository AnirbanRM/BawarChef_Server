package com.bawarchef.Clients;

import com.bawarchef.Broadcast.BroadcastEngine;
import com.bawarchef.Broadcast.BroadcastItem;
import com.bawarchef.Communication.EncryptedPayload;
import com.bawarchef.Communication.Message;
import com.bawarchef.Communication.ObjectByteCode;
import com.bawarchef.Containers.*;
import com.bawarchef.DBConnect;
import com.bawarchef.DBToObject;
import com.bawarchef.TrackEngine;
import com.bawarchef.android.Hierarchy.DataStructure.CartItem;
import com.bawarchef.android.Hierarchy.DataStructure.Tree;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.*;
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

            if(m.getMsg_type().equals("FETCH_STATS")){
                DBConnect dbConnect = DBConnect.getInstance();

                long completed=0,pending=0, rank=0;
                double rating=0, score=0;

                String name=null;
                double scoreCOTM = 0;
                byte[] dp = null;


                ResultSet rs = dbConnect.runFetchQuery("select (SELECT count(*) from orders where chefID = '"+parentClient.getUserID()+"' and status = 'COMPLETED') as completed, (SELECT count(*)as pending from orders where chefID = '"+parentClient.getUserID()+"' and status = 'PENDING') as pending, (select rating from chef_rating where chefID = '"+parentClient.getUserID()+"') as rating, (select score from chef_score where chefID = '"+parentClient.getUserID()+"') as score;");
                try {
                    while (rs.next()) {
                        completed = rs.getLong("completed");
                        pending = rs.getLong("pending");
                        rating = rs.getDouble("rating");
                        score = rs.getDouble("score");
                    }
                }catch (Exception e){}

                rs = dbConnect.runFetchQuery("select * from (\n" +
                        "\tselect row_number() OVER(order by score desc) as position, chefID, score from chef_score where chefID in \n" +
                        "\t\t\t(\n" +
                        "\t\t\t\tselect chefID from chef_circle where circleID = \n" +
                        "\t\t\t\t(\n" +
                        "\t\t\t\t\tSELECT circleID from chef_circle where chef_circle.chefID = '"+parentClient.getUserID()+"'\n" +
                        "\t\t\t\t)\n" +
                        "\t\t\t)\n" +
                        "\t\t) p where chefID = '"+parentClient.getUserID()+"';");
                try {
                    while (rs.next()){
                        rank = rs.getInt("position");
                    }
                }catch (Exception e){}



                rs = dbConnect.runFetchQuery("select chefID,f_name,l_name,dp,score from \n" +
                        "    (\n" +
                        "\t\tselect chefID, score from prev_month_score\n" +
                        "\t\twhere chefID in \n" +
                        "\t\t\t(\n" +
                        "\t\t\t\tselect chefID from chef_circle where circleID = \n" +
                        "\t\t\t\t(\n" +
                        "\t\t\t\t\tSELECT circleID from chef_circle where chef_circle.chefID = '"+parentClient.getUserID()+"'\n" +
                        "\t\t\t\t)\n" +
                        "\t\t\t) order by score desc limit 1\n" +
                        "\t) s_tab\n" +
                        " left join chef_main_table using (chefID) left join chef_profile_table using (chefID); ");


                try {
                    while (rs.next()){
                        name = rs.getString("f_name") + " " + rs.getString("l_name");
                        String tempdp = rs.getString("dp");
                        if(tempdp!=null && tempdp.length()!=0){
                            dp = Base64.getDecoder().decode(tempdp);
                        }
                        scoreCOTM = rs.getDouble("score");
                    }
                }catch (Exception e){e.printStackTrace();}


                Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT,"RESP_STATS_FETCH");
                new_m.putProperty("PEND",pending);
                new_m.putProperty("COMP",completed);
                new_m.putProperty("RATE",rating);
                new_m.putProperty("SCORE",score);
                new_m.putProperty("RANK",rank);
                new_m.putProperty("COTMName",name);
                new_m.putProperty("COTMDP",dp);
                new_m.putProperty("COTMScore",scoreCOTM);

                try {
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}
            }




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

                String regCircleID=null,regCircleName=null;
                rs = dbConnect.runFetchQuery("SELECT chef_circle.circleID, circleName from chef_circle left join circles using(circleID) where chefID = '"+chefID+"';");
                try {
                    while(rs.next()) {
                        regCircleID = rs.getString("circleID");
                        regCircleName = rs.getString("circleName");
                    }
                }catch (Exception e){}


                Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT,"GEOLOC_RESP");
                new_m.putProperty("NEARBY_P",geoLocationCircles);
                new_m.putProperty("REG_CIRCLE",regCircleID);
                new_m.putProperty("REG_CIRCLE_NAME",regCircleName);

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

                dbConnect.runManipulationQuery("UPDATE chef_login set loginID = '"+chefProfileContainer.uName+"' where chefID = '"+parentClient.getUserID()+"';");

                success = dbConnect.runManipulationQuery("REPLACE INTO chef_profile_table values('"+parentClient.getUserID()+"','"+base64DP+"','"+chefProfileContainer.resiLat+"','"+chefProfileContainer.resiLng+"','"+chefProfileContainer.bio+"');");

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

            else if(m.getMsg_type().equals("ORDER_FETCH_PRESENT")){
                DBConnect dbConnect = DBConnect.getInstance();
                String query = "SELECT orderID, f_name, l_name, dp, bookingDateTime, status, cart from orders left join user_main_table on (orders.userID = user_main_table.userID) left join user_profile_table on (orders.userID = user_profile_table.userID) where orders.chefID = '"+parentClient.getUserID()+"' and (status = '"+Order.Status.CHEF_APPROVED+"' or status = '"+Order.Status.ONGOING+"' or status = '"+Order.Status.PENDING+"');";

                ResultSet rs = dbConnect.runFetchQuery(query);
                ArrayList<ChefOrderListItemClass> orders = DBToObject.rsToChefOrdersMIni(rs,true);

                try {
                    Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT, "RESP_ORDER");
                    new_m.putProperty("Orders", orders);
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}
            }



            else if(m.getMsg_type().equals("ORDER_FETCH_PAST")){
                DBConnect dbConnect = DBConnect.getInstance();
                String query = "SELECT orderID, f_name, l_name, dp, bookingDateTime, price, status from orders left join user_main_table on (orders.userID = user_main_table.userID) left join user_profile_table on (orders.userID = user_profile_table.userID) where orders.chefID = '"+parentClient.getUserID()+"' and (status = '"+Order.Status.COMPLETED+"');";

                ResultSet rs = dbConnect.runFetchQuery(query);
                ArrayList<ChefOrderListItemClass> orders = DBToObject.rsToChefOrdersMIni(rs,false);


                try {
                    Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT, "RESP_ORDER");
                    new_m.putProperty("Orders", orders);
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}
            }

            else if(m.getMsg_type().equals("ORDER_INFO")){

                String orderID = (String) m.getProperty("ORDERID");
                String query = "SELECT orderID, orders.userID, orders.lat, orders.lng, mobNo, f_name, l_name, dp, bookingDateTime, address, cart, status from orders left join user_main_table on (orders.userID = user_main_table.userID) left join user_profile_table on (orders.userID = user_profile_table.userID) where orders.orderID = '"+orderID+"';";
                OrderSummaryItem osi = DBToObject.rsToChefOrders(DBConnect.getInstance().runFetchQuery(query));
                try {
                    Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT, "RESP_ORDERID_INFO");
                    new_m.putProperty("OrderDetail", osi);
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}
            }

            else if(m.getMsg_type().equals("ORDER_APPROVE_DECLINE")){
                String orderid = (String) m.getProperty("ORDER");
                Order.Status response = (Order.Status) m.getProperty("RESPONSE");
                String cart=null;

                DBConnect dbConnect = DBConnect.getInstance();
                boolean done = false;
                if(response.equals(Order.Status.CHEF_APPROVED)) {
                    try{
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject((ArrayList<CartItem>)m.getProperty("CART"));
                        oos.flush();
                        cart = Base64.getEncoder().encodeToString(baos.toByteArray());

                    }catch (Exception e){}
                    done = dbConnect.runManipulationQuery("UPDATE orders set status = '" + response.toString() + "', cart = '"+cart+"' where orderID = '" + orderid + "';");
                }

                else
                    done = dbConnect.runManipulationQuery("UPDATE orders set status = '" + response.toString() + "' where orderID = '" + orderid + "';");

                try {
                    Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT, "RESP_ORDER_APPROVE_DECLINE");
                    new_m.putProperty("RESULT", done?"SUCCESS":"FAILURE");
                    new_m.putProperty("RESPONSE",response);
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}
            }



            else if(m.getMsg_type().equals("ORDER_START_STOP")){
                String query="";
                DBConnect dbConnect = DBConnect.getInstance();

                if(m.getProperty("RESPONSE").equals("START")){
                    query = "INSERT INTO orders_extend(orderID,sStart) value('"+m.getProperty("ORDER")+"','"+System.currentTimeMillis()/1000+"');";
                    dbConnect.runInsertQuery(query);
                    query = "UPDATE orders set status = '"+Order.Status.ONGOING+"' where orderID = '"+m.getProperty("ORDER")+"';";
                    dbConnect.runManipulationQuery(query);
                }else if(m.getProperty("RESPONSE").equals("STOP")){
                    query = "UPDATE orders_extend set sEnd = '"+System.currentTimeMillis()/1000+"' where orderID = '"+m.getProperty("ORDER")+"';";
                    dbConnect.runManipulationQuery(query);

                    double price = getPrice((String) m.getProperty("ORDER"));

                    query = "UPDATE orders set status = '"+Order.Status.COMPLETED+"', price = '"+price+"' where orderID = '"+m.getProperty("ORDER")+"';";
                    dbConnect.runManipulationQuery(query);
                }

                try {
                    Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT, "ORDER_START_STOP_RESP");
                    new_m.putProperty("RESPONSE", m.getProperty("RESPONSE"));
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}

            }

            else if(m.getMsg_type().equals("FETCH_BROADCASTS")){

                String regCircleID=null;
                DBConnect dbConnect = DBConnect.getInstance();
                ResultSet rs = dbConnect.runFetchQuery("SELECT circleID from chef_circle where chefID = '"+parentClient.getUserID()+"';");
                try {
                    while(rs.next())
                        regCircleID = rs.getString("circleID");
                }catch (Exception e){}

                ArrayList<BroadcastItemContainer> bics = new ArrayList<BroadcastItemContainer>();

                BroadcastEngine be = BroadcastEngine.getInstance();
                for(BroadcastItem bi : be.getBroadcasts(regCircleID)) {
                    BroadcastItemContainer bic = new BroadcastItemContainer();
                    bic.id = bi.id;
                    bic.message = bi.message;
                    bic.timestamp = bi.timestamp;
                    bics.add(bic);
                }

                try {
                    System.out.println(bics.size());
                    Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT, "RESP_BROADCASTS");
                    new_m.putProperty("BROADCASTS", bics);
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}

            }


            else if(m.getMsg_type().equals("BROADCAST_REPLY")){
                String regCircleID=null;
                DBConnect dbConnect = DBConnect.getInstance();
                ResultSet rs = dbConnect.runFetchQuery("SELECT circleID from chef_circle where chefID = '"+parentClient.getUserID()+"';");
                try {
                    while(rs.next())
                        regCircleID = rs.getString("circleID");
                }catch (Exception e){}

                BroadcastEngine be = BroadcastEngine.getInstance();
                be.newReply(regCircleID,(String)m.getProperty("ID"),String.valueOf(parentClient.userID), String.valueOf(m.getProperty("NAME")), (String)m.getProperty("MSG"));

                try {
                    Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT, "BROADCAST_REPLY_RESP");
                    new_m.putProperty("RESULT", "SUCCESS");
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


            else if(m.getMsg_type().equals("")){


            }



            //.....
        }

        private double getPrice(String orderID){
            String query = "SELECT cart,sEnd-sStart as duration from orders left join orders_extend on (orders.orderID = orders_extend.orderID) where orders.orderID = '"+orderID+"';";
            DBConnect dbConnect = DBConnect.getInstance();

            ResultSet rs = dbConnect.runFetchQuery(query);
            try {
                rs.next();
                long duration = rs.getLong("duration");

                String cart = rs.getString("cart");
                byte[] cartbytearr = Base64.getDecoder().decode(cart);
                ByteArrayInputStream bais = new ByteArrayInputStream(cartbytearr);
                ObjectInputStream ois = new ObjectInputStream(bais);
                ArrayList<CartItem> cartitems = (ArrayList<CartItem>)  ois.readObject();

                double price = 0;

                for(CartItem c : cartitems)
                    price += c.getBasePrice() + c.getIncrPrice() * (((int)(duration/1800))+1);

                return price;
            }catch (Exception e){ }
            return 0;
        }

        private boolean updateLDetails(Message m) {
            ChefLogin cl = (ChefLogin) m.getProperty("IDENTITY");
            if(cl!=null){
                DBConnect con = DBConnect.getInstance();
                String regNo = cl.regNo;
                if(regNo!=null){
                    String exec = "REPLACE INTO chef_login values ('"+cl.regNo+"','"+cl.uName+"','"+cl.pwd+"');";
                    return con.runManipulationQuery(exec);
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
