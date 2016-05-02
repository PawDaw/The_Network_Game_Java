package Game2015.src.game2015;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class RunServerThread extends Thread
{

	Main main;
	
	public RunServerThread(Main main) {

		this.main = main;

	}

	@Override
	public synchronized void run() {
		try {
			ServerSocket serversocket = new ServerSocket(7777); // listening on port 77777

			while (true) {
				System.out.println("Waiting");
				
				Socket clientSocket = serversocket.accept(); // accept the port

				CommunicationThread e1 = new CommunicationThread(clientSocket, main);

				e1.start();

			}

		} catch (IOException e) {

			System.err.println("Connection was not made !!!");
			System.err.println(e);

		}

	}
}
