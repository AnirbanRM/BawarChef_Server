package com.bawarchef.Clients;

import com.bawarchef.Communication.EncryptedPayload;
import com.bawarchef.Communication.Message;
import com.bawarchef.Communication.ObjectByteCode;
import com.bawarchef.Containers.ChefIdentity;
import com.bawarchef.Containers.ChefLogin;
import com.bawarchef.DBConnect;

import java.net.Socket;
import java.sql.ResultSet;

public class ChefClient{

    Client parentClient;
    public ChefClient(Client c) {
        this.parentClient = c;
        c.setMessageProcessor(processor);
    }

    Client.MessageProcessor processor = new Client.MessageProcessor() {
        @Override
        public void process(Message m) {
            if(m.getMsg_type().equals("UPD_PDET")){
                boolean result = updatePDetails(m);
                Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT,"UPD_PDET_RESP");
                if(result) new_m.putProperty("RESULT","SUCCESS");
                else new_m.putProperty("RESULT","FAILURE");
                try {
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}
            }
            else if(m.getMsg_type().equals("UPD_L_DET")){
                boolean result = updateLDetails(m);
                Message new_m = new Message(Message.Direction.SERVER_TO_CLIENT,"UPD_L_DET_RESP");
                if(result) new_m.putProperty("RESULT","SUCCESS");
                else new_m.putProperty("RESULT","FAILURE");
                try {
                    EncryptedPayload ep = new EncryptedPayload(ObjectByteCode.getBytes(new_m), parentClient.getCrypto_key());
                    parentClient.send(ep);
                }catch (Exception e){}

            }
            else if(m.getMsg_type().equals("")){

            }
            else if(m.getMsg_type().equals("")){

            }
            else if(m.getMsg_type().equals("")){

            }
            else if(m.getMsg_type().equals("")){

            }
            else if(m.getMsg_type().equals("")){

            }

            //.....
        }

        private boolean updateLDetails(Message m) {
            ChefLogin cl = (ChefLogin) m.getProperty("IDENTITY");
            if(cl!=null){
                DBConnect con = DBConnect.getInstance();
                String regNo = cl.regNo;
                if(regNo!=null){
                    ResultSet rs = con.runFetchQuery("SELECT * from chef_login where chefID = '"+regNo+"';");
                    try {
                        while (rs.next()) {
                            String exec = "UPDATE chef_login set loginID = '" + cl.uName + "', password = '" + cl.pwd + "' where chefID = '" + cl.regNo + "';";
                            return (con.runManipulationQuery(exec));
                        }
                        String exec = "INSERT into chef_login value('" + cl.regNo + "','" + cl.uName + "','" + cl.pwd + "');";
                        return (con.runInsertQuery(exec));
                    }catch (Exception e){e.printStackTrace(); return false;}
                }
            }
            return false;
        }

        private boolean updatePDetails(Message m) {
            ChefIdentity ci = (ChefIdentity) m.getProperty("IDENTITY");
            if(ci!=null){
                DBConnect con = DBConnect.getInstance();
                String regNo = ci.regNo;
                if(regNo!=null){
                    String manip = "UPDATE chef_main_table set resAddr = '"+ci.resAddr.address+"',resCity = '"+ci.resAddr.city+"', resState = '"+ci.resAddr.state+"', resPin = '"+ci.resAddr.pinNo+"', mailAddr = '"+ci.mailAddr.address+"',mailCity =  '"+ci.mailAddr.city+"', mailState = '"+ci.mailAddr.state+"', mailPin = '"+ci.mailAddr.pinNo+"', mobNo = '"+ci.mob+"', altNo = '"+ci.altmob+"', emerNo = '"+ci.emermob+"', email = '"+ci.email+"' where chefID = '"+regNo+"';";
                    return con.runManipulationQuery(manip);
                }
            }
            return false;
        }
    };


}
