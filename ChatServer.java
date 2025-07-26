import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 5000;
    private static Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);
            System.out.println("Waiting for clients...");

            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("New client connected: " + socket.getInetAddress().getHostAddress());

                    ClientHandler clientHandler = new ClientHandler(socket);
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();
                } catch (IOException e) {
                    System.out.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Could not listen on port " + PORT);
            e.printStackTrace();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static void broadcast(String message, ClientHandler sender) {
        System.out.println("Broadcasting: " + message); // Debug log
        synchronized(clients) {
            Iterator<ClientHandler> iterator = clients.iterator();
            while (iterator.hasNext()) {
                ClientHandler client = iterator.next();
                if (client != sender) {
                    try {
                        if (client.isConnected()) {
                            client.sendMessage(message);
                        } else {
                            iterator.remove();
                            System.out.println("Removed disconnected client");
                        }
                    } catch (Exception e) {
                        System.out.println("Error broadcasting to client: " + e.getMessage());
                        iterator.remove();
                    }
                }
            }
        }
    }

    static void broadcastFile(String fileName, byte[] fileData, ClientHandler sender) {
        System.out.println("Broadcasting file: " + fileName); // Debug log
        synchronized(clients) {
            Iterator<ClientHandler> iterator = clients.iterator();
            while (iterator.hasNext()) {
                ClientHandler client = iterator.next();
                if (client != sender) {
                    try {
                        if (client.isConnected()) {
                            client.sendFile(fileName, fileData);
                        } else {
                            iterator.remove();
                            System.out.println("Removed disconnected client");
                        }
                    } catch (Exception e) {
                        System.out.println("Error broadcasting file to client: " + e.getMessage());
                        iterator.remove();
                    }
                }
            }
        }
    }

    static void removeClient(ClientHandler client) {
        synchronized(clients) {
            clients.remove(client);
            System.out.println("Client removed. Current client count: " + clients.size());
        }
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String username;
    private boolean connected;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.connected = true;
        try {
            dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            connected = false;
        }
    }

    public boolean isConnected() {
        return connected && !socket.isClosed() && socket.isConnected();
    }

    @Override
    public void run() {
        try {
            username = dis.readUTF();
            System.out.println(username + " joined the chat");
            ChatServer.broadcast(username + " joined the chat!", this);

            while (isConnected()) {
                try {
                    String messageType = dis.readUTF();
                    if (messageType.equals("TEXT")) {
                        String message = dis.readUTF();
                        System.out.println(username + ": " + message);
                        ChatServer.broadcast(username + ": " + message, this);
                    } else if (messageType.equals("FILE")) {
                        String fileName = dis.readUTF();
                        int fileSize = dis.readInt();
                        byte[] fileData = new byte[fileSize];
                        dis.readFully(fileData);
                        System.out.println(username + " sent file: " + fileName);
                        ChatServer.broadcast(username + " sent file: " + fileName, this);
                        ChatServer.broadcastFile(fileName, fileData, this);
                    }
                } catch (IOException e) {
                    System.out.println("Error reading from client " + username + ": " + e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
        } finally {
            connected = false;
            try {
                if (username != null) {
                    System.out.println(username + " disconnected");
                    ChatServer.broadcast(username + " left the chat!", this);
                }
                ChatServer.removeClient(this);
                socket.close();
                dis.close();
                dos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        if (!isConnected()) {
            connected = false;
            return;
        }
        try {
            dos.writeUTF("TEXT");
            dos.writeUTF(message);
            dos.flush();
        } catch (IOException e) {
            connected = false;
            System.out.println("Error sending message to " + username + ": " + e.getMessage());
        }
    }

    public void sendFile(String fileName, byte[] fileData) {
        if (!isConnected()) {
            connected = false;
            return;
        }
        try {
            dos.writeUTF("FILE");
            dos.writeUTF(fileName);
            dos.writeInt(fileData.length);
            dos.write(fileData);
            dos.flush();
        } catch (IOException e) {
            connected = false;
            System.out.println("Error sending file to " + username + ": " + e.getMessage());
        }
    }
} 