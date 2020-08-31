package com.bawarchef.Communication;

import com.bawarchef.Clients.Client;

public class Authenticator {

    public static abstract class OnSuccessfulAuthentication{
        public abstract void onSuccess();
    }

    public static abstract class OnFailedAuthentication{
        public abstract void onFailure();
    }

    OnSuccessfulAuthentication onSuccessfulAuthentication=null;
    OnFailedAuthentication onFailedAuthentication=null;
    Client client;

    public Authenticator(Client c){
        this.client = c;
    }

    public void authenticate(){


    }


    public void setOnSuccessfulAuthentication(OnSuccessfulAuthentication onSuccessfulAuthentication) {
        this.onSuccessfulAuthentication = onSuccessfulAuthentication;
    }


    public void setOnFailedAuthentication(OnFailedAuthentication onFailedAuthentication) {
        this.onFailedAuthentication = onFailedAuthentication;
    }
}
