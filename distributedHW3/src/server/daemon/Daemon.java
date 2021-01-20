package server.daemon;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

/**
 * description: Daemon
 * date: 1/19/21 7:22 PM
 * author: fourwood
 */
public class Daemon {
    protected HashMap<String, Long> groupNodes;
    protected final String ip;
    protected final Integer port;

    public Daemon(String ip, Integer port){
        this.ip = ip;
        this.port = port;
        groupNodes = new HashMap<>();
        new Thread(new ListenTask()).start();
    }

    /*
     * description: addNode
     * date: 1/19/21 8:31 PM
     * author: fourwood
     *
     * @param ipAndPort node ip and port
     * @param time timestamp when node added
     * @return java.lang.Boolean whether spread
     */
    public Boolean addNode(String ipAndPort, Long time){
        if(ipAndPort.equals(ip + ":" + port.toString())) return false;
        if(groupNodes.containsKey(ipAndPort) && groupNodes.get(ipAndPort).equals(time)) return false;
        groupNodes.put(ipAndPort, time);
        return true;
    }
    /*
     * description: deleteNode
     * date: 1/19/21 8:34 PM
     * author: fourwood
     *
     * @param ipAndPort node ip and port
     * @return java.lang.Boolean whether spread
     */
    public Boolean deleteNode(String ipAndPort){
        if(groupNodes.containsKey(ipAndPort)) return false;
        groupNodes.remove(ipAndPort);
        return true;
    }

    public void broadcast(String msg){
        for (Map.Entry<String, Long> stringLongEntry : groupNodes.entrySet()) {
            String key = stringLongEntry.getKey();
            String[] ipAndPort = key.split(":");
            String ip = ipAndPort[0];
            Integer port = Integer.parseInt(ipAndPort[1]);
            new Thread(new SendTask(ip, port, msg)).start();
        }
    }

    /*
     * description: msgParse
     * date: 1/20/21 12:32 AM
     * author: fourwood
     * additional: The commented out flood broadcast is for scalability.
     * When there are more nodes, a complex topology can be constructed
     *
     * @param msg
     * "add_{ip and port}_{time}"
     * "del_{ip and port}"
     * "H-B_{ip and port}_{time}"
     * @return void
     */
    public void msgParse(String msg){
        String[] msgs = msg.split("_");
        String ope = msgs[0], ipAndPort = msgs[1];
        switch (ope) {
            case "add": {
                Long time = Long.parseLong(msgs[2]);
                Boolean res = addNode(ipAndPort, time);
                //if(res) broadcast(msg);
                break;
            }
            case "del": {
                Boolean res = deleteNode(ipAndPort);
                //if(res) broadcast("del_" + ipAndPort);
                break;
            }
            case "H-B": {
                Long time = Long.parseLong(msgs[2]);
                Long preTime = groupNodes.getOrDefault(ipAndPort, null);
                if (preTime == null || preTime < time) {
                    groupNodes.put(ipAndPort, time);
                    heartBeating(msg);
                }
                break;
            }
        }
    }

    /*
     * description: heartBeating using random gossip
     * date: 1/20/21 1:02 AM
     * author: fourwood
     *
     * @param
     * @return void
     */
    public void heartBeating() {
        int sz = groupNodes.size();
        List<String> keyList = new ArrayList<>(groupNodes.keySet());
        Collections.shuffle(keyList);
        sz = (sz + 2) / 3;
        for(int i = 0; i < sz; i++){
            String msg = "H-B_" + ip + ":" + port + "_" + System.currentTimeMillis();
            String[] ipAndPort = keyList.get(i).split(":");
            String tarIp = ipAndPort[0];
            int tarPort = Integer.parseInt(ipAndPort[1]);
            try{
                DatagramSocket datagramSocket = new DatagramSocket();
                byte[] bytes = msg.getBytes();
                DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(tarIp), tarPort);
                datagramSocket.send(datagramPacket);
                datagramSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("heart beating");
    }

    public void heartBeating(String msg) {
        int sz = groupNodes.size();
        List<String> keyList = new ArrayList<>(groupNodes.keySet());
        Collections.shuffle(keyList);
        sz = (sz + 2) / 3;
        for(int i = 0; i < sz; i++){
            String[] ipAndPort = keyList.get(i).split(":");
            String tarIp = ipAndPort[0];
            int tarPort = Integer.parseInt(ipAndPort[1]);
            try{
                DatagramSocket datagramSocket = new DatagramSocket();
                byte[] bytes = msg.getBytes();
                DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(tarIp), tarPort);
                datagramSocket.send(datagramPacket);
                datagramSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void check(){
        Long currTime = System.currentTimeMillis();
        String[] ineffectiveNodes = new String[groupNodes.size()];
        int count = 0;
        for (Map.Entry<String, Long> stringLongEntry : groupNodes.entrySet()) {
            Long time = stringLongEntry.getValue();
            if(time - currTime < 1000) continue;
            String ipAndPort = stringLongEntry.getKey();
            ineffectiveNodes[count++] = ipAndPort;
        }
        for(int i = 0; i < count; i++){
            String ipAndPort = ineffectiveNodes[i];
            deleteNode(ipAndPort);
            String msg = "del_" + ipAndPort;
            broadcast(msg);
        }
    }

    private class SendTask implements Runnable {

        private final String ip;
        private final Integer port;
        private final String msg;

        public SendTask(String ip, Integer port, String msg) {
            this.ip = ip;
            this.port = port;
            this.msg = msg;
        }

        @Override
        public void run(){
            try{
                DatagramSocket datagramSocket = new DatagramSocket();
                byte[] bytes = msg.getBytes();
                DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(ip), port);
                datagramSocket.send(datagramPacket);
                datagramSocket.close();
                System.out.println("send message");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class ListenTask implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    DatagramSocket datagramSocket = new DatagramSocket(port);
                    byte[] bytes = new byte[1024];
                    DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
                    datagramSocket.receive(datagramPacket);

                    String str = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                    System.out.println("accept message：" + str);
                    datagramSocket.close();

                    msgParse(str);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
