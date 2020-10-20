package com.bawarchef.Clients;

import com.bawarchef.Communication.Authenticator;
import com.bawarchef.Communication.EncryptedPayload;
import com.bawarchef.Communication.Message;
import com.bawarchef.Communication.MessageQueue;
import com.bawarchef.Preferences;

import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;

public class Client {

    Socket sock;
    ObjectInputStream iStream;
    ObjectOutputStream oStream;

    byte[] crypto_key=null;
    MessageQueue messageQueue;
    MessageProcessor messageProcessor;

    String userID=null;
    ClientType clientType;

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }

    public static enum ClientType{CHEF,USER};

    public static abstract class MessageProcessor{
        public abstract void process(Message m);
    }

    public Client(Socket sock){
        this.sock = sock;
        try {
            this.oStream = new ObjectOutputStream(sock.getOutputStream());
            this.iStream = new ObjectInputStream(sock.getInputStream());
        }catch (Exception e){e.printStackTrace();}
        messageQueue = new MessageQueue();
        messageQueue.setOnMessageArrival(defaultMessageListener);
        messageProcessor = new MessageProcessor() {
            @Override
            public void process(Message m) {
                System.out.println("Handled by netish");
            }
        };
        new Thread(()->{startListening();}).start();

        MessageDigest md5 = null;
        try{ md5 = MessageDigest.getInstance("SHA-256"); }catch (Exception e){}
        crypto_key = md5.digest(Preferences.getInstance().D_KEY_0.getBytes());

        Authenticator authenticator = new Authenticator(this);
        try {
            authenticator.authenticate();
        }catch (Exception e){}
        authenticator.setOnSuccessfulAuthentication(authenticationSuccessful);
        authenticator.setOnFailedAuthentication(authenticationUnsuccessful);
    }

    public void setMessageProcessor(MessageProcessor processor){
        this.messageProcessor = processor;
    }

    public MessageProcessor getMessageProcessor(){
        return messageProcessor;
    }

    private void startListening() {
        while(!sock.isClosed()){
            try {
                EncryptedPayload p = (EncryptedPayload) iStream.readObject();
                Message o = p.getDecryptedPayload(crypto_key);
                messageQueue.addToQueue(o);
            } catch (Exception e) {
                System.out.println("CONNECTION CLOSED !"+e.toString());
                return;
            }
        }
        System.out.println("CONNECTION CLOSED !");
    }

    public byte[] getCrypto_key() {
        return crypto_key;
    }

    public void setCrypto_key(byte[] crypto_key) {
        this.crypto_key = crypto_key;
    }

    Authenticator.OnSuccessfulAuthentication authenticationSuccessful = new Authenticator.OnSuccessfulAuthentication() {
        @Override
        public void onSuccess(ClientType clientType) {
            if(clientType==ClientType.CHEF) {
                System.out.println("SUccESSfully Authenticated");
                ChefClient c = new ChefClient(Client.this);
            }else if(clientType==ClientType.USER) {
                UserClient u = new UserClient(Client.this);
            }

        }
    };

    Authenticator.OnFailedAuthentication authenticationUnsuccessful = new Authenticator.OnFailedAuthentication() {
        @Override
        public void onFailure(ClientType clientType) {
            closeConnection();
        }
    };

    public void closeConnection() {
        try {
            Client.this.sock.close();
        }catch (Exception e){}
    }

    MessageQueue.OnMessageArrivalListener defaultMessageListener = new MessageQueue.OnMessageArrivalListener() {
        @Override
        public void OnArrival() {
            if(messageProcessor==null) {
                try {
                    closeConnection();
                }catch (Exception e){}
                return;
            }
            while(messageQueue.size()>0)
                messageProcessor.process(messageQueue.getLastMessage());
        }
    };

    public void send(EncryptedPayload encryptedPayload){
        try{
            oStream.writeUnshared(encryptedPayload);
            oStream.reset();
        }catch (Exception e){e.printStackTrace();}
    }



}
