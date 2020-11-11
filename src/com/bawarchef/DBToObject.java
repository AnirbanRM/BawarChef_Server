package com.bawarchef;

import com.bawarchef.Containers.*;
import com.bawarchef.android.Hierarchy.DataStructure.CartItem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;

public class DBToObject {

    public static ArrayList<ChefIdentity> ChefMTableToChefIdentity(ResultSet rs){
        ArrayList<ChefIdentity> arrayList =  new ArrayList<ChefIdentity>();
        try {
            while (rs.next()) {

                ChefIdentity ci = new ChefIdentity();

                ci.regNo = rs.getString("chefID");
                ci.fname = rs.getString("f_name");
                ci.lname = rs.getString("l_name");
                ci.dob = rs.getDate("dob").toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
                ci.gender = rs.getString("gender").charAt(0);

                ci.resAddr.address = rs.getString("resAddr");
                ci.resAddr.city = rs.getString("resCity");
                ci.resAddr.state = rs.getString("resState");
                ci.resAddr.pinNo = rs.getString("resPin");

                ci.mailAddr.address = rs.getString("mailAddr");
                ci.mailAddr.city = rs.getString("mailCity");
                ci.mailAddr.state = rs.getString("mailState");
                ci.mailAddr.pinNo = rs.getString("mailPin");

                ci.mob = rs.getString("mobNo");
                ci.altmob = rs.getString("altNo");
                ci.emermob = rs.getString("emerNo");
                ci.email = rs.getString("email");
                ci.aadhar = rs.getString("aadhar_no");

                arrayList.add(ci);

            }
        }catch (Exception e){ }
        return arrayList;
    }

    public static ArrayList<UserIdentity> UserMTableToUserIdentity(ResultSet rs){
        ArrayList<UserIdentity> arrayList =  new ArrayList<UserIdentity>();
        try {
            while (rs.next()) {

                UserIdentity ui = new UserIdentity();

                ui.userID = rs.getString("userID");
                ui.fname = rs.getString("f_name");
                ui.lname = rs.getString("l_name");

                ui.addr.address = rs.getString("addr");
                ui.addr.city = rs.getString("city");
                ui.addr.state = rs.getString("state");
                ui.addr.pinNo = rs.getString("pin");

                ui.mob = rs.getString("mobNo");
                ui.email = rs.getString("email");

                if(rs.getDate("dob")!=null) {
                    LocalDate date = rs.getDate("dob").toLocalDate();
                    ui.dob = date.getDayOfMonth() + "/" + date.getMonthValue() + "/" + date.getYear();
                }

                String dpStr = rs.getString("dp");
                if(dpStr!=null&&dpStr.length()!=0) {
                    ui.dp = Base64.getDecoder().decode(dpStr);
                }
                ui.gender = rs.getString("gender");
                ui.lati = rs.getDouble("lat");
                ui.longi = rs.getDouble("lng");;
                ui.userID = rs.getString("userID");

                arrayList.add(ui);

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return arrayList;
    }

    public static ArrayList<OrderListItemClass> rsToOrdersMIni(ResultSet rs){
        ArrayList<OrderListItemClass> orders = new ArrayList<OrderListItemClass>();

        try {
            while (rs.next()) {
                String ordID = String.valueOf(rs.getInt("orderID"));
                String name = rs.getString("f_name")+" "+rs.getString("l_name");
                byte[] dp= null;
                String dpStr = rs.getString("dp");
                if(dpStr!=null&&dpStr.length()!=0) {
                    dp = Base64.getDecoder().decode(dpStr);
                }

                String bookingDateTime = rs.getDate("bookingDateTime").toString()+" "+rs.getTime("bookingDateTime");
                String status = rs.getString("status");
                double price = rs.getDouble("price");

                OrderListItemClass orderListItemClass = new OrderListItemClass(ordID,name,dp,price,status, bookingDateTime);
                orders.add(orderListItemClass);
            }
        }catch (Exception e){}

        return orders;
    }

    public static OrderSummaryItem rsToOrders(ResultSet rs){
        ArrayList<OrderSummaryItem> orders = new ArrayList<OrderSummaryItem>();

        try {
            while (rs.next()) {
                String ordID = String.valueOf(rs.getInt("orderID"));
                String chefID = rs.getString("chefID");

                String name = rs.getString("f_name")+" "+rs.getString("l_name");
                byte[] dp= null;
                String dpStr = rs.getString("dp");
                if(dpStr!=null&&dpStr.length()!=0) {
                    dp = Base64.getDecoder().decode(dpStr);
                }
                double lat = rs.getDouble("lat");
                double lng = rs.getDouble("lng");

                String bookingDateTime = rs.getDate("bookingDateTime").toString()+" "+rs.getTime("bookingDateTime");
                String status = rs.getString("status");
                double price = rs.getDouble("price");
                String address = rs.getString("address");

                byte b[] = Base64.getDecoder().decode(rs.getString("cart"));
                ByteArrayInputStream bais = new ByteArrayInputStream(b);
                ObjectInputStream oid = new ObjectInputStream(bais);
                ArrayList<CartItem> items = (ArrayList<CartItem>) oid.readObject();

                OrderSummaryItem osi = new OrderSummaryItem(ordID,chefID,name,bookingDateTime,address,status,dp,price,items,lat,lng);
                return osi;

            }
        }catch (Exception e){e.printStackTrace();}
        return null;
    }

}
