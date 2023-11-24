package view;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class ClientView {
    protected JFrame frame;
    protected JPanel settingPanel,         //Configuration panel
            messagePanel;  	   //Message panel
    protected JSplitPane centerSplitPanel; //Divider panel
    protected JScrollPane userPanel,	   //Left user panel
            messageBoxPanel; //Message box on the right
    protected JTextArea messageTextArea;   //Message edit box
    protected JTextField nameTextField,    //Username input box
            ipTextField;	   //Server IP address input box
    protected JTextField portTextField;	   //Port input box
    protected JTextField messageTextField; //Message edit box
    protected JLabel messageToLabel;       //To.. label
    protected JButton connectButton, 	   //Connect button
            disconnectButton,    //Disconnect button
            sendButton;  		   //Send button
    protected JList userList;  			   //A dynamically changing list of users

    //Model
    protected DefaultListModel<String> listModel;

    //Constructor
    public ClientView() {
        initUI();
    }

    //UI initialization functions
    private void initUI() {

        //Set the client window title, size, and layout
        frame = new JFrame("Client");
        frame.setSize(600, 400);
        frame.setResizable(false);
        frame.setLayout(new BorderLayout());

        //Set the initial parameters of the panel
        ipTextField = new JTextField("127.0.0.1");
        portTextField = new JTextField("5418");
        nameTextField = new JTextField("Set Your Name");
        connectButton = new JButton("Connect");
        disconnectButton = new JButton("Disconnect");

        //Configuration panel
        settingPanel = new JPanel();
        settingPanel.setLayout(new GridLayout(1, 8));  //Set the layout to one row and eight columns
        settingPanel.add(new JLabel("         Name:")); //Add components to the configuration panel
        settingPanel.add(nameTextField);
        settingPanel.add(new JLabel("  ServerIP:"));
        settingPanel.add(ipTextField);
        settingPanel.add(new JLabel("  PostID:"));
        settingPanel.add(portTextField);
        settingPanel.add(connectButton);
        settingPanel.add(disconnectButton);
        settingPanel.setBorder(new TitledBorder("Client Config")); //Set the configuration panel title

        //Online user panel
        listModel = new DefaultListModel<String>();
        userList = new JList(listModel);
        userPanel = new JScrollPane(userList);
        userPanel.setBorder(new TitledBorder("Online Users"));  //Set the online user panel title

        //Receive message panel
        messageTextArea = new JTextArea();
        messageTextArea.setEditable(false);        //Set the area to be non-editable
        messageTextArea.setForeground(Color.blue); //Set the font default color to blue

        messageBoxPanel = new JScrollPane(messageTextArea);   //Set to a text box with a slider
        messageBoxPanel.setBorder(new TitledBorder("Accept Message")); //Set the title

        //Send message component
        messageToLabel = new JLabel("To:All  ");   //The default is to send to everyone
        messageTextField = new JTextField();
        sendButton = new JButton("Send");

        messagePanel = new JPanel(new BorderLayout());  //Place the component on the panel
        messagePanel.add(messageToLabel, "West");
        messagePanel.add(messageTextField, "Center");
        messagePanel.add(sendButton, "East");
        messagePanel.setBorder(new TitledBorder("Send Message"));

        //Combine an intermediate online user panel with a receiving message panel
        centerSplitPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, userPanel, messageBoxPanel);
        centerSplitPanel.setDividerLocation(100);  //Set the divider 100px from the left

        frame.add(settingPanel, "North");
        frame.add(centerSplitPanel, "Center");
        frame.add(messagePanel, "South");
        frame.setVisible(true);

        serviceUISetting(false); //Sets the default state of buttons and text boxes
    }

    public void serviceUISetting(boolean connected) {
        nameTextField.setEnabled(!connected);
        ipTextField.setEnabled(!connected);
        portTextField.setEnabled(!connected);
        connectButton.setEnabled(!connected);
        disconnectButton.setEnabled(connected);
        messageTextField.setEnabled(connected);
        sendButton.setEnabled(connected);
    }
}
