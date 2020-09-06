package com.bawarchef;

import com.bawarchef.Containers.ChefIdentity;

import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

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
}
