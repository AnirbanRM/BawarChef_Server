package com.bawarchef.Communication;

import com.bawarchef.Clients.Client;
import com.bawarchef.Preferences;

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
    Client.MessageProcessor originalMessageProcessor;

    public Authenticator(Client c){
        this.client = c;
    }

    int count = 0;

    public void authenticate() throws Exception{
        originalMessageProcessor = client.getMessageProcessor();
        client.setMessageProcessor(authProcessor);

        //// INITIAL
        Message m = new Message(Message.Direction.SERVER_TO_CLIENT,"CHALLENGE->CLIENT");
        EncryptedPayload encryptedPayload = new EncryptedPayload(ObjectByteCode.getBytes(m), client.getCrypto_key());
        client.send(encryptedPayload);
    }

    public void setOnSuccessfulAuthentication(OnSuccessfulAuthentication onSuccessfulAuthentication) {
        this.onSuccessfulAuthentication = onSuccessfulAuthentication;
    }


    public void setOnFailedAuthentication(OnFailedAuthentication onFailedAuthentication) {
        this.onFailedAuthentication = onFailedAuthentication;
    }


    Client.MessageProcessor authProcessor = new Client.MessageProcessor() {
        @Override
        public void process(Message m) {

        }
    };
}
