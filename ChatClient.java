import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ChatClient extends JFrame {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5000;
    
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String username;
    private boolean connected;
    
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton fileButton;
    private JPanel mainPanel;
    
    public ChatClient() {
        initializeGUI();
        connectToServer();
    }
    
    private void initializeGUI() {
        setTitle("Chat Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        
        mainPanel = new JPanel(new BorderLayout());
        
        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Bottom panel for input
        JPanel bottomPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Send");
        fileButton = new JButton("Send File");
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(sendButton);
        buttonPanel.add(fileButton);
        
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // Add action listeners
        sendButton.addActionListener(e -> sendMessage());
        fileButton.addActionListener(e -> sendFile());
        messageField.addActionListener(e -> sendMessage());
        
        // Add window listener for cleanup
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
        
        setLocationRelativeTo(null);
    }
    
    private void connectToServer() {
        try {
            username = JOptionPane.showInputDialog(this, "Enter your username:", "Login", JOptionPane.QUESTION_MESSAGE);
            if (username == null || username.trim().isEmpty()) {
                System.exit(0);
            }
            username = username.trim();
            
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            
            dos.writeUTF(username);
            dos.flush();
            
            connected = true;
            chatArea.append("Connected to server!\n");
            
            // Start message receiving thread
            new Thread(this::receiveMessages).start();
            
        } catch (IOException e) {
            showError("Could not connect to server: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private void disconnect() {
        if (connected) {
            try {
                connected = false;
                if (socket != null) socket.close();
                if (dis != null) dis.close();
                if (dos != null) dos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void sendMessage() {
        if (!connected) {
            showError("Not connected to server");
            return;
        }
        
        try {
            String message = messageField.getText().trim();
            if (!message.isEmpty()) {
                dos.writeUTF("TEXT");
                dos.writeUTF(message);
                dos.flush();
                chatArea.append("You: " + message + "\n");
                messageField.setText("");
            }
        } catch (IOException e) {
            showError("Error sending message: " + e.getMessage());
            disconnect();
        }
    }
    
    private void sendFile() {
        if (!connected) {
            showError("Not connected to server");
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                if (file.length() > Integer.MAX_VALUE) {
                    showError("File is too large to send");
                    return;
                }
                
                byte[] fileData = new byte[(int) file.length()];
                FileInputStream fis = new FileInputStream(file);
                fis.read(fileData);
                fis.close();
                
                dos.writeUTF("FILE");
                dos.writeUTF(file.getName());
                dos.writeInt(fileData.length);
                dos.write(fileData);
                dos.flush();
                
                chatArea.append("You sent file: " + file.getName() + "\n");
            } catch (IOException e) {
                showError("Error sending file: " + e.getMessage());
                disconnect();
            }
        }
    }
    
    private void receiveMessages() {
        while (connected) {
            try {
                String messageType = dis.readUTF();
                if (messageType.equals("TEXT")) {
                    String message = dis.readUTF();
                    SwingUtilities.invokeLater(() -> chatArea.append(message + "\n"));
                } else if (messageType.equals("FILE")) {
                    receiveFile();
                }
            } catch (IOException e) {
                if (connected) {
                    showError("Lost connection to server");
                    disconnect();
                }
                break;
            }
        }
    }
    
    private void receiveFile() {
        try {
            String fileName = dis.readUTF();
            int fileSize = dis.readInt();
            byte[] fileData = new byte[fileSize];
            dis.readFully(fileData);
            
            SwingUtilities.invokeLater(() -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(new File(fileName));
                
                int result = fileChooser.showSaveDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    try {
                        File file = fileChooser.getSelectedFile();
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(fileData);
                        fos.close();
                        
                        chatArea.append("Received and saved file: " + fileName + "\n");
                    } catch (IOException e) {
                        showError("Error saving file: " + e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            if (connected) {
                showError("Error receiving file");
                disconnect();
            }
        }
    }
    
    private void showError(String message) {
        SwingUtilities.invokeLater(() -> 
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE)
        );
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            new ChatClient().setVisible(true);
        });
    }
} 