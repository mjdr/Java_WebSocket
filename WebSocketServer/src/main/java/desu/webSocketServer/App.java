package desu.webSocketServer;

import java.io.IOException;

public class App {
    public static void main( String[] args ) {
    	new App();
    }
    public App() {
    	try {
			new Server();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
