package dataServer;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;

public class DataSend extends Socket {
    private final String SERVER_IP;
    private final int SERVER_PORT ;
    private String filePath;

    private Socket client;
    private FileInputStream fis;
    private DataOutputStream dos;

    public DataSend(String ip, int port, String filepath) throws Exception{
        super(ip, port);
        SERVER_IP = ip;
        SERVER_PORT = port;
        filePath = filepath;
        this.client = this;
        System.out.println("Client[port:" + client.getLocalPort() + "] 成功连接服务端");
    }


    public void sendFile() throws Exception{
        try{
            //String path = DataSend.class.getResource("/ServerFile/" + fileName).getFile();
            File file = new File(filePath);
            if(file.exists()){
                fis = new FileInputStream(file);
                dos = new DataOutputStream(client.getOutputStream());

                dos.writeUTF(file.getName());
                dos.flush();
                dos.writeLong(file.length());
                dos.flush();

                System.out.println("======= 开始传输文件 ========");
                byte[] buffer = new byte[1024];
                int length = 0;
                long progress = 0;
                while((length = fis.read(buffer, 0, buffer.length)) != -1){
                    dos.write(buffer, 0, length);
                    dos.flush();
                    progress += length;
                    System.out.print("| " + (100 * progress/file.length()) + "% |");
                }
                System.out.println();
                System.out.println("======= 文件传输成功 =======");
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(fis != null)
                fis.close();
            if(dos != null)
                dos.close();
            client.close();
        }
    }

    public static void main(String[] args){
        try{
            String path = "src/dataProcess/";
            String ip1 = "127.0.0.1", ip2 = "127.0.0.1", ip3 = "127.0.0.1";
            DataSend client = new DataSend(ip1, 8888, path + "dblp.xml00");
            client.sendFile();
            client = new DataSend(ip2, 8888, path + "dblp.xml01");
            client.sendFile();
            client = new DataSend(ip3, 8888, path + "dblp.xml02");
            client.sendFile();
            client = new DataSend(ip1, 8888, path + "dblp.xml01");
            client.sendFile();
            client = new DataSend(ip2, 8888, path + "dblp.xml02");
            client.sendFile();
            client = new DataSend(ip3, 8888, path + "dblp.xml00");
            client.sendFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
