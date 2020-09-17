import com.bawarchef.Communication.Listener;
import com.bawarchef.DBConnect;
import com.bawarchef.Preferences;

public class Main {

    static Preferences p=null;

    public static void main(String[] args){
        System.out.println("STARTING...");
        p = Preferences.getInstance();
        DBConnect.getInstance();

        Listener l = new Listener(p.PORT0);

        l.setOnStartListeningListener(new Listener.OnStartListeningListener() {
            @Override
            public void onStartListening() {
                System.out.println("Started Listening on port. "+l.getPort_no());
            }
        });

        l.setOnStopListeningListener(new Listener.OnStopListeningListener() {
            @Override
            public void onStopListening() {
                System.out.println("ListeningStopped");
            }
        });

        l.startListening();
    }


}
