package chatting.system;

import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client extends JFrame {

    private static Socket socket;
    private static DataInputStream din;
    private static DataOutputStream dout;

    private JTextPane msg_area;
    private JTextField msg_text;
    private JButton msg_send;
    private JButton file_send;

    public Client() {
        super("Client Chat");
        initComponents();
        setupNetworking();
    }

    private void initComponents() {
        msg_area = new JTextPane();
        msg_area.setEditable(false);
        msg_area.setEditorKit(new HTMLEditorKit());
        msg_area.setContentType("text/html");
        msg_area.setText("<html><body></body></html>");
        
        JScrollPane jScrollPane1 = new JScrollPane(msg_area);

        msg_text = new JTextField();

        // Create buttons with proper error handling
        msg_send = createIconButton("Send", "send.png");
        file_send = createIconButton("Send File", "file.png");

        // Set tooltips
        msg_send.setToolTipText("Send Message");
        file_send.setToolTipText("Send File");

        msg_send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                msg_sendActionPerformed(evt);
            }
        });

        msg_text.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                msg_sendActionPerformed(evt);
            }
        });

        file_send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                file_sendActionPerformed(evt);
            }
        });

        // Layout setup
        setLayout(new BorderLayout());
        add(jScrollPane1, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(msg_send);
        buttonPanel.add(file_send);
        
        southPanel.add(msg_text, BorderLayout.CENTER);
        southPanel.add(buttonPanel, BorderLayout.EAST);
        add(southPanel, BorderLayout.SOUTH);

        setSize(500, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void appendToChat(String text, String color) {
        try {
            HTMLDocument doc = (HTMLDocument) msg_area.getDocument();
            HTMLEditorKit editorKit = (HTMLEditorKit) msg_area.getEditorKit();
            
            // Escape HTML special characters
            text = text.replace("&", "&amp;")
                      .replace("<", "&lt;")
                      .replace(">", "&gt;")
                      .replace("\n", "<br>");
            
            String styledText = String.format("<span style='color:%s;'>%s</span><br>", color, text);
            
            editorKit.insertHTML(doc, doc.getLength(), styledText, 0, 0, null);
            
            // Scroll to bottom
            msg_area.setCaretPosition(doc.getLength());
        } catch (BadLocationException | IOException e) {
            e.printStackTrace();
            // Fallback to plain text if HTML fails
            msg_area.setText(msg_area.getText() + text + "\n");
        }
    }

    private void msg_sendActionPerformed(ActionEvent evt) {
        try {
            String msgout = msg_text.getText().trim();
            if (!msgout.isEmpty()) {
                dout.writeUTF("MSG:" + msgout);
                appendToChat("You: " + msgout, "blue");
                msg_text.setText("");
            }
        } catch (IOException ex) {
            appendToChat("\n[Error Sending]: " + ex.getMessage(), "red");
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void file_sendActionPerformed(ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            new Thread(() -> {
                try {
                    // First send the file header
                    dout.writeUTF("FILE_START:" + selectedFile.getName());
                    
                    // Send file size
                    dout.writeLong(selectedFile.length());
                    
                    // Then send the file content
                    FileInputStream fis = new FileInputStream(selectedFile);
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        dout.write(buffer, 0, bytesRead);
                    }
                    
                    fis.close();
                    dout.writeUTF("FILE_END");
                    dout.flush();
                    
                    appendToChat("You sent a file: " + selectedFile.getName(), "green");
                } catch (IOException ex) {
                    appendToChat("\n[Error sending file]: " + ex.getMessage(), "red");
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            }).start();
        }
    }

    private void setupNetworking() {
        try {
            appendToChat("Connecting to server...", "black");
            socket = new Socket("localhost", 1201);
            appendToChat("Connected to server: " + socket.getInetAddress().getHostAddress(), "darkgreen");

            din = new DataInputStream(socket.getInputStream());
            dout = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    while (true) {
                        String header = din.readUTF();
                        
                        if (header.startsWith("MSG:")) {
                            // Regular message
                            final String receivedMsg = header.substring(4);
                            appendToChat(receivedMsg, "black");
                        } 
                        else if (header.startsWith("FILE_START:")) {
                            // File transfer start
                            String fileName = header.substring(11);
                            long fileSize = din.readLong();
                            
                            File downloadsDir = new File("downloads");
                            if (!downloadsDir.exists()) downloadsDir.mkdir();
                            
                            File file = new File(downloadsDir, fileName);
                            FileOutputStream fos = new FileOutputStream(file);
                            
                            byte[] buffer = new byte[4096];
                            long remaining = fileSize;
                            int read;
                            
                            while (remaining > 0 && 
                                  (read = din.read(buffer, 0, (int)Math.min(buffer.length, remaining))) != -1) {
                                fos.write(buffer, 0, read);
                                remaining -= read;
                            }
                            
                            fos.close();
                            
                            // Wait for FILE_END marker
                            String endMarker = din.readUTF();
                            if (!"FILE_END".equals(endMarker)) {
                                throw new IOException("File transfer incomplete");
                            }
                            
                            appendToChat("Received file: " + fileName + " (saved in downloads folder)", "green");
                        }
                    }
                } catch (IOException e) {
                    appendToChat("\n[Server disconnected] " + e.getMessage(), "red");
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, "Message Receive Error", e);
                } finally {
                    try {
                        if (din != null) din.close();
                        if (dout != null) dout.close();
                        if (socket != null && !socket.isClosed()) socket.close();
                    } catch (IOException e) {
                        Logger.getLogger(Client.class.getName()).log(Level.SEVERE, "Error closing resources", e);
                    }
                    appendToChat("Client communication ended.", "gray");
                }
            }).start();

        } catch (IOException e) {
            appendToChat("\n[Connection Error]: Could not connect to server. " + e.getMessage(), "red");
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, "Connection Error", e);
        }
    }
    

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Client().setVisible(true);
        });
    }


    // Helper method to create buttons with icons and fallback
private JButton createIconButton(String text, String iconPath) {
    JButton button = new JButton(text); // Start with text button
    
    try {
        ImageIcon icon = loadIcon(iconPath);
        if (icon != null) {
            icon = scaleIcon(icon, 20, 20);
            button.setIcon(icon);
            button.setText(""); // Remove text if icon loaded
        }
    } catch (Exception e) {
        System.err.println("Couldn't load icon--: " + iconPath);
        e.printStackTrace();
        // Keep as text button
    }
    
    // Style the button
    button.setBorderPainted(false);
    button.setContentAreaFilled(false);
    button.setFocusPainted(false);
    
    return button;
}

// Helper method to safely load icons
private ImageIcon loadIcon(String path) {
    try {
        // Get resource URL
        URL imgURL = getClass().getResource(path);
        System.out.println(getClass().getResource(path).toString());
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        }
        // Try alternative loading method
        InputStream imgStream = getClass().getResourceAsStream(path);
        if (imgStream != null) {
            byte[] bytes = imgStream.readAllBytes();
            return new ImageIcon(bytes);
        }
    } catch (Exception e) {
        e.getStackTrace();
        System.err.println("Couldn't load icon**: " + path);
    }
    return null;
}

// Helper method to scale icons
private ImageIcon scaleIcon(ImageIcon icon, int width, int height) {
    Image img = icon.getImage();
    Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    return new ImageIcon(scaledImg);
}
}