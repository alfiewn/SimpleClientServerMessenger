import java.io.*;
import java.util.ArrayList;
import java.awt.EventQueue;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.JFrame;
import javax.swing.JTextArea;

/**
 * Sets up a server to receive messages from multiple clients and send to all
 * connected clients
 * 
 * @author alfiewn
 *
 */
public class ChatServer {

	protected ServerSocket ss;
	protected Socket s;
	protected ArrayList<Object> connections = new ArrayList<Object>();
	protected ArrayList<String> clientNames = new ArrayList<String>();

	/**
	 * Opens a new server socket, starts an input thread to read from the command
	 * line. Calls the method to listen for client connections
	 * 
	 * @param port
	 * 		the port of the ServerSocket
	 */
	public ChatServer(int port) {
		
		try {
			this.ss = new ServerSocket(port);

			System.out.printf("Listening for connections on port " + port + "\n");
			System.out.println("To shutdown the server, type EXIT");

			Thread t = new Thread(new ServerInput(this));
			t.start();

			listenForConnections();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ChatServer() {
	}

	/**
	 * A method to close the server socket and exit the program
	 */
	protected void shutdownServer() {

		try {

			this.ss.close();

			System.out.println("Server has been shut down");
			Runtime.getRuntime().halt(0);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * A method to listen for and accept connections from clients. It creates an
	 * instance of ServerConnection each time and adds it to the connections
	 * arraylist. Starts the serverconnection on a new thread.
	 */
	private synchronized void listenForConnections() {
		while (true) {

			try {

				this.s = ss.accept();

				ServerConnection sc = new ServerConnection(s, this);

				Thread t = new Thread(sc);
				this.connections.add(sc);
				t.start();

			} catch (IOException e) {			
				e.printStackTrace();
			}
		}
	}

	/**
	 * an accessor method for the connections arraylist
	 * 
	 * @return The arraylist of current client connections
	 */
	public ArrayList<Object> getConnections() {
		return this.connections;
	}

	/**
	 * adds a new connection to the connections arraylist
	 * 
	 * @param sc
	 * 		An instance of serverconnection
	 */
	public synchronized void addConnection(ServerConnection sc) {
		this.connections.add(sc);
	}

	/**
	 * an accessor method for the client names arraylist
	 * 
	 * @return the arraylist of names of connected clients
	 */
	public ArrayList<String> getClientNames() {
		return this.clientNames;
	}

	/**
	 * adds a new client name to the client names arraylist
	 * 
	 * @param name
	 * 		the username of a connected client
	 */
	public void addClientName(String name) {
		this.clientNames.add(name);
	}

	/**
	 * removes a connection and its name from the corresponding arraylist
	 * 
	 * @param name
	 * 		the username of a connected client
	 */
	public void removeConnection(String name) {
		int position = getClientNames().indexOf(name);
		
		this.connections.remove(position);
		this.clientNames.remove(position);
	}

	/**
	 * Allows the user to choose between CLI and GUI. Checks if the user is using
	 * command line arguments to input their own port. either makes an instance of
	 * the ChatServer class or call the main method from the GUIChatServer.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		try {

			System.out.println("Would you like to run in GUI mode? Y/N");
			String answer = new BufferedReader(new InputStreamReader(System.in)).readLine();
			boolean repeat = true;

			// repeats until a valid input is recieved
			while (repeat) {
				if (answer.equals("N")) {
					repeat = false;
					if (args.length != 0) {
						if (args[0].startsWith("-csp")) {
							int port = Integer.parseInt(args[1].toString());
							new ChatServer(port);
						}
					} else {
						new ChatServer(14001);
					}
				} else if (answer.equals("Y")) {
					repeat = false;
					GUIChatServer.main(args);
				} else {
					System.out.println("Please enter either Y or N");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

/**
 * A class to handle command line input to the server. Allows the user to shut
 * down using EXIT.
 * 
 * @author alfiewn
 *
 */
class ServerInput implements Runnable {

	private ChatServer server;

	/**
	 * Constructor, sets field defualts
	 * 
	 * @param server
	 * 		an instance of the chatserver
	 */
	public ServerInput(ChatServer server) {
		this.server = server;
	}

	/**
	 * Listens for command line input, if it equals the exit command calls the
	 * shutdown server method.
	 */
	public void run() {
		while (true) {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				String input = br.readLine();
				
				if (input.equals("EXIT")) {
					this.server.shutdownServer();
				} else {
					System.out.println("Invalid input, please type EXIT to quit");
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

}

/**
 * Handles the server connection to the client
 * 
 * @author alfiewn
 *
 */
class ServerConnection implements Runnable {

	protected Socket s;
	protected ChatServer server;
	protected DataInputStream inputFromClient;
	protected DataOutputStream outputToClient;
	boolean shouldRun = true;

	public ServerConnection() {
	}

	/**
	 * Constructor method, sets default fields
	 * 
	 * @param s
	 * 		the socket of the server
	 * @param server
	 * 		an instance of the chatserver
	 */
	public ServerConnection(Socket s, ChatServer server) {
		this.s = s;
		this.server = server;
		
		try {
			this.inputFromClient = new DataInputStream(s.getInputStream());
			this.outputToClient = new DataOutputStream(s.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Method to write a message to a single client
	 * 
	 * @param message
	 * 		the message received by the server
	 */
	protected void sendToClient(String message) {
		
		try {
			this.outputToClient.writeUTF(message);
			this.outputToClient.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Method to call the sendToClient method for each client in order to send the
	 * message to all clients
	 * 
	 * @param message
	 * 		the message received by the server
	 */
	private void sendToAllClients(String message) {
		
		for (int i = 0; i < server.getConnections().size(); i++) {
			ServerConnection ch = (ServerConnection) server.getConnections().get(i);
			ch.sendToClient(message);
		}
	}

	/**
	 * Reads input from the client and calls send to all clients with the message.
	 * If the message string begins with exit, call the removeConnection method. If
	 * it is a name, notify all clients a new client has joined. Else output the
	 * message to all clients
	 */
	public void run() {

		try {
			while (shouldRun) {
				
				String message = inputFromClient.readUTF();
				
				if (message.substring(0, 4).equals("exit")) {
					String name = message.substring(4);
					this.server.removeConnection(name);
					sendToAllClients(name + " has left the chat");
					System.out.println("Client disconnected: " + name);
				} else if (message.substring(0, 4).equals("name")) {
					String name = message.substring(4);
					this.server.addClientName(name);
					System.out.println("New client: " + name);
					sendToAllClients(name + " has joined the chat");
				} else {
					System.out.println(message);
					this.sendToAllClients(message);
				}
			}

			this.inputFromClient.close();
			this.outputToClient.close();
			this.s.close();

		} catch (IOException e) {
			//client has disconnected
		}
	}
}

/**
 * Extends ChatServer with additional methods for creating GUI elements and
 * dealing with eventlisteners for input
 * 
 * @author alfiewn
 *
 */
class GUIChatServer extends ChatServer {

	private JFrame frame;
	private JTextArea textArea;
	private static GUIChatServer instance;

	/**
	 * Launch the application, checking for an args entry to change the port
	 */
	public static void main(String[] args) {
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (args.length != 0) {
					if (args[0].startsWith("-csp")) {
						int port = Integer.parseInt(args[1].toString());
						instance = new GUIChatServer(port);

					}
				} else {
					instance = new GUIChatServer(14001);

				}
				instance.frame.setVisible(true);
			}
		});
	}

	/**
	 * Initialises GUI elements, defines shutdown procedure and calls the method to
	 * start the server
	 * 
	 * @param port
	 * 		The  port of the socket
	 */
	private GUIChatServer(int port) {

		super();

		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setBounds(6, 6, 438, 266);
		frame.getContentPane().add(textArea);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					ss.close();
					System.out.println("Server has been shut down");
					Runtime.getRuntime().halt(0);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		this.startServer(port);
	}

	/**
	 * Creates a new server socket, outputs instructions to the user. Creates a new
	 * thread to listen for connections from clients and a new thread to listen
	 * for an exit command from the command line 
	 * 
	 * @param port
	 * 		The port of the socket
	 */
	private void startServer(int port) {
		
		try {
			
			ss = new ServerSocket(port);
			this.appendTextArea("Listening for connections on port " + port + "\n");
			textArea.append("To shutdown the server, close this window\n");

			Thread t = new Thread(new ServerInput(this));
			t.start();
			
			ListenForConnection lfc = new ListenForConnection(this, ss, s);
			Thread t2 = new Thread(lfc);
			t2.start();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method to output to the text area
	 * 
	 * @param input
	 * 		The message input from  the client
	 */
	public void appendTextArea(String input) {
		textArea.append(input);
	}

}

/**
 * Listens for and handles new connections from clients
 * 
 * @author alfiewn
 *
 */
class ListenForConnection implements Runnable {

	private GUIChatServer server;
	private ServerSocket ss;
	private Socket s;

	/**
	 * Constructor method, sets defualt fields
	 * 
	 * @param server
	 * 		An instance of the gui chatserver
	 * @param ss
	 * 		the server socket of the gui chatserver
	 * @param s
	 * 		the socket of the gui chatserver
	 */
	public ListenForConnection(GUIChatServer server, ServerSocket ss, Socket s) {
		this.server = server;
		this.ss = ss;
		this.s = s;
	}

	/**
	 * Listens for connections from new clients, when received, starts a new
	 * connection thread and calls the method to add the connection to the arraylist
	 */
	public synchronized void run() {
		while (!Thread.interrupted()) {
			try {

				this.s = ss.accept();

				GUIServerConnection sc = new GUIServerConnection(s, server);

				Thread t = new Thread(sc);
				server.addConnection(sc);

				t.start();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

/**
 * Extends the ServerConnection method, adding methods to send messages received
 * from a GUI client and listen for messages from a GUI client
 * 
 * @author alfiewn
 *
 */
class GUIServerConnection extends ServerConnection implements Runnable {

	private GUIChatServer server;

	/**
	 * Constructor method, sets default fields
	 * 
	 * @param s
	 * 		The socket of the gui chatserver
	 * @param server
	 * 		An instancae of the gui chatserver
	 */
	public GUIServerConnection(Socket s, GUIChatServer server) {
		
		super();
		
		this.s = s;
		this.server = server;

		try {
			inputFromClient = new DataInputStream(s.getInputStream());
			outputToClient = new DataOutputStream(s.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * A method to call the sendToClient method for each connection to send a
	 * message to all clients
	 * 
	 * @param message
	 * 		The message recieved by the server
	 */
	private void sendToAllClients(String message) {
		
		for (int i = 0; i < server.getConnections().size(); i++) {
			GUIServerConnection ch = (GUIServerConnection) server.getConnections().get(i);
			ch.sendToClient(message);
		}
	}

	/**
	 * Reads input from the client and calls send to all clients with the message.
	 * If the message string begins with exit, call the removeConnection method. If
	 * it is a name, notify all clients a new client has joined. Else output the
	 * message to all clients
	 */
	public synchronized void run() {

		try {
			while (shouldRun) {
				
				String message = inputFromClient.readUTF();
				
				if (message.length() > 4) {
					if (message.substring(0, 4).equals("exit")) {
						String name = message.substring(4);
						server.removeConnection(name);
						server.appendTextArea("Client disconnected: " + name + "\n");
						sendToAllClients(name + " has left the chat");
					} else if (message.substring(0, 4).equals("name")) {
						String name = message.substring(4);
						server.addClientName(name);
						server.appendTextArea("New client: " + name + "\n");
						sendToAllClients(name + " has joined the chat");
					} else {
						server.appendTextArea(message + "\n");
						sendToAllClients(message);
					}
				} else {
					sendToAllClients(message);
				}
			}

			this.inputFromClient.close();
			this.outputToClient.close();
			this.s.close();

		} catch (IOException e) {
			//client has disconnected
		}
	}
}
