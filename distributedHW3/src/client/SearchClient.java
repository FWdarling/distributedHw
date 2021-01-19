package client;

import server.SearchServer;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class SearchClient {

    public Map<String, String> map;
    public Boolean lock;

    public SearchClient(){
        map = new HashMap<>();
        lock = false;
    }

    public void search(String ip, int port, String name, String id) throws IOException {
        Socket socket = new Socket(ip, port);
        new Thread(new Task(socket, name, id)).start();
        System.out.println("Successfully connected to the server" + id);
        System.out.println("ip: " + ip + "and port: " + port);
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
                while(lock) {}
                lock = true;
                InputStream in = null;
                in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();

                out.write((id + ':' + name).getBytes());

                byte[] bytes = new byte[1024];
                int len = in.read(bytes);
                String str = new String(bytes, 0, len);
                System.out.println(str);
                String ans = new String(bytes, 17, len - 18);
                map.put(id, ans);
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

            SearchClient searchClient0 = new SearchClient();
            searchClient0.search("127.0.0.1", 9999, name, "00");
            SearchClient searchClient1 = new SearchClient();
            searchClient1.search("127.0.0.1", 9998, name, "01");
            SearchClient searchClient2 = new SearchClient();
            searchClient2.search("127.0.0.1", 9997, name, "02");
            int i = 0;
//            while(searchClient0.lock) {}
            Thread.sleep(1000);
            sum += Integer.parseInt(searchClient0.map.get("00"));
            System.out.println(sum);
//            while(searchClient1.lock) {}
            sum += Integer.parseInt(searchClient1.map.get("01"));
            System.out.println(sum);
//            while(searchClient2.lock) {}
            sum += Integer.parseInt(searchClient2.map.get("02"));
            System.out.println(sum);
            long endTime = System.currentTimeMillis();
            long timeCost = endTime - startTime;
            System.out.println("程序运行时间：" + timeCost + "ms");
            System.out.println("The total number of occurrences of the author is " + sum);
            SearchServer.exec("echo \"Author name searched is " + name + "\"" + " >> 1853204-hw2-q1.log");
            SearchServer.exec("echo \"The total number is " + sum + "\"" + " >> 1853204-hw2-q1.log");
            SearchServer.exec("echo \"Time cost " + timeCost + "ms\"" + " >> 1853204-hw2-q1.log");
        }
        catch(IOException | InterruptedException e){
            e.printStackTrace();
        }

    }

}
