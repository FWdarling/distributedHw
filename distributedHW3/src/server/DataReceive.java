package server;


import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class DataReceive extends ServerSocket {
    private static final int SERVER_PORT = 8888;

    public DataReceive() throws Exception{
        super(SERVER_PORT);
    }

    public void load(String path) throws Exception{
        Socket socket = this.accept();
        run(socket, path);
    }

    public void run(Socket socket, String path) {
        DataInputStream dis = null;
        FileOutputStream fos = null;
        try {
            dis = new DataInputStream(socket.getInputStream());

            String fileName = dis.readUTF();
            long fileLength = dis.readLong();
            File directory = new File(path);
            if (!directory.exists()) {
                directory.mkdir();
            }

            File file = new File(directory.getAbsolutePath() + File.separatorChar + fileName);
            fos = new FileOutputStream(file);

            byte[] bytes = new byte[1024];
            int length = 0;
            while ((length = dis.read(bytes, 0, bytes.length)) != -1) {
                fos.write(bytes, 0, length);
                fos.flush();
            }
            System.out.println("======== 文件接收成功 [File Name：" + fileName + "]  ========");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null)
                    fos.close();
                if (dis != null)
                    dis.close();
                socket.close();
            } catch (Exception e) {
            }
        }
    }

    public static void main(String[] args){
        try {
            DataReceive server = new DataReceive();
            String path = "src/server/File";
            server.load(path);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
