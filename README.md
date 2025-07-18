# Simple Chat System

## Overview
The Simple Chat System is a basic chat application built using Java, Java Swing, and Java Sockets. This system allows users to send text messages and files in a client-server architecture. It is developed with NetBeans IDE and utilizes Ant for building the project.

## Features
- **Text Messaging**: Send and receive text messages in real-time.
- **File Transfer**: Share files between clients seamlessly.
- **Client-Server Architecture**: Communicate through a centralized server.
- **User-Friendly Interface**: Built with Java Swing for an intuitive user experience.

## Technologies Used
- **Java**: The primary programming language used for development.
- **Java Swing**: This is used to create the graphical user interface.
- **Java Sockets**: To handle network communication.
- **Ant**: For building the project.
- **NetBeans IDE**: Integrated Development Environment used during development.

## Getting Started

### Prerequisites
- Java Development Kit (JDK) 8 or higher
- NetBeans IDE (optional)
- Ensure the port number isn't busy (default 1207)

### Running the Application

#### Running the Server
1. **Open the Project**: Launch NetBeans (or any Java IDE) and open the Simple Chat System project.
2. **Locate the Server Class**: In the project structure, find the server class (`Server.java`).
3. **Run the Server**: 
   - Right-click on the server class and select "Run File" or use the run option in the menu.
   - The server will start listening for incoming connections on the specified port (default 1207).
4. **Check Console Output**: Monitor the console for any messages indicating successful startup or connection details.

**or easily run jar file (`Server.jar`)**

#### Running the Client
1. **Open the Project**: If not already open, ensure the Simple Chat System project is loaded in the IDE.
2. **Locate the Client Class**: Find the client class (`Client.java`).
4. **Connect to the Server**: 
   - Check the server IP address (use `localhost` if running on the same machine) and the port number (default 1207).
3. **Run the Client**:
   - Right-click on the client class and select "Run File."

**or easily run jar file (`Client.jar`)**

### Usage
- Enter text messages in the input field and press 'Send' to communicate with other users.
- Use the file transfer feature to share files with connected clients.
