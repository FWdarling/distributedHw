package server.daemon;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * description: Introducer
 * date: 1/20/21 12:59 PM
 * author: fourwood
 */
public class Introducer extends Daemon{
    private final Integer tcpPort;

    public Introducer(String ip, Integer port, Integer tcpPort){
        super(ip, port);
        this.tcpPort = tcpPort;
        new Thread(new TcpListenTask()).start();
    }

    private class TcpListenTask implements Runnable{

        @Override
        public void run(){
            InputStream inputStream = null;
            OutputStream outputStream = null;
            PrintWriter printWriter = null;
            InputStreamReader inputStreamReader = null;
            try{
                ServerSocket serverSocket = new ServerSocket(tcpPort);
                System.out.println("introducer start");
                while(true){
                    Socket socket = serverSocket.accept();
                    inputStream = socket.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader reader = new BufferedReader(inputStreamReader);
                    StringBuffer stringBuffer = new StringBuffer();
                    String temStr = null;
                    while((temStr = reader.readLine()) != null){
                        stringBuffer.append(temStr);
                    }
                    System.out.println("server accept message:" + stringBuffer.toString());
                    tcpMsgParse(stringBuffer.toString());

                    outputStream = socket.getOutputStream();
                    printWriter = new PrintWriter(outputStream);
                    printWriter.write(generateNodesMsg());
                    printWriter.flush();

                    inputStreamReader.close();
                    printWriter.close();
                    inputStream.close();
                    outputStream.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        /*
         * description: generateNodesMsg
         * date: 1/20/21 2:40 PM
         * author: fourwood
         *
         * @param
         * @return java.lang.String
         * {ip}:{port}-{time}_{ip}:{port}-{time}_...
         */
        public String generateNodesMsg(){
            StringBuilder msg = new StringBuilder();
            for (Map.Entry<String, Long> stringLongEntry : groupNodes.entrySet()) {
                String ipAndPort = stringLongEntry.getKey();
                Long time = stringLongEntry.getValue();
                msg.append(ipAndPort).append("-").append(time.toString()).append("_");
            }
            msg.append(ip).append(":").append(port.toString()).append("-").append(System.currentTimeMillis());
            return msg.toString();
        }

        /*
         * description: tcpMsgParse
         * date: 1/20/21 2:34 PM
         * author: fourwood
         *
         * @param msg
         * "join_{ip and port}_{time}"
         * "leave_{ip and port}"
         * @return void
         */
        public void tcpMsgParse(String msg){
            String[] msgs = msg.split("_");
            String ope = msgs[0], ipAndPort = msgs[1];
            switch (ope) {
                case "join": {
                    Long time = Long.parseLong(msgs[2]);
                    Boolean res = addNode(ipAndPort, time);
                    if(res) broadcast("add_" + ipAndPort + "_" + time);
                    break;
                }
                case "leave": {
                    Boolean res = deleteNode(ipAndPort);
                    if(res) broadcast("del_" + ipAndPort);
                    break;
                }
            }
        }
    }

    public static void main(String[] args){
        Introducer introducer = new Introducer("127.0.0.1", 11111, 11112);
        Timer timer1 = new Timer();
        timer1.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                introducer.heartBeating();
            }
        }, 5000, 5000);

        Timer timer2 = new Timer();
        timer2.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                introducer.check();
            }
        }, 20000, 20000);
    }
}
