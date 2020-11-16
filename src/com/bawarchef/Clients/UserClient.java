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

            if (m.getMsg_type().equals("PROFILE_FETCH")) {
                Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT, "PROFILE_FETCH_RESP");
                UserIdentity userIdentity = new UserIdentity();
                DBConnect dbConnect = DBConnect.getInstance();
                ResultSet rs = dbConnect.runFetchQuery("select * from user_main_table left join user_profile_table using (userID) left join user_login using (userID) where LoginID = '" + parentClient.getUserID() + "';");

                try {
                    while (rs.next()) {
                        userIdentity.addr.address = rs.getString("addr");
                        userIdentity.addr.city = rs.getString("city");
                        userIdentity.addr.state = rs.getString("state");
                        userIdentity.addr.pinNo = rs.getString("pin");
                        userIdentity.dob = rs.getString("dob");
                        System.out.println(rs.getString("dob"));
                        String dpStr = rs.getString("dp");
                        if (dpStr != null && dpStr.length() != 0) {
                            userIdentity.dp = Base64.getDecoder().decode(dpStr);
                        }
                        userIdentity.email = rs.getString("email");
                        userIdentity.fname = rs.getString("f_name");
                        userIdentity.lname = rs.getString("l_name");
                        userIdentity.gender = rs.getString("gender");
                        ;
                        userIdentity.lati = rs.getDouble("lat");
                        userIdentity.longi = rs.getDouble("lng");
                        ;
                        userIdentity.mob = rs.getString("f_name");
                        ;
                        userIdentity.userID = rs.getString("userID");
                    }
                } catch (Exception e) {
                }

                new_m.putProperty("DATA", userIdentity);
                try {
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                } catch (Exception e) {
                }
            }

            if (m.getMsg_type().equals("UPD_PROFILE_USER")) {
                Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT, "UPD_PROFILE_RESP");
                UserIdentity userIdentity = (UserIdentity) m.getProperty("DATA");
                DBConnect dbConnect = DBConnect.getInstance();

                String updquery1 = "UPDATE user_main_table SET f_name = '" + userIdentity.fname + "',l_name = '" + userIdentity.lname + "',addr = '" + userIdentity.addr.address + "',city = '" + userIdentity.addr.city + "',state = '" + userIdentity.addr.state + "',pin = '" + userIdentity.addr.pinNo + "',mobNo = '" + userIdentity.mob + "',email = '" + userIdentity.email + "' where userID = '" + parentClient.getUserID() + "';";
                boolean a = dbConnect.runManipulationQuery(updquery1);

                String base64DP = "";
                if (userIdentity.dp != null)
                    base64DP = Base64.getEncoder().encodeToString(userIdentity.dp);

                String updquery2 = "REPLACE into user_profile_table VALUES('" + parentClient.getUserID() + "'," + userIdentity.lati + "," + userIdentity.longi + ",'" + userIdentity.gender + "','" + userIdentity.dob + "','" + base64DP + "');";
                boolean b = dbConnect.runManipulationQuery(updquery2);

                String updquery3 = "UPDATE user_login SET loginID = '" + userIdentity.userID + "' were userID = '" + parentClient.getUserID() + "';";
                boolean c = dbConnect.runManipulationQuery(updquery3);

                if (a & b & c)
                    new_m.putProperty("RESULT", "SUCCESS");
                else
                    new_m.putProperty("RESULT", "FAILURE");

                new_m.putProperty("DATA", userIdentity);
                try {
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                } catch (Exception e) {
                }
            } else if (m.getMsg_type().equals("GEOLOC_QUERY")) {

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

            } else if (m.getMsg_type().equals("FETCH_CHEF")) {

                DBConnect dbConnect = DBConnect.getInstance();
                ResultSet rs = dbConnect.runFetchQuery("select chef_main_table.chefID, rating,\n" +
                        "chef_main_table.f_name,\n" +
                        "chef_main_table.l_name,\n" +
                        "chef_profile_table.dp,\n" +
                        "chef_profile_table.lat,\n" +
                        "chef_profile_table.lng\n" +
                        "from chef_main_table \n" +
                        "left join chef_profile_table using (chefID) left join chef_rating using (chefID) \n" +
                        "left join chef_circle using (chefID) where circleID = " + m.getProperty("CIRCLE") + ";");

                ArrayList<ChefAdvertMinorContainer> chefs = new ArrayList<ChefAdvertMinorContainer>();

                try {
                    while (rs.next()) {
                        ChefAdvertMinorContainer chef = new ChefAdvertMinorContainer(rs.getString("chefID"), rs.getString("f_name"), rs.getString("l_name"), (float) rs.getDouble("rating"));
                        chef.setLocation(new ChefAdvertMinorContainer.LatLng(rs.getDouble("lat"), rs.getDouble("lng")));
                        chef.setDp(rs.getString("dp"));
                        chefs.add(chef);
                    }
                } catch (Exception e) {
                }

                Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT, "FETCH_CHEF_RESULT");
                new_m.putProperty("CHEFS", chefs);

                try {
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                } catch (Exception e) {
                }
            } else if (m.getMsg_type().equals("FETCH_CHEF_IND")) {
                String chefID = (String) m.getProperty("chefID");

                DBConnect dbConnect = DBConnect.getInstance();

                ResultSet rs = dbConnect.runFetchQuery("select chef_main_table.chefID, rating,\n" +
                        "chef_main_table.f_name,\n" +
                        "chef_main_table.l_name,\n" +
                        "chef_profile_table.dp,\n" +
                        "chef_profile_table.bio\n" +
                        "from chef_main_table \n" +
                        "left join chef_profile_table using (chefID) left join chef_rating using (chefID) where chef_main_table.chefID = '" + chefID + "';");

                try {
                    rs.next();
                    ChefAdvertMajorContainer c = new ChefAdvertMajorContainer(chefID, rs.getString("f_name"), rs.getString("l_name"), (float)rs.getDouble("rating"), rs.getString("bio"));
                    c.setDp(rs.getString("dp"));

                    Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT, "RESP_IND_CHEF");
                    new_m.putProperty("CHEF", c);
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                } catch (Exception e) {
                }
            } else if (m.getMsg_type().equals("FETCH_CHEF_MENU")) {
                String chefID = (String) m.getProperty("chefID");

                DBConnect dbConnect = DBConnect.getInstance();
                ResultSet rs = dbConnect.runFetchQuery("select menuSerial from chef_menu where chefID = '" + chefID + "';");

                Tree t = null;

                try {
                    rs.next();
                    byte[] bytarr = Base64.getDecoder().decode(rs.getString("menuSerial"));
                    ByteArrayInputStream bais = new ByteArrayInputStream(bytarr);
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    t = (Tree) ois.readObject();

                    Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT, "RESP_CHEF_MENU");
                    new_m.putProperty("CHEF_MENU", t);
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                } catch (Exception e) { }
            }




            else if (m.getMsg_type().equals("ORDER_BOOK")) {
                DBConnect dbConnect = DBConnect.getInstance();
                Order o = (Order) m.getProperty("ORDER");

                ArrayList<CartItem> items = o.getOrdereditems();
                String itemStr=null;

                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(items);
                    oos.flush();
                    byte[] itembyt = baos.toByteArray();
                    itemStr = Base64.getEncoder().encodeToString(itembyt);
                }catch (Exception e){}

                String query = "INSERT INTO Orders(chefID,userID,lat,lng,address,bookingDateTime,currentDateTime,cart,status) value('"+o.getChefID()+"','"+o.getUserID()+"','"+o.getLati()+"','"+o.getLongi()+"','"+o.getAddress()+"','"+o.getBookingDate()+" "+o.getBookingTime()+"','"+o.getCurrentDate()+" "+o.getCurrentTime()+"','"+itemStr+"','"+o.getStatus()+"');";
                ArrayList<String> key = dbConnect.runInsertQueryAndgetKey(query);

                try {
                    Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT, "RESP_BOOK_ORDER");
                    new_m.putProperty("OrderID", key.get(0));
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}

            }



            else if(m.getMsg_type().equals("ORDER_FETCH")){
                DBConnect dbConnect = DBConnect.getInstance();
                String query = "SELECT orderID, f_name, l_name, dp, bookingDateTime, price, status from orders left join chef_main_table on (orders.chefID = chef_main_table.chefID) left join chef_profile_table on (orders.chefID = chef_profile_table.chefID) where orders.userID = '"+parentClient.getUserID()+"';";

                ResultSet rs = dbConnect.runFetchQuery(query);
                ArrayList<OrderListItemClass> orders = DBToObject.rsToOrdersMIni(rs);

                try {
                    Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT, "RESP_ORDER");
                    new_m.putProperty("Orders", orders);
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}
            }



            else if(m.getMsg_type().equals("ORDER_INFO")){

                String orderID = (String) m.getProperty("ORDERID");
                String query = "SELECT orders.orderID, orders.chefID, orders.lat, orders.lng, mobNo, f_name, l_name, dp, bookingDateTime, price, address, cart, status, rating from orders left join chef_main_table on (orders.chefID = chef_main_table.chefID) left join chef_profile_table on (orders.chefID = chef_profile_table.chefID) left join orders_extend on (orders.orderID = orders_extend.orderID) where orders.orderID = '"+orderID+"';";

                OrderSummaryItem osi = DBToObject.rsToOrders(DBConnect.getInstance().runFetchQuery(query));
                try {
                    Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT, "RESP_ORDERID_INFO");
                    new_m.putProperty("OrderDetail", osi);
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}
            }




            else if(m.getMsg_type().equals("CANCEL_ORDER")){

                String orderID = (String) m.getProperty("ORDERID");
                String query = "UPDATE orders SET status = '"+Order.Status.USER_CANCELLED+"' where orderID = '"+orderID+"' and userID = '"+parentClient.getUserID()+"';";
                DBConnect dbConnect = DBConnect.getInstance();
                boolean b = dbConnect.runManipulationQuery(query);

                try {
                    Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT, "RESP_ORDER_CANCEL");
                    new_m.putProperty("RESULT",b?"SUCCESS":"FAILURE");
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}

            }




            else if(m.getMsg_type().equals("GET_CHEF_LOC")){
                TrackEngine te = TrackEngine.getInstance();
                String chefID = (String)m.getProperty("CHEF");
                TrackEngine.LatLngPair pair = te.getLocation(chefID);

                try {
                    Message newm = new Message(Message.Direction.SERVER_TO_CLIENT,"RESP_CHEF_LOC");
                    newm.putProperty("LAT",pair==null?0:te.getLocation(chefID).getLatitude());
                    newm.putProperty("LNG",pair==null?0:te.getLocation(chefID).getLongitude());
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(newm), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}
            }




            else if(m.getMsg_type().equals("ORDER_RATING")){
                String query = "UPDATE orders_extend set rating = '"+m.getProperty("RATING")+"' where orderID = '"+m.getProperty("ORDER")+"';";
                boolean d = DBConnect.getInstance().runManipulationQuery(query);

                try {
                    Message newm = new Message(Message.Direction.SERVER_TO_CLIENT,"RESP_ORDER_RATING");
                    newm.putProperty("RESULT",d?"SUCCESS":"FAILURE");
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(newm), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}

            }


            else if(m.getMsg_type().equals("NEW_BCAST")){
                BroadcastEngine broadcastEngine = BroadcastEngine.getInstance();
                broadcastEngine.newBroadcast((String)m.getProperty("CIRCLE"),parentClient.getUserID(),(String)m.getProperty("MSG"));

                try {
                    Message newm = new Message(Message.Direction.SERVER_TO_CLIENT,"NEW_BCAST_REPLY");
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(newm), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}

            }



            else if(m.getMsg_type().equals("FETCH_BCAST")){
                BroadcastEngine broadcastEngine = BroadcastEngine.getInstance();
                ArrayList<BroadcastItem> items = broadcastEngine.getBroadcasts((String)m.getProperty("CIRCLE"),parentClient.getUserID());

                ArrayList<BroadcastItemUser> itemUsers = new ArrayList<BroadcastItemUser>();


                System.out.println(items.size());



                for(BroadcastItem i : items){
                    BroadcastItemUser biu = new BroadcastItemUser();
                    biu.id = i.id;
                    biu.message = i.message;
                    biu.timestamp = i.timestamp;
                    biu.userID = i.userID;
                    biu.broadcast_replies = new ArrayList<BroadcastReply>();

                    for(com.bawarchef.Broadcast.BroadcastReply r : i.broadcast_replies){
                        BroadcastReply reply = new BroadcastReply(r.chefID,r.chefName,r.message);
                        biu.broadcast_replies.add(reply);
                    }

                    itemUsers.add(biu);
               }


                try {
                    Message newm = new Message(Message.Direction.SERVER_TO_CLIENT,"BCAST_RESP");
                    newm.putProperty("DATA",itemUsers);
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(newm), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}



            }
        }
    };
}

