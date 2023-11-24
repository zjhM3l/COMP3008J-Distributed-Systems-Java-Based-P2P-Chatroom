package main;

import model.User;
import view.ClientView;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

public class Client extends ClientView {

    private ArrayList<String> commands = new ArrayList<>();

    //model
    private User me;
    // All online users
    private ConcurrentHashMap<String, User> onlineUsers = new ConcurrentHashMap<String, User>();
    private String sendTarget = "ALL";  //The default send object

    //Socket
    private Socket socket;
    private PrintWriter writer;    //Output stream
    private BufferedReader reader; //Input stream

    // The thread responsible for receiving messages
    private MessageThread messageThread;

    //Status
    private boolean isConnected;   //Determine whether to connect to the server

    //Constructor
    public Client() {

        // The Enter event is pressed in the text box of the written message
        messageTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                send();
            }
        });

        // Event when the Send button is clicked
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                send();
            }
        });

        // Event when the Connect button is clicked
        connectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!isConnected) {
                    connect();
                }
            }
        });

        // Event when the Disconnect button is clicked
        disconnectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isConnected) {
                    disconnect();
                }
            }
        });

        // Event when the window is closed
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (isConnected) {
                    disconnect();
                }
                System.exit(0);
            }
        });

        // Add click events for online users
        userList.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                int index = userList.getSelectedIndex();  //Gets the serial number of the clicked user
                if (index < 0) return;

                if (index == 0) {  //The default is everyone
                    sendTarget = "ALL";
                    messageToLabel.setText("To: All");
                } else {
                    String name = (String)listModel.getElementAt(index);  //Gets the name of the clicked user
                    if (onlineUsers.containsKey(name)) {
                        sendTarget = onlineUsers.get(name).description();
                        messageToLabel.setText("To: " + name);  //Replace To: The label is changed to To username
                    } else {
                        sendTarget = "ALL";
                        messageToLabel.setText("To: All");
                    }
                }
            }
        });
    }

    //connect
    private void connect() {
        int port;

        try {
            port = Integer.parseInt(portTextField.getText().trim());  //Gets the port number
        } catch(NumberFormatException e) {
            showErrorMessage("The port number must be an integer!");
            return;
        }

        if (port < 1024 || port > 65535) {  //Determine whether the port number matches
            showErrorMessage("The port number must be between 1024~65535");
            return;
        }

        String name = nameTextField.getText().trim();  //Gets the user name

        if (name == null || name.equals("")) {  //Determine whether the user name is empty
            showErrorMessage("The name cannot be empty!");
            return;
        }

        String ip = ipTextField.getText().trim();  //Get the IP address

        if (ip == null || ip.equals("")) {  //Determine whether the IP address is empty
            showErrorMessage("The IP address cannot be empty!");
            return;
        }

        try {
            listModel.addElement("All");
            me = new User(name);
            socket = new Socket(ip, port);  //Establish threads based on the specified IP address and port number
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));  //Input stream
            writer = new PrintWriter(socket.getOutputStream());  //Output stream

            String myIP = socket.getLocalAddress().toString().substring(1);  //Obtain the IP address where the client is located
            sendMessage("LOGIN@" + name + "%" + myIP);  //Send user login information

            messageThread = new MessageThread();  //Create a thread that receives messages
            messageThread.start();
            isConnected = true;

        } catch(Exception e) {
            isConnected = false;
            logMessage("Client connection failed");
            listModel.removeAllElements();  //Remove all users from the online panel
            e.printStackTrace();
            return;
        }

        logMessage("The client connection was successful");       //Displays a successful connection message on the message panel
        serviceUISetting(isConnected); //Sets the state of the button
    }

    //Message sending
    private void send() {
        if (!isConnected) {
            showErrorMessage("Not connected to the server!");
            return;
        }
        String message = messageTextField.getText().trim();  //Gets the contents of the send box
        if (message == null || message.equals("")) {
            showErrorMessage("Message cannot be empty!");
            return;
        }

        String to = sendTarget;

        String term1 = message.replace("{", " ");
        String term2 = term1.replace("}", " ");
        String[] commandList = term2.split("_");
        System.out.println(commandList.length);

        if (commandList.length == 2) {
            String commandType = commandList[0];
            String contentInMsg = commandList[1];
            if (commandType.replace(" ", "").equals("BROADCAST")) {
                try {
                    //Send a message to the server
                    //MSG@+"Receiving Message Username %IP Address" + "Sender Username %IP Address" +@+message
                    sendMessage("MSG@" + to + "@" + me.description() + "@" + contentInMsg);
                    logMessage("Me->" + to + ": " + contentInMsg);
                } catch(Exception e) {
                    e.printStackTrace();
                    logMessage("(Failed to send) Me->" + to + ": " + contentInMsg);
                }
            }
        } else if (commandList.length == 1) {
            String commandType = commandList[0];
            if (commandType.replace(" ", "").equals("STOP")) {
                disconnect();
            } else if (commandType.replace(" ", "").equals("LIST")) {
                for (int i=0; i < listModel.getSize(); i++){
                    logMessage("Username: " + listModel.getElementAt(i) + " ID: " + i);
                }
            } else {
                try {
                    //Send a message to the server
                    //MSG@+"Receiving Message Username %IP Address" + "Sender Username %IP Address" +@+message
                    sendMessage("MSG@" + to + "@" + me.description() + "@" + message);
                    logMessage("Me->" + to + ": " + message);
                } catch(Exception e) {
                    e.printStackTrace();
                    logMessage("(Failed to send) Me->" + to + ": " + message);
                }
            }
        } else if (commandList.length == 3) {
            String commandType = commandList[0];
            String commandTarget = commandList[1];
            String contentInMsg = commandList[2];
            if (commandType.replace(" ", "").equals("MESSAGE")) {
                try {
                    to = listModel.getElementAt(Integer.parseInt(commandTarget));
                    //Send a message to the server
                    //MSG@+"Receiving Message Username %IP Address" + "Sender Username %IP Address" +@+message
                    sendMessage("MSG@" + to + "@" + me.description() + "@" + contentInMsg);
                    logMessage("Me->" + to + ": " + contentInMsg);
                } catch(Exception e) {
                    e.printStackTrace();
                    logMessage("(Failed to send) Me->" + to + ": " + contentInMsg);
                }
            }
        }


        messageTextField.setText(null);  //After sending, leave the input box blank
    }

    //Disconnect
    private synchronized void disconnect() {
        try {
            //Send a disconnected message to the server
            sendMessage("LOGOUT");

            messageThread.close();
            listModel.removeAllElements();
            onlineUsers.clear();

            reader.close();
            writer.close();
            socket.close();
            isConnected = false;
            serviceUISetting(false);

            sendTarget = "ALL";
            messageToLabel.setText("To: ALL");

            logMessage("Disconnected...");
        } catch(Exception e) {
            e.printStackTrace();
            isConnected = true;
            serviceUISetting(true);
            showErrorMessage("Server disconnect failed!");
        }
    }

    private void sendMessage(String message) {
        writer.println(message);
        writer.flush();
        commands.add(message);
    }

    private void logMessage(String msg) {
        messageTextArea.append(msg + "\r\n");
    }

    private void showErrorMessage(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    //The thread that received the message
    private class MessageThread extends Thread {
        private boolean isRunning = false;

        public MessageThread() {
            isRunning = true;
        }

        public void run() {
            while (isRunning) {  //Constantly receive messages
                try {
                    String message = reader.readLine();
                    //LOGIN@SUCCESS@Set Your NameConnection to server successful!
                    //MSG@ALL@Set Your Name@1
                    StringTokenizer tokenizer = new StringTokenizer(message, "@");
                    //MSG@ALL@Mel@a(command, to, name, content)
                    String command = tokenizer.nextToken();
                    //MSG

                    if (command.equals("CLOSE")) {
                        logMessage("Server down, disconnecting...");
                        disconnect();
                        isRunning = false;
                        return;
                    } else if (command.equals("STATS")) {
                        logMessage("Server is searching for your history...");
                        for (int i=0; i < commands.size(); i++){
                            logMessage("History: " + commands.get(i));
                        }


                    } else if (command.equals("ERROR")) {
                        String error = tokenizer.nextToken();
                        logMessage("The server returned an error, error type:" + error);
                    } else if (command.equals("LOGIN")) {
                        String status = tokenizer.nextToken();
                        if (status.equals("SUCCESS")) {
                            logMessage("Login successful!" + tokenizer.nextToken());
                        } else if (status.equals("FAIL")) {
                            logMessage("Login failed, disconnected! Cause:" + tokenizer.nextToken());
                            disconnect();
                            isRunning = false;
                            return;
                        }
                    } else if (command.equals("USER")) {
                        String type = tokenizer.nextToken();
                        if (type.equals("ADD")) {
                            String userDescription = tokenizer.nextToken();
                            User newUser = new User(userDescription);
                            onlineUsers.put(newUser.getName(), newUser);
                            listModel.addElement(newUser.getName());

                            logMessage("New user（" + newUser.description() + "）Online!");

                        } else if (type.equals("DELETE")) {
                            String userDescription = tokenizer.nextToken();
                            User deleteUser = new User(userDescription);
                            onlineUsers.remove(deleteUser.getName());
                            listModel.removeElement(deleteUser.getName());

                            logMessage("user（" + deleteUser.description() + "）Outline!");

                            if (sendTarget.equals(deleteUser.description())) {
                                sendTarget = "ALL";
                                messageToLabel.setText("To: All");
                            }

                        } else if (type.equals("LIST")) {
                            int num = Integer.parseInt(tokenizer.nextToken());
                            for (int i = 0; i < num; i++) {
                                String userDescription = tokenizer.nextToken();
                                User newUser = new User(userDescription);
                                onlineUsers.put(newUser.getName(), newUser);
                                listModel.addElement(newUser.getName());

                                logMessage("Get to the user（" + newUser.description() + "）Online！");
                            }
                        }
                    } else if (command.equals("MSG")) {
                        StringBuffer buffer = new StringBuffer();
                        String to = tokenizer.nextToken();
                        String from = tokenizer.nextToken();
                        String content = tokenizer.nextToken();
                        buffer.append(from);
                        if (to.equals("ALL")) {
                            buffer.append("（Mass）");
                        }
                        buffer.append(": " + content);
                        logMessage(buffer.toString());
                    }

                } catch(Exception e) {
                    e.printStackTrace();
                    logMessage("Receive message exception!");
                }
            }
        }

        public void close() {
            isRunning = false;
        }
    }


    // Main function
    public static void main(String args[]){
        new Client();
    }
}
