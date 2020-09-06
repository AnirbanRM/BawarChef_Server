package com.bawarchef;

import java.io.Serializable;

public class Preferences implements Serializable {

    public String D_KEY_0 = "6cf'K,ZvZd11lF|rTzUiz;Y6($!ZXWgc^/[-WI";
    public int PORT0 = 3009;
    public String DB_SERVER_HOST = "127.0.0.1";
    public int DB_SERVER_PORT = 3306;
    public String DB_SERVER_DBNAME = "BawarChef";
    public String DB_SERVER_CLIENT_NAME = "root";
    public String DB_SERVER_CLIENT_PWD = "root";


    private static Preferences p = new Preferences();

    public static Preferences getInstance() {
        return p;
    }
}
