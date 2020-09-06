package com.bawarchef.Communication;

import com.bawarchef.Clients.Client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Listener {

    ServerSocket serverSocket=null;
    int port_no;

    public static abstract class OnStartListeningListener{
        public abstract void onStartListening();
    }

    public static abstract class OnStopListeningListener{
        public abstract void onStopListening();
    }

    OnStartListeningListener onStartListeningListener=null;
    OnStopListeningListener onStopListeningListener=null;

    public Listener(int port_no) {
        this.port_no = port_no;
    }

    public void startListening() {
        Thread thd = new Thread(() -> {
            try {
                Listener.this.serverSocket = new ServerSocket(port_no);
                if(onStartListeningListener!=null)
                    onStartListeningListener.onStartListening();
            } catch (IOException e) {
                e.printStackTrace();
            }

            while(true) {
                try {
                    Socket s = serverSocket.accept();
                    new Thread(() -> new Client(s)).start();
                    System.out.println("NEW CONNECTION !");
                } catch (IOException e) {
                    if(onStopListeningListener!=null)
                        onStopListeningListener.onStopListening();
                    return;
                }
            }
        });
        thd.start();
    }

    public void stopListener(){
        try {
            serverSocket.close();
        }catch (IOException e){}
    }

    public void setOnStartListeningListener(OnStartListeningListener onStartListeningListener) {
        this.onStartListeningListener = onStartListeningListener;
    }

    public void setOnStopListeningListener(OnStopListeningListener onStopListeningListener) {
        this.onStopListeningListener = onStopListeningListener;
    }

    public int getPort_no() {
        return port_no;
    }
}
