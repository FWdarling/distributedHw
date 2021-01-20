package server.daemon;

import java.io.*;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * description: Node
 * date: 1/20/21 12:59 PM
 * author: fourwood
 */
public class Node extends Daemon{

    private final String introducerIp;
    private final Integer introducerPort;

    public Node(String ip, Integer port, String introducerIp, Integer introducerPort){
        super(ip, port);
        this.introducerIp = introducerIp;
        this.introducerPort = introducerPort;
        join();
    }

    public void join(){
        try{
            Socket socket = new Socket(introducerIp, introducerPort);
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter printWriter = new PrintWriter(outputStream);
            groupNodes.put(introducerIp + ":" + introducerPort.toString(), System.currentTimeMillis());

            String msg = "join_" + ip + ":" + port + "_" + System.currentTimeMillis();
            printWriter.write(msg);
            printWriter.flush();
            socket.shutdownOutput();

            Thread.sleep(5000);
            InputStream inputStream = socket.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            StringBuffer buffer = new StringBuffer();
            String temStr = null;
            while((temStr = reader.readLine()) != null){
                buffer.append(temStr);
            }
            System.out.println(buffer.toString());
            nodesParse(buffer.toString());

            printWriter.close();
            outputStream.close();
            inputStreamReader.close();
            inputStream.close();
            socket.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /*
     * description: nodesParse
     * date: 1/20/21 3:07 PM
     * author: fourwood
     *
     * @param msg
     *   {ip}:{port}-{time}_{ip}:{port}-{time}_...
     * @return void
     */
    public void nodesParse(String msg){
        String[] nodes = msg.split("_");
        for(String node : nodes){
            String[] nodeMsg = node.split("-");
            Long time = Long.parseLong(nodeMsg[1]);
            String ipAndPort = nodeMsg[0];
            groupNodes.put(ipAndPort, time);
        }
    }

    public void leave(){
        try{
            Socket socket = new Socket(introducerIp, introducerPort);
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter printWriter = new PrintWriter(outputStream);

            String msg = "leave_" + ip + ":" + port;
            printWriter.write(msg);
            printWriter.flush();
            groupNodes.clear();

            socket.shutdownOutput();
            printWriter.close();
            outputStream.close();
            socket.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        Node node = new Node("127.0.0.1", 12111, "127.0.0.1", 11112);
        Timer timer1 = new Timer();
        timer1.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                node.heartBeating();
            }
        }, 10000, 10000);

        Timer timer2 = new Timer();
        timer2.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                node.check();
            }
        }, 10000, 10000);
    }
}
