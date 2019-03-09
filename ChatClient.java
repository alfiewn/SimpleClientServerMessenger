import java.io.IOException;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Creates a client, reads their name and then message input opens a socket on
 * the required port then starts a client connection thread
 * 
 * @author alfiewn
 *
 */
public class ChatClient {

	private Socket s;
	private ClientConnection cc;
	private String name;
	
	/**
	 * Reads the users name, creates a ClientConnection object then starts it in a
	 * thread. Opens a new socket. While loop listens for input from command line
	 * and outputs to server.
	 *
	 * @param port
	 * 		The port on which to open the socket
	 * @param address
	 * 		The address of the socket
	 */
	public ChatClient(String address, int port) {
		try {

			
			System.out.println("To shutdown the client type EXIT.\nPlease enter your name: ");

			BufferedReader nameInput = new BufferedReader(new InputStreamReader(System.in));
			this.name = nameInput.readLine();

			BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

			this.s = new Socket(address, port);

			this.cc = new ClientConnection(s, this, name);
			Thread t = new Thread(cc);
			t.start();

			this.cc.sendClientName();

			while (true) {

				String message = input.readLine();

				// if the user types exit, call the shutdown method and exit the system
				if (message.equals("EXIT")) {

					this.cc.shutdownClient();
					t.interrupt();
					System.exit(0);
					break;
				}
				this.cc.sendToServer(message);

			}

		} catch (IOException e) {
			System.out.println("Error connecting to server, please check args and try again");
			System.exit(0);
		} 
	}

	/**
	 * accessor method for name
	 * 
	 * @return name
	 * 		Username of the client
	 */		
	public String getName() {
		return name;
	}

	/**
	 * Main method. Reads whether to run in CLI or GUI mode, then either makes an
	 * instance of the class ChatClient or calls the main method of GUIChatClient.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {

			String defaultAddress = "localhost";
			int defaultPort = 14001;
			
			System.out.println("Would you like to run in GUI mode? Y/N");
			
			String answer = new BufferedReader(new InputStreamReader(System.in)).readLine();
			
			boolean repeat = true;

			// repeats until a correct entry has been made
			while (repeat) {
				if (answer.equals("N")) {
					repeat = false;
					
					// check how many args are present
					if (args.length == 2) {
						
						if (args.length == 4) {
							
							// checks if valid args are provided
							if (args[0].startsWith("-cca") && args[2].startsWith("-ccp")) {
								String address = args[1].toString();
								int port = Integer.parseInt(args[3].toString());
								
								new ChatClient(address, port);
							}
						} else {
							
							if (args[0].startsWith("-ccp")) {
								int port = Integer.parseInt(args[1].toString());
								new ChatClient(defaultAddress, port);
							} else if (args[0].startsWith("-cca")) {
								String address = args[1].toString();
								new ChatClient(address, defaultPort);
							}
						}

					} else {
						new ChatClient(defaultAddress, defaultPort);
					}
					
				} else if (answer.equals("Y")) {
					repeat = false;
					GUIChatClient.main(args);
				} else {
					System.out.println("Please enter either Y or N");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch(NumberFormatException i) {
			System.out.print("Port not valid. Please check args input and try again");
			System.exit(0);
		}
	}
}

/**
 * handles the connection of the client to the server, allowing it to be
 * multithreaded
 * 
 * @author alfiewn
 *
 */
class ClientConnection implements Runnable {

	private Socket s;
	private DataInputStream inputFromServer;
	private DataOutputStream outputToServer;
	private String name;

