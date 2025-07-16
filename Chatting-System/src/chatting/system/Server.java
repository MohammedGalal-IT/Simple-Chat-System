package chatting.system;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

    private static final int PORT = 1201;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static int clientCount = 0;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT + ". Waiting for clients...");

            while (true) {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    if(clientCount >= 2) {
                        clientSocket.close(); // Reject connection if max clients reached
                        break;
                    }
                    clientCount++;
                    System.out.println("Client " + clientCount + " connected from: " + clientSocket.getInetAddress().getHostAddress());

                    ClientHandler clientHandler = new ClientHandler(clientSocket, clientCount);
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();
                }
                System.out.println("Maximum number of clients (2) reached. No more connections will be accepted.");
            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, "Server startup error", ex);
        }
    }

    public static void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public static void broadcastFile(String fileName, byte[] fileData, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendFile(fileName, fileData);
            }
        }
    }

    public static void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        clientCount--;
        System.out.println("Client disconnected. Current clients: " + clientCount);
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private DataInputStream din;
        private DataOutputStream dout;
        private int clientId;

        public ClientHandler(Socket socket, int clientId) {
            this.socket = socket;
            this.clientId = clientId;
            try {
                din = new DataInputStream(socket.getInputStream());
                dout = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                Logger.getLogger(Server.ClientHandler.class.getName()).log(Level.SEVERE, "Error setting up streams for client " + clientId, e);
            }
        }

        public void sendMessage(String message) {
            try {
                dout.writeUTF("MSG:" + message);
                dout.flush();
            } catch (IOException e) {
                Logger.getLogger(Server.ClientHandler.class.getName()).log(Level.SEVERE, "Error sending message to client " + clientId, e);
                closeResources();
            }
        }

        @Override
        public void run() {
            try {
                while (!socket.isClosed()) {
                    String header = din.readUTF();
                    
                    if (header.startsWith("MSG:")) {
                        // Handle regular message
                        String message = header.substring(4);
                        System.out.println("Received from Client " + clientId + ": " + message);
                        broadcastMessage("Client " + clientId + ": " + message, this);
                    } 
                    else if (header.startsWith("FILE_START:")) {
                        // Handle file transfer
                        String fileName = header.substring(11);
                        long fileSize = din.readLong();
                        
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];
                        long remaining = fileSize;
                        int read;
                        
                        while (remaining > 0 && 
                              (read = din.read(buffer, 0, (int)Math.min(buffer.length, remaining))) != -1) {
                            baos.write(buffer, 0, read);
                            remaining -= read;
                        }
                        
                        byte[] fileData = baos.toByteArray();
                        baos.close();
                        
                        // Verify end marker
                        String endMarker = din.readUTF();
                        if (!"FILE_END".equals(endMarker)) {
                            throw new IOException("File transfer incomplete");
                        }
                        
                        System.out.println("Received file from Client " + clientId + ": " + fileName);
                        broadcastFile(fileName, fileData, this);
                    }
                }
            } catch (IOException e) {
                System.out.println("Client " + clientId + " disconnected: " + e.getMessage());
            } finally {
                closeResources();
                removeClient(this);
            }
        }

        public void sendFile(String fileName, byte[] fileData) {
            try {
                // Send file header
                dout.writeUTF("FILE_START:" + fileName);
                // Send file size
                dout.writeLong(fileData.length);
                // Send file data
                dout.write(fileData);
                // Send end marker
                dout.writeUTF("FILE_END");
                dout.flush();
            } catch (IOException e) {
                Logger.getLogger(Server.ClientHandler.class.getName()).log(Level.SEVERE, "Error sending file to client " + clientId, e);
                closeResources();
            }
        }

        private void closeResources() {
            try {
                if (din != null) din.close();
                if (dout != null) dout.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                Logger.getLogger(Server.ClientHandler.class.getName()).log(Level.SEVERE, "Error closing resources for client " + clientId, e);
            }
        }
    }
}