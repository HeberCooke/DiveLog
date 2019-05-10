import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class ServerDiveLog extends Application {

	public static void main(String[] args) {
		Application.launch(args);
	}

	private TextArea ta = new TextArea();

	private int clientNo = 0;
	private ServerSocket serverSocket;
	private Socket socket;

	@Override
	public void start(Stage primaryStage) throws Exception {

		// setting the server text area to be non edible
		ta.setEditable(false);
		// Create a scene and place it on the stage
		Scene scene = new Scene(new ScrollPane(ta), 500, 300);
		primaryStage.setTitle("Server");
		primaryStage.setScene(scene);
		primaryStage.show();

		new Thread(() -> {

			try {
				// Create a server socket

				serverSocket = new ServerSocket(8000);
				ta.appendText("Server started at " + new Date() + "\n");

				while (true) {
					// Listen for a new connection
					socket = serverSocket.accept();

					clientNo++; // increasing number of logins

					Platform.runLater(() -> {
						InetAddress inetAddress = socket.getInetAddress();
						ta.appendText("Started thread for client " + clientNo + " at " + new Date() + "\n");
						ta.appendText("Client" + clientNo + "'s IP address is " + inetAddress.getHostAddress() + "\n");

					});
					new Thread(new HandleAClient(socket)).start();

				}

			} catch (IOException ex) {
				ex.printStackTrace();
			}finally {
				try {
					serverSocket.close();// closing the server socket
					socket.close(); // closing the socket
				} catch (IOException e) {
					e.printStackTrace();
				} 
			
			}
		
			
		}).start();

	}

	// Global so each handle event can use them
	public HashMap<String, String> users = new HashMap<>();
	public String name = "";
	public String password = "";
	public boolean isSignUp;
	private ObjectInputStream in;
	private DataInputStream inputFromClient;
	private DataOutputStream outputToClient;

	class HandleAClient implements Runnable {

		private Socket socket;

		public HandleAClient(Socket socket) {
			this.socket = socket;
		}



		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			try { // getting users from file
				in = new ObjectInputStream(new FileInputStream("usersLogin.dat"));

	
				users = (HashMap<String, String>) in.readObject();
				
				
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			} 
			

			try {
				inputFromClient = new DataInputStream(socket.getInputStream());
				outputToClient = new DataOutputStream(socket.getOutputStream());

				// continue serving the client
				while (true) {
					isSignUp = inputFromClient.readBoolean();
					name = inputFromClient.readUTF();
					password = inputFromClient.readUTF();

					// Checking if user signed up or log in
					if (users.containsKey(password) && users.get(password).matches(name) && isSignUp == false) {
						outputToClient.writeUTF("LoginSucsess");
						ta.appendText(name + " logged in\n");
					}

					if (isSignUp == false && !users.containsKey(password)) {
						outputToClient.writeUTF("SignUp");
					}
					if (users.containsKey(password)) { // has to come before the next if statement
						outputToClient.writeUTF("UseDiffPass");
					}

					if (isSignUp == true && !users.containsKey(password)) {
						users.put(password, name);
						outputToClient.writeUTF("LogIn");
					}

					// saving users to file
					try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("usersLogin.dat"));) {
						out.writeObject(users);
					}

				}
				
				
			} catch (IOException ex) {
				ex.printStackTrace();
			} finally {
				try {
					outputToClient.close();
					inputFromClient.close();

				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}
	}
}