	/**
	 * Constructor method. sets defualt fields.
	 * 
	 * @param socket
	 * 		Socket of the client
	 * @param client
	 * 		An instance of the ChatClient
	 * @param name
	 * 		The username of the client
	 */
	public ClientConnection(Socket socket, ChatClient client, String name) {
		
		this.s = socket;
		this.name = name;
		
		try {
			outputToServer = new DataOutputStream(s.getOutputStream());
			inputFromServer = new DataInputStream(s.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * A method to write the message input by the user to the server.
	 * 
	 * @param message
	 * 		The message to be sent to the server
	 */
	public void sendToServer(String message) {
		try {
			
			// adds the users name to the message
			this.outputToServer.writeUTF("<" + name + "> " + message);
			this.outputToServer.flush();
		} catch (IOException e) {
			System.out.println("Server could not be found. Please try again later");
		}

	}

	/**
	 * A method to shutdown the client. Sends an exit string to the server with the
	 * name of the client to disconnect, closes the socket and exits the program.
	 */
	public void shutdownClient() {
		try {
			this.outputToServer.writeUTF("exit" + name);
			this.s.close();
			
			System.exit(0);

		} catch (IOException e) {
			try {
				this.s.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.exit(0);
		}
	}

	/**
	 * A method to send the name of the client to the serve
	 */
	public void sendClientName() {
		try {
			this.outputToServer.writeUTF("name" + name);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * listens for input from server and prints it when received.
	 */
	public synchronized void run() {

		while (true) {
			
			try {
				System.out.println(inputFromServer.readUTF());
			} catch (IOException e) {
				try {
					this.inputFromServer.close();
					this.outputToServer.close();
				} catch (IOException i) {
					break;
				}
			}
		}

		try {
			this.s.close();
			this.inputFromServer.close();
			this.outputToServer.close();
		} catch (IOException e) {
			e.printStackTrace();

		}
	}
}

/**
 * Creates a GUI and handles input using a text box and button
 * 
 * @author alfiewn
 *
 */
class GUIChatClient {

	private JFrame frame;
	private JTextField txtUserInput;
	private String input;
	private String name;
	private JTextArea textArea;
	private boolean isName;
	private static GUIChatClient instance;
	private GUIClientConnection cc;
	private boolean serverFound = true;

	
	/**
	 * Constructor calls methods to create the GUI elements, and to start the client
	 * 
	 * @param address
	 * 		the address the socket needs to connect to
	 * @param port
	 * 		the port the socket needs to connect to
	 */
	public GUIChatClient(String address, int port) {

		this.startFrame();
		this.startTextField();
		this.startButton();
		this.startTextArea();
		this.showLabel();
		this.handleShutdown();

		this.isName = true;

		this.startClient(address, port);
	}

	/**
	 * Starts the client by outputting a welcome message, then opening a new socket.
	 * Then it creates an instance of the GUIClientConnection class and starts it in
	 * a thread.
	 * 
	 * @param address
	 * 		the address the socket needs to connect to	
	 * @param port
	 * 		the port the socket needs to connect to
	 */
	public void startClient(String address, int port) {

		try {

			this.output("Welcome. Please enter your name:");

			Socket s = new Socket(address, port);

			this.cc = new GUIClientConnection(s, this);
			Thread t = new Thread(cc);
			t.start();

		} catch (IOException e) {
			this.setServerFound(false);
			this.output("Could not find a server. Please exit and try again");
		}

	}

	/**
	 * initialises the frame and sets its characteristics
	 */
	private void startFrame() {
		this.frame = new JFrame();
		this.frame.setBounds(100, 100, 450, 300);
		this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.frame.getContentPane().setLayout(null);
	}

	/**
	 * initialises the text field and sets its characteristics
	 */
	private void startTextField() {
		this.txtUserInput = new JTextField();
		this.txtUserInput.setToolTipText("");
		this.txtUserInput.setBounds(6, 246, 322, 26);
		this.frame.getContentPane().add(txtUserInput);
		this.txtUserInput.setColumns(10);
	}

	/**
	 * initialises the button and sets its characteristics. Defines the action
	 * listener for a button click. Gets input from the text field, checks if it is
	 * a name entry and calls the method to send it to the server.
	 */
	private void startButton() {
		JButton btnSendButton = new JButton("Send");
		btnSendButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				setInput(txtUserInput.getText());
				txtUserInput.setText("");

				if (instance.getIsName()) {
					name = input;
					setInput("name" + name);
					instance.setIsName();
					
					cc.sendToServer(input);
				} else {
					cc.sendToServer("<" + name + "> " + input);
				}

			}
		});
		btnSendButton.setBounds(327, 246, 117, 29);
		this.frame.getContentPane().add(btnSendButton);
	}

	/**
	 * initialises the text area and sets its characteristics
	 */
	private void startTextArea() {
		this.textArea = new JTextArea();
		this.textArea.setEditable(false);
		this.textArea.setBounds(6, 6, 438, 220);
		this.frame.getContentPane().add(textArea);
	}

	/**
	 * initialises the label and sets its characteristics
	 */
	private void showLabel() {
		JLabel lblTypeYourMessage = new JLabel("Type your message here:");
		lblTypeYourMessage.setBounds(6, 228, 173, 16);
		this.frame.getContentPane().add(lblTypeYourMessage);
	}

	/**
	 * sends an exit string to the server when the window is closed
	 */
	private void handleShutdown() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if(getServerFound()) {
					cc.sendToServer("exit" + name);
				}
			}
		});
	}

