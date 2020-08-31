package com.bawarchef;

import java.io.Serializable;

public class Preferences implements Serializable {

    public String D_KEY_0 = "6cf'K,ZvZd11lF|rTzUiz;Y6($!ZXWgc^/[-WI";
    public int PORT0 = 3009;

    static Preferences p = new Preferences();

    public static Preferences getInstance() {
        return p;
    }
}
