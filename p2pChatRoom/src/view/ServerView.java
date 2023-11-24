package view;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class ServerView {
    protected JFrame frame;
    protected JPanel settingPanel,                //Configuration panel
            messagePanel;  			  //Message panel
    protected JSplitPane centerSplitPanel;        //Divider panel
    protected JScrollPane userPanel,              //Left user panel
            logPanel;               //Message box on the right
    protected JTextArea logTextArea;              //Server logs
    protected JTextField maxClientTextField, 	  //Maximum number of people
            portTextField;           //Port number
    protected JTextField serverMessageTextField;  //Broadcast message input box
    protected JButton startButton, 				  //Start button
            stopButton, 				  //Stop button
            sendButton;  				  //Send button
    protected JList userList;                     //A dynamically changing list of users

    //Model
    protected DefaultListModel<String> listModel;

    //Constructor
    public ServerView() {
        initUI();
    }

    //UI initialization functions
    @SuppressWarnings("unchecked")
    private void initUI() {

        //Set the server window title, default size, and layout
        frame = new JFrame("Server");
        frame.setSize(600, 400);
        frame.setResizable(false);
        frame.setLayout(new BorderLayout());

        //Server Configuration Panel (Set Default Parameters)
        maxClientTextField = new JTextField("10");
        portTextField = new JTextField("5418");
        startButton = new JButton("Run");
        stopButton = new JButton("Stop");

        settingPanel = new JPanel();
        settingPanel.setLayout(new GridLayout(1, 6));  //Set the layout to one row and six columns
        settingPanel.add(new JLabel("Maximum Users"));
        settingPanel.add(maxClientTextField);
        settingPanel.add(new JLabel("PostID"));
        settingPanel.add(portTextField);
        settingPanel.add(startButton);
        settingPanel.add(stopButton);
        settingPanel.setBorder(new TitledBorder("Server Config"));  //Set the title

        //Online user panel
        listModel = new DefaultListModel<String>();

        userList = new JList(listModel);
        userPanel = new JScrollPane(userList);
        userPanel.setBorder(new TitledBorder("Online Users"));

        //Server logs panel
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.setForeground(Color.blue);  //Set the default font color to blue

        logPanel = new JScrollPane(logTextArea);
        logPanel.setBorder(new TitledBorder("Server Logs"));

        //Send message component
        serverMessageTextField = new JTextField();
        sendButton = new JButton("Send");

        messagePanel = new JPanel(new BorderLayout());
        messagePanel.add(serverMessageTextField, "Center");
        messagePanel.add(sendButton, "East");
        messagePanel.setBorder(new TitledBorder("BroadCast"));


        //Combine an intermediate online user panel with a receiving message panel
        centerSplitPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, userPanel, logPanel);
        centerSplitPanel.setDividerLocation(100);  //Set the divider 100px from the left

        frame.add(settingPanel, "North");
        frame.add(centerSplitPanel, "Center");
        frame.add(messagePanel, "South");
        frame.setVisible(true);

        serviceUISetting(false);  //Sets the default state of buttons and text boxes
    }

    protected void serviceUISetting(boolean started) {
        maxClientTextField.setEnabled(!started);
        portTextField.setEnabled(!started);
        startButton.setEnabled(!started);
        stopButton.setEnabled(started);
        serverMessageTextField.setEnabled(started);
        sendButton.setEnabled(started);
    }
}
