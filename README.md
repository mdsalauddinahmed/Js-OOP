# Java Socket Chat Application

A simple chat application with file sharing capabilities built using Java Socket Programming.

## Features

- Multi-client chat support
- Real-time text messaging
- File sharing (supports all file types)
- User-friendly GUI interface
- Username-based identification
- Auto-server discovery
- Support for sending/receiving:
  - Text messages
  - Files
  - Images
  - Videos
  - Folders

## How to Run

1. Compile both Java files:
```bash
javac ChatServer.java
javac ChatClient.java
```

2. Start the server first:
```bash
java ChatServer
```

3. Start the client(s):
```bash
java ChatClient
```

## Usage Instructions

1. When you start the client, you'll be prompted to enter your username
2. The main chat window will open with:
   - A chat area showing all messages
   - A text field for typing messages
   - A "Send" button for sending messages
   - A "Send File" button for sharing files

### Sending Messages
- Type your message in the text field
- Press Enter or click the "Send" button

### Sending Files
1. Click the "Send File" button
2. Select the file you want to send
3. The file will be sent to all connected clients
4. Other clients will be prompted to choose where to save the received file

## System Requirements

- Java 8 or higher
- Graphical user interface support
- Network connectivity between server and clients

## Notes

- The server runs on port 5000 by default
- The application uses localhost for local testing
- To connect from different computers, modify the SERVER_ADDRESS in ChatClient.java to the server's IP address 