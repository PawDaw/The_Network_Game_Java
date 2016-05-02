package Game2015.src.game2015;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

import javafx.application.Platform;

public class CommunicationThread extends Thread
{
	private Socket socket;
	private String message;
	private Main main;

	private String instruction;
	private String name;
	private int xpos;
	private int ypos;
	private int points;
	private String direction;
	private int hisLogicalTime;

	private Player tempPlayer; // every thread remember the own parameters

	public CommunicationThread(Socket socket, Main main) {
		this.socket = socket;
		this.main = main;

	}

	@Override
	public void run() {
		try {

			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // read text from Terminal

			while ((message = in.readLine()) != null) {

				// split String by single space
				String[] stream = message.split(" ");
				instruction = stream[0];
				name = stream[1];
				xpos = Integer.parseInt(stream[2]);
				ypos = Integer.parseInt(stream[3]);
				direction = stream[4];
				points = Integer.parseInt(stream[5]);
				hisLogicalTime = Integer.parseInt(stream[6]);

				// update points
				if (this.tempPlayer != null) {
					if (points != this.tempPlayer.getPoints()) {
						tempPlayer.updatePoints(points);
					}
				}
				
				System.out.println("Got Input Stream from : " + message.toUpperCase());

				Platform.runLater(() -> { // Platform.runLater(() to prevent ERROR like this -> java.lang.IllegalStateException: Not on FX
					// application thread; currentThread = Thread-4

					// ------------------------CREATE-------------------------------------
					// create new Player
					if (instruction.equals("create")) {

						Player player = new Player(name, xpos, ypos, direction);
						main.players.add(player);
						tempPlayer = player;
						main.setHeroGrafik(xpos, ypos);

					}

					// ------------------------OK-------------------------------------
					// if received name is equal OK
					else if (instruction.equals("ok")) {

						Main.playerState = PlayerState.BUSY;
						main.LblMessage.setText("State : " + Main.playerState);

						main.counter++;

						// When a process pk receives (m,t,p), it changes its own logical clock: Lk=max(Lk,t)+1.
						// (Here t is the value of Li from the sending process, and p is the process id)
						hisLogicalTime = Math.max(hisLogicalTime, Main.LogicalTime) + 1;
								
								System.out.println("Counter " + main.counter);
								
								if (main.counter == Main.playersIP.length) {
							System.out.println(Main.playersIP.length);

							// split String "main.keyTemp" by single space
							String[] key = main.keyTemp.split(" ");
							xpos = Integer.parseInt(key[0]);
							ypos = Integer.parseInt(key[1]);
							direction = key[2];
							main.playerMoved(xpos, ypos, direction);
							main.UpdateLocation(xpos, ypos, direction);

							System.out.println("moved" + direction);
							main.counter = 0;
						}
					}

					// ------------------------UPDATE-------------------------------------
					// update localization
					else if (instruction.equals("update")) {

						main.foreignPlayerMoved(tempPlayer, xpos, ypos, direction);

					}

					// ------------------------ASK-------------------------------------
					// ask for permission FROM other PLAYERS !!!
					else if (instruction.equals("ask")) {

						// if playerState is IDLE
						if (Main.playerState.equals(PlayerState.IDLE)) {
							main.sendPermission(socket);
						}

						// if playerState is BUSY
						else if (Main.playerState.equals(PlayerState.BUSY)) {
							main.playerQueue.add(socket);
							main.LblMessage.setText("State : " + Main.playerState);
						}

						// if playerState is WAITING
						else if (Main.playerState.equals(PlayerState.WAITING)) {

							main.LblMessage.setText("State : " + Main.playerState);

							if (Main.requestTIME < hisLogicalTime) {
								Main.playerState = PlayerState.IDLE;
								main.playerQueue.add(socket);
							}
							else if (Main.requestTIME > hisLogicalTime) {
								main.sendPermission(socket);
								Main.playerState = PlayerState.IDLE;
								main.LblMessage.setText("State : " + Main.playerState);

							}
							else if (Main.requestTIME == hisLogicalTime) {

								if (Main.me.getName().compareTo(name) > 0) {
									main.sendPermission(socket);
									Main.playerState = PlayerState.IDLE;
									main.LblMessage.setText("State : " + Main.playerState);
								}
								else if (Main.me.getName().compareTo(name) < 0) {
									Main.playerState = PlayerState.IDLE;
									main.playerQueue.add(socket);
								}
							}
						}

					}
				});

			}
		} catch (Exception e) {
			System.err.println("Exception caught: client disconnected.");
			e.printStackTrace();
		}

	}

}
