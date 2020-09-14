package com.bawarchef.Clients;

import com.bawarchef.Communication.Message;

import java.net.Socket;

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
                System.out.print("UPD PDET REQ RECVD");
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
            else if(m.getMsg_type().equals("")){

            }

            //.....
        }
    };


}
