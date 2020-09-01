package com.bawarchef.Clients;

import com.bawarchef.Communication.Authenticator;
import com.bawarchef.Communication.EncryptedPayload;
import com.bawarchef.Communication.Message;
import com.bawarchef.MessageQueue;
import com.bawarchef.Preferences;

import java.io.*;
import java.net.Socket;

public class Client {

    Socket sock;
    ObjectInputStream iStream;
    ObjectOutputStream oStream;

    byte[] crypto_key=null;
    MessageQueue messageQueue;
    MessageProcessor messageProcessor;

    public static abstract class MessageProcessor{
        public abstract void process(Message m);
    }

    public Client(Socket sock){
        this.sock = sock;
        try {
            this.iStream = new ObjectInputStream(sock.getInputStream());
            this.oStream = new ObjectOutputStream(sock.getOutputStream());
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

        crypto_key = Preferences.getInstance().D_KEY_0.getBytes();
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
        while(true){
            try {
                EncryptedPayload p = (EncryptedPayload) iStream.readObject();
                Message o = p.getDecryptedPayload(Preferences.getInstance().D_KEY_0.getBytes());
                messageQueue.addToQueue(o);
            } catch (IOException | ClassNotFoundException | EncryptedPayload.WrongKeyException e) {
                e.printStackTrace();
                try {
                    closeConnection();
                }catch (Exception e2){}
                break;
            }
        }
    }

    public byte[] getCrypto_key() {
        return crypto_key;
    }

    public void setCrypto_key(byte[] crypto_key) {
        this.crypto_key = crypto_key;
    }

    Authenticator.OnSuccessfulAuthentication authenticationSuccessful = new Authenticator.OnSuccessfulAuthentication() {
        @Override
        public void onSuccess() {

        }
    };

    Authenticator.OnFailedAuthentication authenticationUnsuccessful = new Authenticator.OnFailedAuthentication() {
        @Override
        public void onFailure() {
            try {
                closeConnection();
            }catch (Exception e){}
        }
    };

    public void closeConnection() throws Exception{
        Client.this.sock.close();
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
        }catch (Exception e){}
    }



}
