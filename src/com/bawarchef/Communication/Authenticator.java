package com.bawarchef.Communication;

import com.bawarchef.Containers.ChefIdentity;
import com.bawarchef.Clients.Client;
import com.bawarchef.DBConnect;
import com.bawarchef.DBToObject;

import java.sql.ResultSet;
import java.util.ArrayList;

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

            if(m.getMsg_type().equals("AUTH->RESPONSE")){

                client.setCrypto_key(arrayxor(client.getCrypto_key(),(byte[])m.getProperty("IDENTITY")));

                if(m.getProperty("TYPE").equals("UNREGISTERED"))
                    handleResponseByID(m);

                else if(m.getProperty("TYPE").equals("REGISTERED"))
                    handleResponseByUNP(m);

            }
            else {
                client.setMessageProcessor(originalMessageProcessor);
                onFailedAuthentication.onFailure();
            }
        }

        byte[] arrayxor(byte[] a,byte[] b){
            byte[] t = new byte[a.length];
            for(int i = 0 ; i< t.length; i++)
                t[i] = (byte) (a[i] ^ b[i]);
            return t;
        }

        private void handleResponseByUNP(Message m) {
            boolean success = false;

            DBConnect dbConnect = DBConnect.getInstance();
            ResultSet rs = dbConnect.runFetchQuery("SELECT * from chef_login where loginID = '"+m.getProperty("UNAME")+"' and password = '"+m.getProperty("PWD")+"';");

            String regNo=null;
            try {
                while (rs.next()) {
                    regNo = rs.getString("chefID");
                }
            }catch (Exception e){}

            Message sendMsg = new Message(Message.Direction.SERVER_TO_CLIENT,"AUTH_ACK");
            if(regNo==null) {
                sendMsg.putProperty("RESULT", "FAILURE");
            }
            else{
                rs = dbConnect.runFetchQuery("SELECT * from chef_main_table where chefID = '"+regNo+"';");
                ArrayList<ChefIdentity> al = DBToObject.ChefMTableToChefIdentity(rs);

                sendMsg.putProperty("RESULT","SUCCESS");
                sendMsg.putProperty("CHEF_IDENTITY",al.get(0));
                success = true;
            }

            try {
                EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(sendMsg), client.getCrypto_key());
                client.send(ep);
            }catch (Exception e){e.printStackTrace();}

            client.setMessageProcessor(originalMessageProcessor);

            if(success){
                onSuccessfulAuthentication.onSuccess();
            }
            else
                onFailedAuthentication.onFailure();

        }


        private void handleResponseByID(Message m) {
            boolean success = false;

            DBConnect dbConnect = DBConnect.getInstance();
            ResultSet rs = dbConnect.runFetchQuery("SELECT * from chef_main_table where chefID = '"+m.getProperty("RegNo")+"';");

            ArrayList<ChefIdentity> al = DBToObject.ChefMTableToChefIdentity(rs);
            Message sendMsg = new Message(Message.Direction.SERVER_TO_CLIENT,"AUTH_ACK");

            if(al.size()==0) {
                sendMsg.putProperty("RESULT", "FAILURE");
            }

            else if(al.size()==1){
                sendMsg.putProperty("RESULT","SUCCESS");
                sendMsg.putProperty("CHEF_IDENTITY",al.get(0));
                success = true;
            }
            try {
                EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(sendMsg), client.getCrypto_key());
                client.send(ep);
            }catch (Exception e){e.printStackTrace();}

            client.setMessageProcessor(originalMessageProcessor);

            if(success){
                onSuccessfulAuthentication.onSuccess();
            }
            else
                onFailedAuthentication.onFailure();

        }
    };
}
