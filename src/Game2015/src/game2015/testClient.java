package Game2015.src.game2015;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class testClient
{
	public static Socket socket;
	
	public static void main(String[] args) {
		
		try {
			socket = new Socket("127.0.0.1", 7777);
			System.out.println("Client connected ");
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}

		try {
			// read text from the socket
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			// write text to the socket
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			out.println("Radi 9 6 up");
			
			System.out.println("wyslane 5 ");
			
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}
}