	/**
	 * sets the IsName variable to false
	 */
	public void setIsName() {
		this.isName = false;
	}

	/**
	 * accessor method for the IsName variable
	 * 
	 * @return Whether the entry is a name
	 */
	public boolean getIsName() {
		return this.isName;
	}

	/**
	 * mutator method for the input variable
	 * 
	 * @param message
	 * 		The message received from the server
	 */
	public void setInput(String message) {
		this.input = message;
	}

	/**
	 * accessor method for the input variable
	 * 
	 * @return The user input
	 */
	public String getInput() {
		return this.input;
	}

	/**
	 * method to output a string to the textArea
	 * 
	 * @param message
	 * 		The message received from the server
	 */
	public void output(String message) {
		this.textArea.append(message + "\n");

	}

	/**
	 * @return whether there is a server running
	 */
	private boolean getServerFound() {
		return this.serverFound;
	}

	/**
	 * 
	 * @param serverFound
	 * 		whether there is a server running
	 */
	private void setServerFound(boolean serverFound) {
		this.serverFound = serverFound;
	}

	/**
	 * main method. Reads the args to see if it should use user defined address and
	 * port. instantiates GUIChatClient with the address and port as parameters
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String defaultAddress = "localhost";
		int defaultPort = 14001;

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				
				try {
					
					// check how many args are present
					if (args.length == 2) {
						if (args.length == 4) {
							
							// checks if valid args are provided
							if (args[0].startsWith("-cca") && args[2].startsWith("-ccp")) {
								String address = args[1].toString();
								int port = Integer.parseInt(args[3].toString());
								instance = new GUIChatClient(address, port);
							}
						} else {
							if (args[0].startsWith("-ccp")) {
								int port = Integer.parseInt(args[1].toString());
								instance = new GUIChatClient(defaultAddress, port);
							} else if (args[0].startsWith("-cca")) {
								String address = args[1].toString();
								instance = new GUIChatClient(address, defaultPort);
							}
						}

					} else {
						instance = new GUIChatClient(defaultAddress, defaultPort);
					}
					
					// makes the frame of the UI visible
					instance.frame.setVisible(true);

				} catch (Exception e) {

				}
			}
		});

	}
}

/**
 * handles the connection of the GUI client to the server, allowing it to be
 * multithreaded
 * 
 * @author alfiewn
 *
 */
class GUIClientConnection implements Runnable {

	private Socket s;
	private GUIChatClient client;
	private DataInputStream inputFromServer;
	private DataOutputStream outputToServer;

	/**
	 * Constructor method. Sets defualt variables.
	 * 
	 * @param s
	 * 		the client socket
	 * @param client
	 * 		an instance of the GUIChatClient
	 */
	public GUIClientConnection(Socket s, GUIChatClient client) {
		
		this.client = client;
		this.s = s;
		
		try {
			outputToServer = new DataOutputStream(s.getOutputStream());
			inputFromServer = new DataInputStream(s.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * method to send a string to the server
	 * 
	 * @param input
	 * 		input received from the user
	 */
	public void sendToServer(String input) {
		try {
			this.outputToServer.writeUTF(input);
			this.outputToServer.flush();
		} catch (IOException e) {
			this.client.output("Server could not be found, please try again later");
		}
	}

	/**
	 * Listens for input from the server and calls the output method when received
	 */
	public synchronized void run() {
		while (true) {
			try {
				String input = inputFromServer.readUTF();

				this.client.output(input);

			} catch (IOException e) {
				try {
					inputFromServer.close();
					outputToServer.close();
				} catch (IOException i) {
					break;
				}
			}
		}
		
		//close the socket and input and output streams
		try {
			this.s.close();
			this.inputFromServer.close();
			this.outputToServer.close();
		} catch (IOException e) {
			e.printStackTrace();

		}
	}
}