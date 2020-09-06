package com.bawarchef;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBConnect {
    private static DBConnect dBConnect = new DBConnect();
    private Connection connection=null;
    private DBConnect(){
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            Preferences p = Preferences.getInstance();
            connection= DriverManager.getConnection("jdbc:mysql://"+p.DB_SERVER_HOST+":"+p.DB_SERVER_PORT+"/"+ p.DB_SERVER_DBNAME,p.DB_SERVER_CLIENT_NAME, p.DB_SERVER_CLIENT_PWD);
            System.out.println("DB Working");
        }catch(Exception e){System.out.println(e);}
    }

    public static DBConnect getInstance(){
        return dBConnect;
    }

    public ResultSet runFetchQuery(String q){
        try {
            Statement s = connection.createStatement();
            return s.executeQuery(q);
        }catch (Exception e){return null;}
    }

    public boolean runManipulationQuery(String q){
        try {
            Statement s = connection.createStatement();
            return s.execute(q);
        }catch (Exception e){return false;}
    }


}
