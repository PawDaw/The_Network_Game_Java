package Game2015.src.game2015;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Main extends Application
{

	public static final int size = 20;
	public static final int scene_height = size * 20 + 120;
	public static final int scene_width = size * 20 + 200;

	public static Image image_floor;
	public static Image image_wall;
	public static Image hero_right, hero_left, hero_up, hero_down;

	public static Player me;
	public List<Player> players = new ArrayList<Player>();

	public static String playersIP[] =
	{ "192.168.1.17" };

	public Label[][] fields;
	private TextArea scoreList;
	public Label LblMessage;
	private Button btnConnect, btnConnect_2;

	static int id = 1;
	static int conNr = 1;
	public static Socket socket;
	public String keyTemp;
	public boolean EnableKeys = true;
	public int counter = 0;

	public static PlayerState playerState = PlayerState.IDLE;
	
	public static int LogicalTime = 0;
	public static int requestTIME;
	private static Object lock;
	
	public ArrayList<Socket> playerQueue = new ArrayList<>();
	public ArrayList<Socket> playersSockets = new ArrayList<>();
	
	private String[] board =
	{ // 20x20
	"wwwwwwwwwwwwwwwwwwww", "w        ww        w", "w w  w  www w  w  ww", "w w  w   ww w  w  ww", "w  w               w", "w w w w w w w  w  ww",
			"w w     www w  w  ww", "w w     w w w  w  ww", "w   w w  w  w  w   w", "w     w  w  w  w   w", "w ww ww        w  ww",
			"w  w w    w    w  ww", "w        ww w  w  ww", "w         w w  w  ww", "w        w     w  ww", "w  w              ww",
			"w  w www  w w  ww ww", "w w      ww w     ww", "w   w   ww  w      w", "wwwwwwwwwwwwwwwwwwww" };

	// -------------------------------------------
	// | Maze: (0,0) | Score: (1,0) |
	// |-----------------------------------------|
	// | boardGrid (0,1) | scorelist |
	// | | (1,1) |
	// -------------------------------------------

	@Override
	public void start(Stage primaryStage) {
		try {
			GridPane grid = new GridPane();
			grid.setHgap(10);
			grid.setVgap(10);
			grid.setPadding(new Insets(0, 10, 0, 10));

			btnConnect = new Button("Run Server");
			grid.add(btnConnect, 0, 4);
			btnConnect.setOnAction(event -> RunServer());

			btnConnect_2 = new Button("connect");
			grid.add(btnConnect_2, 1, 4);
			btnConnect_2.setOnAction(event -> Connect());
			btnConnect_2.setDisable(true);

			// LblMessage = new Label("Info message will be shown here...");
			LblMessage = new Label("HEJ");
			grid.add(LblMessage, 0, 5);

			LblMessage.setStyle("-fx-text-fill: red");

			Text mazeLabel = new Text("Maze:");
			mazeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

			Text scoreLabel = new Text("Score:");
			scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

			scoreList = new TextArea();

			GridPane boardGrid = new GridPane();

			image_wall = new Image(getClass().getResourceAsStream("Image/wall4.png"), size, size, false, false);
			image_floor = new Image(getClass().getResourceAsStream("Image/floor1.png"), size, size, false, false);

			hero_right = new Image(getClass().getResourceAsStream("Image/heroRight.png"), size, size, false, false);
			hero_left = new Image(getClass().getResourceAsStream("Image/heroLeft.png"), size, size, false, false);
			hero_up = new Image(getClass().getResourceAsStream("Image/heroUp.png"), size, size, false, false);
			hero_down = new Image(getClass().getResourceAsStream("Image/heroDown.png"), size, size, false, false);

			fields = new Label[20][20];
			for (int j = 0; j < 20; j++) {
				for (int i = 0; i < 20; i++) {
					switch (board[j].charAt(i)) {
						case 'w':
							fields[i][j] = new Label("", new ImageView(image_wall));
							break;
						case ' ':
							fields[i][j] = new Label("", new ImageView(image_floor));
							break;
						default:
							throw new Exception("Illegal field value: " + board[j].charAt(i));
					}
					boardGrid.add(fields[i][j], i, j);
				}
			}
			scoreList.setEditable(false);

			grid.add(mazeLabel, 0, 0);
			grid.add(scoreLabel, 1, 0);
			grid.add(boardGrid, 0, 1);
			grid.add(scoreList, 1, 1);

			Scene scene = new Scene(grid, scene_width, scene_height);
			primaryStage.setScene(scene);
			primaryStage.show();

			scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {

				if (playerState.equals(PlayerState.IDLE)) {
					switch (event.getCode()) {
						case UP:
							
							keyTemp = "0 -1 up";
							askForPermission();
							System.out.println("UP");
							break;
						case DOWN:
							
							keyTemp = "0 +1 down";
							askForPermission();
							System.out.println("down");
							break;
						case LEFT:
							
							keyTemp = "-1 0 left";
							askForPermission();
							System.out.println("Left");
							break;
						case RIGHT:
							
							keyTemp = "+1 0 right";
							askForPermission();
							System.out.println("Right");
							break;
						default:
							break;
					}
				}
			});
			
			// Setting up standard players

			me = new Player("pawel", 8, 4, "up");
			players.add(me);
			fields[8][4].setGraphic(new ImageView(hero_up));
			LblMessage.setText(" Hej  " + me.getName().toUpperCase().toString());
			
			scoreList.setText(getScoreList());

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void playerMoved(int delta_x, int delta_y, String direction) {
		
		playerState = PlayerState.BUSY;
		LblMessage.setText("State : " + Main.playerState);
		
		me.direction = direction;
		
		int x = me.getXpos(), y = me.getYpos();

		if (board[y + delta_y].charAt(x + delta_x) == 'w') {
			me.addPoints(-1);
		}
		else {
			Player p = getPlayerAt(x + delta_x, y + delta_y);
			if (p != null) {
				me.addPoints(10);
				p.addPoints(-10);
			}
			else {
				me.addPoints(1);

				fields[x][y].setGraphic(new ImageView(image_floor));
				x += delta_x;
				y += delta_y;

				if (direction.equals("right")) {
					fields[x][y].setGraphic(new ImageView(hero_right));
				}
				
				if (direction.equals("left")) {
					fields[x][y].setGraphic(new ImageView(hero_left));
				}
				
				if (direction.equals("up")) {
					fields[x][y].setGraphic(new ImageView(hero_up));
				}
				
				if (direction.equals("down")) {
					fields[x][y].setGraphic(new ImageView(hero_down));
				}

				me.setXpos(x);
				me.setYpos(y);
			}
		}

		scoreList.setText(getScoreList());
		playerState = PlayerState.IDLE;
		LblMessage.setText("State : " + Main.playerState);
	}

	public void foreignPlayerMoved(Player player, int delta_x, int delta_y, String direction) {
		player.direction = direction;
		int x = player.getXpos(), y = player.getYpos();

		Player p = getPlayerAt(x + delta_x, y + delta_y);
		if (p != null) {
			
			p.addPoints(-10);
		}
		if (board[y + delta_y].charAt(x + delta_x) != 'w' && p == null) {

			fields[x][y].setGraphic(new ImageView(image_floor));
			x += delta_x;
			y += delta_y;

			if (direction.equals("right")) {
				fields[x][y].setGraphic(new ImageView(hero_right));
			}

			if (direction.equals("left")) {
				fields[x][y].setGraphic(new ImageView(hero_left));
			}

			if (direction.equals("up")) {
				fields[x][y].setGraphic(new ImageView(hero_up));
			}

			if (direction.equals("down")) {
				fields[x][y].setGraphic(new ImageView(hero_down));
			}

			player.setXpos(x);
			player.setYpos(y);
		}
		scoreList.setText(getScoreList());
	}

	public String getScoreList() {
		StringBuffer b = new StringBuffer(100);
		for (Player p : players) {
			b.append(p + "\r\n");
		}
		return b.toString();
	}

	public Player getPlayerAt(int x, int y) {
		for (Player p : players) {
			if (p.getXpos() == x && p.getYpos() == y)
				return p;
		}
		return null;
	}

	public void setHeroGrafik(int x, int y) {
		fields[x][y].setGraphic(new ImageView(hero_up));
		scoreList.setText(getScoreList());
	}

	/*
	 * Run ServerSocket until connected players ( amount declared in playersAmount)
	 */
	private void RunServer() {

		RunServerThread t = new RunServerThread(this);
		t.start();
		btnConnect.setDisable(true);
		btnConnect_2.setDisable(false);
	}

	/*
	 * Connect to few SocketServer(players) and send my location
	 */

	private void Connect() {
		btnConnect_2.setDisable(true);
		
		for (String ip : playersIP) { // array with players IPs
		
			try {
				// TimeUnit.SECONDS.sleep(1);
				socket = new Socket(ip, 7777);
				System.out.println("Client connected : " + socket.getInetAddress());

				playersSockets.add(socket);

				// write text to the socket
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

				// merging few Strings+Space with name,x,y,direction
				String myLocationString = "create" + " " + me.getName() + " " + me.getXpos() + " " + me.getYpos() + " " + me.getDirection() + " "
						+ me.getPoints() + " " + LogicalTime;
				out.println(myLocationString);
				// out.println("pawel 9 4 up");

				System.out.println("SENT MY location to the Server!!!");

			} catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public synchronized void UpdateLocation(int delta_x, int delta_y, String direction) {

		for (Socket s : playersSockets) {
			try {

				// write text to the socket
				PrintWriter out = new PrintWriter(s.getOutputStream(), true);

				// merging few Strings+Space with name,x,y,direction

				String myLocationString = "update" + " " + me.getName() + " " + delta_x + " " + delta_y + " " + direction + " " + me.getPoints()
						+ " " + LogicalTime;
				out.println(myLocationString);
				
			} catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private synchronized void askForPermission() {

		LogicalTime++;
		requestTIME = LogicalTime;
		playerState = PlayerState.WAITING;
		LblMessage.setText("State : " + playerState);

		for (Socket s : playersSockets) {
			try {
				
				// write text to the socket
				PrintWriter out = new PrintWriter(s.getOutputStream(), true);
				
				// merging few Strings+Space with name,x,y,direction
				
				String myLocationString = "ask" + " " + me.getName() + " " + me.getXpos() + " " + me.getYpos() + " " + me.getDirection() + " "
						+ me.getPoints() + " " + LogicalTime;
				
				out.println(myLocationString);

			} catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public synchronized void sendPermission(Socket S) {

		try {
			if (!playersSockets.isEmpty()) {
				
				for (Socket s : playersSockets) {
					if (S.getInetAddress().equals(s.getInetAddress())) {
						
						// write text to the socket
						PrintWriter out = new PrintWriter(s.getOutputStream(), true);
						
						// merging few Strings+Space with name,x,y,direction
						String myLocationString = "ok" + " " + me.getName() + " " + me.getXpos() + " " + me.getYpos() + " " + me.getDirection() + " "
								+ me.getPoints() + " " + LogicalTime;
						out.println(myLocationString);
						
						System.out.println("Sent Permission to:" + s.getInetAddress() + " from PAWEL : " + " logical Time " + LogicalTime);

					}

					if (playerQueue != null) {
						// for (Socket Queue : playerQueue)
						for (int i = 0; i < playerQueue.size(); i++) {
							if (playerQueue.get(i).getInetAddress().equals(s.getInetAddress())) {
								
								// write text to the socket
								PrintWriter out = new PrintWriter(s.getOutputStream(), true);
								
								// merging few Strings+Space with name,x,y,direction
								String myLocationString = "ok" + " " + me.getName() + " " + me.getXpos() + " " + me.getYpos() + " "
										+ me.getDirection() + " " + me.getPoints() + " " + LogicalTime;
								out.println(myLocationString);
								
								System.out.println("Sent Permission to:" + s.getInetAddress() + " from PAWEL : " + " logical Time " + LogicalTime);
								playerQueue.remove(i);
							}
						}
					}
				}
			}

		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Application.launch(args);

	}
}