package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

public class SearchServer2 {

    public void search() throws IOException {
        int port = 9997;
        ServerSocket server = new ServerSocket(port);
        System.out.println("Server started successfully, waiting for user access");

        Socket socket = server.accept();
        System.out.println("Client access, client IP: " + socket.getInetAddress());

        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();

        byte[] bytes = new byte[1024];
        int len = inputStream.read(bytes);

        String id = new String(bytes, 0, 2);
        String name = new String(bytes, 3, len - 3);
        System.out.println("ServerId: " + id + " and author name is " + name + "\n");
        String searchString = "<author>" + name + "</author>";
        String path = SearchServer2.class.getResource("File/dblp.xml"+ id).getFile();
        String commandString = "grep -c '" + searchString + "' " + path;
        String answerString =  Objects.requireNonNull(Helper.exec(commandString)).toString();
        outputStream.write(("server" + id + " returns " + answerString).getBytes());
        socket.close();
    }

    public static void main(String[] args){
        try {
            SearchServer2 searchServer = new SearchServer2();
            searchServer.search();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
