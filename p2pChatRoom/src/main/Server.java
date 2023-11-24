package main;

import model.User;
import view.ServerView;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

public class Server extends ServerView {
    //Socket
    private ServerSocket serverSocket;

    //Status
    private boolean isStart = false;  //Determine if the server is started
    private int maxClientNum;  //Maximum number of connections

    //Threads
    //ArrayList<ClientServiceThread> clientServiceThreads;
    ConcurrentHashMap<String, ClientServiceThread> clientServiceThreads;
    ServerThread serverThread;

    //Constructor
    public Server() {

        //The broadcast message box is bound to the Enter key
        serverMessageTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendAll();
            }
        });

        //Send a button binding click event
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendAll();
            }
        });

        //The start button binds the click event
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!isStart) {
                    startServer();
                }
            }
        });

        //The stop button binds the click event
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isStart) {
                    stopServer();
                }
            }
        });

        //Bind window close events
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (isStart) {
                    stopServer();
                }
                System.exit(0);
            }
        });
    }

    //Start the server
    private void startServer() {
        int port;

        //Determine whether the port number and the maximum number of people entered meet the specifications
        try {
            port = Integer.parseInt(portTextField.getText().trim());
        } catch(NumberFormatException e) {
            showErrorMessage("The port number must be an integer!");
            return;
        }

        if (port < 1024 || port > 65535) {
            showErrorMessage("The port number must be between 1024~65535");
            return;
        }

        try {
            maxClientNum = Integer.parseInt(maxClientTextField.getText().trim());
        } catch(NumberFormatException e) {
            showErrorMessage("Users Maximum must be positive Int！");
            maxClientNum = 0;
            return;
        }

        if (maxClientNum <= 0) {
            showErrorMessage("Users Maximum must be positive Int！");
            maxClientNum = 0;
            return;
        }

        try {  //Start the server thread with the obtained port number
            clientServiceThreads = new ConcurrentHashMap<String, ClientServiceThread>();
            serverSocket = new ServerSocket(port);
            serverThread = new ServerThread();
            serverThread.start();
            isStart = true;
        } catch (BindException e) {
            isStart = false;
            showErrorMessage("Server can't run：Post being used！");
            return;
        } catch (Exception e) {
            isStart = false;
            showErrorMessage("Server can't run：Error! ");
            e.printStackTrace();
            return;
        }

        logMessage("Server run：Maximum Users：" + maxClientNum + " PostID：" + port);
        serviceUISetting(true);
    }

    private synchronized void stopServer() {
        try {
            serverThread.closeThread();
            //Disconnect all clients
            for (Map.Entry<String, ClientServiceThread> entry : clientServiceThreads.entrySet()) {
                ClientServiceThread clientThread = entry.getValue();
                clientThread.sendMessage("CLOSE");
                clientThread.close();
            }

            clientServiceThreads.clear();
            listModel.removeAllElements();
            isStart = false;
            serviceUISetting(false);
            logMessage("Server close！");
        } catch(Exception e) {
            e.printStackTrace();
            showErrorMessage("Server can't close！");
            isStart = true;
            serviceUISetting(true);
        }
    }

    private void sendAll() {
        if (!isStart) {
            showErrorMessage("Server not start yet，Can't send msg！");
            return;
        }

        if (clientServiceThreads.size() == 0) {
            showErrorMessage("No user online，Can't send msg！");
            return;
        }

        String message = serverMessageTextField.getText().trim();
        if (message == null || message.equals("")) {
            showErrorMessage("Msg can't be blank!");
            return;
        }

        for (Map.Entry<String, ClientServiceThread> entry : clientServiceThreads.entrySet()) {
            entry.getValue().sendMessage("MSG@ALL@SERVER@" + message);
        }

        logMessage("Server: " + message);
        serverMessageTextField.setText(null);
    }

    private void logMessage(String msg) {
        logTextArea.append(msg + "\r\n");
    }

    private void showErrorMessage(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    //Server Thread class
    private class ServerThread extends Thread {
        private boolean isRunning;

        public ServerThread() {
            this.isRunning = true;
        }

        public void run() {
            while (this.isRunning) {
                try {
                    if (!serverSocket.isClosed()) {  //Receive a connection request from a client
                        Socket socket = serverSocket.accept();

                        if (clientServiceThreads.size() == maxClientNum) {  //Determine if the maximum number of people has been reached
                            PrintWriter writer = new PrintWriter(socket.getOutputStream());
                            writer.println("LOGIN@FAIL@Sorry, the server has reached the limit of online capacity, please try it later！");
                            writer.flush();
                            writer.close();
                            socket.close();
                        } else {
                            ClientServiceThread clientServiceThread = new ClientServiceThread(socket);
                            User user = clientServiceThread.getUser();
                            clientServiceThreads.put(user.description(), clientServiceThread);
                            listModel.addElement(user.getName());
                            logMessage(user.description() + "Online...");

                            clientServiceThread.start();
                        }
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public synchronized void closeThread() throws IOException {
            this.isRunning = false;
            serverSocket.close();
            System.out.println("serverSocket close!!!");
        }
    }

    //Client Thread class
    private class ClientServiceThread extends Thread {
        private Socket socket;
        private User user;
        private BufferedReader reader;
        private PrintWriter writer;
        private boolean isRunning;

        private synchronized boolean init() {
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream());

                String info = reader.readLine();
                StringTokenizer tokenizer = new StringTokenizer(info, "@");
                String type = tokenizer.nextToken();
                if (!type.equals("LOGIN")) {
                    sendMessage("ERROR@MESSAGE_TYPE");
                    return false;
                }

                user = new User(tokenizer.nextToken());
                sendMessage("LOGIN@SUCCESS@" + user.description() + "Connection to server successful!");

                int clientNum = clientServiceThreads.size();
                if (clientNum > 0) {
                    //Tell the client who else is online
                    StringBuffer buffer = new StringBuffer();
                    buffer.append("@");
                    for (Map.Entry<String, ClientServiceThread> entry : clientServiceThreads.entrySet()) {
                        ClientServiceThread serviceThread = entry.getValue();
                        buffer.append(serviceThread.getUser().description() + "@");
                        //Tell other users that this user is online
                        serviceThread.sendMessage("USER@ADD@" + user.description());
                    }
                    //list
                    sendMessage("USER@LIST@" + clientNum + buffer.toString());
                }

                return true;

            } catch(Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        public ClientServiceThread(Socket socket) {
            this.socket = socket;
            this.isRunning = init();
            if (!this.isRunning) {
                logMessage("Service thread failed to start!");
            }
        }

        public void run() {
            while (isRunning) {
                try {
                    String message = reader.readLine();
                    //MSG@ALL@Mel@aaaa
                    if (message.equals("LOGOUT")) {
                        logMessage(user.description() + "Logout...");

                        int clientNum = clientServiceThreads.size();

                        //Tell other users that the user is offline
                        for (Map.Entry<String, ClientServiceThread> entry : clientServiceThreads.entrySet()) {
                            entry.getValue().sendMessage("USER@DELETE@" + user.description());
                        }

                        //Remove the user and the server thread
                        listModel.removeElement(user.getName());
                        clientServiceThreads.remove(user.description());

                        // System.out.println(user.description() + " logout, now " + listModel.size() + " client(s) online...(" + clientServiceThreads.size() + " Thread(s))");

                        close();
                        return;
                    } else {  //Send a message
                        dispatchMessage(message);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void dispatchMessage(String message) {
            StringTokenizer tokenizer = new StringTokenizer(message, "@");
            String type = tokenizer.nextToken();
            if (!type.equals("MSG")) {
                sendMessage("ERROR@MESSAGE_TYPE");
                return;
            }

            String to = tokenizer.nextToken();
            String from = tokenizer.nextToken();
            String content = tokenizer.nextToken();

            logMessage(from + "->" + to + ": " + content);
            if (to.equals("ALL")) {
                //send to everyone
                for (Map.Entry<String, ClientServiceThread> entry : clientServiceThreads.entrySet()) {
                    entry.getValue().sendMessage(message);
                }
            } else {
                //Send to a person
                if (clientServiceThreads.containsKey(to)) {
                    clientServiceThreads.get(to).sendMessage(message);
                } else {
                    sendMessage("ERROR@INVALID_USER");
                }
            }
        }

        public void close() throws IOException {
            this.isRunning = false;
            this.reader.close();
            this.writer.close();
            this.socket.close();

        }

        public void sendMessage(String message) {
            System.out.println(message);
            String[] slices = message.split("@");
            if (slices.length == 4) {
                String content = slices[3];
                String term1 = content.replace("{", " ");
                String term2 = term1.replace("}", " ");
                String term3 = term2.replace(" ", "");
                String[] commandList = term3.split("_");
                if (commandList.length == 2) {
                    String command = commandList[0];
                    int id = Integer.parseInt(commandList[1]);
                    if (command.equals("KICK")) {
                        for (Map.Entry<String, ClientServiceThread> entry : clientServiceThreads.entrySet()) {
                            String name = entry.getKey();
                            ClientServiceThread clientThread = entry.getValue();
                            if (name.equals(listModel.getElementAt(id))) {
                                clientThread.sendMessage("CLOSE");
                                try {
                                    clientThread.close();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                listModel.remove(id);
                                logMessage("Kick ID" + id + "successfully!");
                            }
                        }
                    } else if (command.equals("STATS")) {
                        for (Map.Entry<String, ClientServiceThread> entry : clientServiceThreads.entrySet()) {
                            String name = entry.getKey();
                            ClientServiceThread clientThread = entry.getValue();
                            if (name.equals(listModel.getElementAt(id))) {
                                clientThread.sendMessage("STATS");
                                logMessage("Searching for history of ID" + id + "successfully!");
                            } else {
                                logMessage("Searching for history of ID" + id + "Failed");
                            }
                        }

                    } else {
                        writer.println(message);
                        writer.flush();
                    }
                } else {
                    writer.println(message);
                    writer.flush();
                }
            } else {
                writer.println(message);
                writer.flush();
            }
        }

        public User getUser() {
            return user;
        }
    }

    //客户端主函数
    public static void main(String args[]) {
        new Server();
    }
}
