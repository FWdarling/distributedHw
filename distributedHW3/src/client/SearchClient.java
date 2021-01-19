package client;

import server.SearchServer;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class SearchClient {

    public Map<String, String> map;
    public Boolean lock = false;

    public void search(String ip, int port, String name, String id) throws IOException {
        Socket socket = new Socket(ip, port);
        new Thread(new Task(socket, name, id)).start();
        //System.out.println("Successfully connected to the server" + id);
        //System.out.println("ip: " + ip + "and port: " + port);
    }

    class Task implements Runnable{
        private final Socket socket;
        private final String name;
        private final String id;

        public Task(Socket socket, String name, String id){
            this.socket = socket;
            this.name = name;
            this.id = id;
        }

        @Override
        public void run(){
            try {
                while(!lock) {
                    lock = true;
                    InputStream in = null;
                    in = socket.getInputStream();
                    OutputStream out = socket.getOutputStream();

                    out.write((id + ':' + name).getBytes());

                    byte[] bytes = new byte[1024];
                    int len = in.read(bytes);
                    String str = new String(bytes, 0, len);
                    System.out.println(str);
                    String ans = new String(bytes, 17, len - 17);
                    map.put(id, ans);
                }
                lock = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args)  {
        System.out.println("Please input author name you want to search: ");
        InputStreamReader is = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(is);
        String name = "";
        try{
            name = br.readLine();
            System.out.println("Please wait for server response...");
            long startTime = System.currentTimeMillis();
            String ans = "";
            int sum = 0;

            SearchClient searchClient = new SearchClient();
            searchClient.search("127.0.0.1", 9999, name, "00");
            searchClient.search("127.0.0.1", 9999, name, "01");
            searchClient.search("127.0.0.1", 9999, name, "02");
            while(!searchClient.lock) {
                sum += Integer.parseInt(searchClient.map.get("00"));
                break;
            }
            while(!searchClient.lock) {
                sum += Integer.parseInt(searchClient.map.get("01"));
                break;
            }
            while(!searchClient.lock) {
                sum += Integer.parseInt(searchClient.map.get("02"));
                break;
            }
            long endTime = System.currentTimeMillis();
            System.out.println("程序运行时间：" + (endTime - startTime) + "ms");
            System.out.println("The total number of occurrences of the author is " + sum);
            SearchServer.exec("echo \"The total number is " + sum + "\"" + " > 1853204-hw2-q1.log");
            SearchServer.exec("echo \"Time cost " + endTime + "ms\"" + " > 1853204-hw2-q1.log");
        }
        catch(IOException e){
            e.printStackTrace();
        }

    }

}
