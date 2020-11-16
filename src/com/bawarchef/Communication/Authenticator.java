package com.bawarchef.Communication;

import com.bawarchef.Containers.ChefIdentity;
import com.bawarchef.Clients.Client;
import com.bawarchef.Containers.UserIdentity;
import com.bawarchef.DBConnect;
import com.bawarchef.DBToObject;

import java.sql.ResultSet;
import java.util.ArrayList;

public class Authenticator {

    public static abstract class OnSuccessfulAuthentication{
        public abstract void onSuccess(Client.ClientType clientType);
    }

    public static abstract class OnFailedAuthentication{
        public abstract void onFailure(Client.ClientType clientType);
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

                if(m.getProperty("TYPE").equals("UNREGISTERED")) {
                    if (m.getProperty("CLIENT_TYPE").equals("CHEF"))
                        handleResponseByIDChef(m);
                    else if (m.getProperty("CLIENT_TYPE").equals("USER"))
                        handleResponseByIDUser(m);
                }

                else if(m.getProperty("TYPE").equals("REGISTERED")) {

                    if (m.getProperty("CLIENT_TYPE").equals("CHEF"))
                        handleResponseByUNPChef(m);
                    else if (m.getProperty("CLIENT_TYPE").equals("USER"))
                        handleResponseByUNPUser(m);
                }

            }
            else {
                client.setMessageProcessor(originalMessageProcessor);
                if(m.getProperty("CLIENT_TYPE").equals("CHEF"))
                    onFailedAuthentication.onFailure(Client.ClientType.CHEF);
                else if(m.getProperty("CLIENT_TYPE").equals("USER"))
                    onFailedAuthentication.onFailure(Client.ClientType.USER);
            }
        }

        byte[] arrayxor(byte[] a,byte[] b){
            byte[] t = new byte[a.length];
            for(int i = 0 ; i< t.length; i++)
                t[i] = (byte) (a[i] ^ b[i]);
            return t;
        }

        private void handleResponseByUNPChef(Message m) {
            boolean success = false;
            client.setClientType(Client.ClientType.CHEF);

            DBConnect dbConnect = DBConnect.getInstance();

            ResultSet rs = dbConnect.runFetchQuery("SELECT * from chef_login where loginID = '"+m.getProperty("UNAME")+"' and password = '"+m.getProperty("PWD")+"';");

            String regNo=null;
            try {
                while (rs.next()) {
                    regNo = rs.getString("chefID");
                    client.setUserID(regNo);
                }
            }catch (Exception e){}

            Message sendMsg = new Message(Message.Direction.SERVER_TO_CLIENT,"AUTH_ACK");
            if(regNo==null) {
                sendMsg.putProperty("RESULT", "FAILURE");
            }
            else{
                rs = dbConnect.runFetchQuery("SELECT * from chef_main_table left join chef_profile_table using (chefID) where chefID = '"+regNo+"';");
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
                onSuccessfulAuthentication.onSuccess(Client.ClientType.CHEF);
            }
            else
                onFailedAuthentication.onFailure(Client.ClientType.CHEF);

        }

        private void handleResponseByUNPUser(Message m) {
            boolean success = false;

            client.setClientType(Client.ClientType.USER);

            DBConnect dbConnect = DBConnect.getInstance();

            ResultSet rs = dbConnect.runFetchQuery("SELECT * from user_login where loginID = '"+m.getProperty("UNAME")+"' and password = '"+m.getProperty("PWD")+"';");
            String userID=null;
            try {
                while (rs.next()) {
                    userID = String.valueOf(rs.getInt("userID"));
                    client.setUserID(userID);
                }
            }catch (Exception e){}

            Message sendMsg = new Message(Message.Direction.SERVER_TO_CLIENT,"AUTH_ACK");
            if(userID==null) {
                sendMsg.putProperty("RESULT", "FAILURE");
            }
            else{
                rs = dbConnect.runFetchQuery("select * from user_main_table left join user_profile_table using (userID) left join user_login using (userID) where userID = '"+userID+"';");
                ArrayList<UserIdentity> al = DBToObject.UserMTableToUserIdentity(rs);

                sendMsg.putProperty("RESULT","SUCCESS");
                sendMsg.putProperty("USER_IDENTITY",al.get(0));
                success = true;
            }

            try {
                EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(sendMsg), client.getCrypto_key());
                client.send(ep);
            }catch (Exception e){e.printStackTrace();}

            client.setMessageProcessor(originalMessageProcessor);

            if(success){
                onSuccessfulAuthentication.onSuccess(Client.ClientType.USER);
            }
            else
                onFailedAuthentication.onFailure(Client.ClientType.USER);
        }

        private void handleResponseByIDChef(Message m) {
            boolean success = false;

            client.setClientType(Client.ClientType.CHEF);

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
                onSuccessfulAuthentication.onSuccess(Client.ClientType.CHEF);
            }
            else
                onFailedAuthentication.onFailure(Client.ClientType.CHEF);

        }

        private void handleResponseByIDUser(Message m) {
            boolean success = false;

            client.setClientType(Client.ClientType.USER);

            DBConnect dbConnect = DBConnect.getInstance();
            ArrayList<String> keys = dbConnect.runInsertQueryAndgetKey("INSERT INTO user_main_table(f_name,l_name,mobNo,email) value('"+m.getProperty("FNAME")+"','"+m.getProperty("LNAME")+"','"+m.getProperty("MOB")+"','"+m.getProperty("EMAIL")+"');");

            Message sendMsg = new Message(Message.Direction.SERVER_TO_CLIENT,"AUTH_ACK");

            if(keys.size()==0) {
                sendMsg.putProperty("RESULT", "FAILURE");
            }

            else if(keys.size()==1){
                sendMsg.putProperty("RESULT","SUCCESS");
                for(String s : keys){
                    dbConnect.runInsertQuery("INSERT INTO user_login value('"+s+"','"+m.getProperty("UNAME")+"','"+m.getProperty("PWD")+"');");
                    client.setUserID((String) m.getProperty("UNAME"));
                }
                success = true;
            }
            try {
                EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(sendMsg), client.getCrypto_key());
                client.send(ep);
            }catch (Exception e){e.printStackTrace();}

            client.setMessageProcessor(originalMessageProcessor);

            if(success){
                onSuccessfulAuthentication.onSuccess(Client.ClientType.USER);
                client.closeConnection();
            }
            else
                onFailedAuthentication.onFailure(Client.ClientType.USER);

        }
    };
}
