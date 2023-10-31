import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class DataContainer {

    private StreamContainer streams;
    private ArrayList<Line> path;
    private Socket socket;
    private double key;
    public int x = 0, y = 0;
    public boolean received = false;
    public boolean isDisposed = false, canSend = false;
    public boolean haveBlock = false;

    DataContainer(Socket socket, double key) throws IOException {
        streams = new StreamContainer(socket);
        path = new ArrayList<>(30);
        this.socket = socket;
        this.key = key;
        new receiveThread().start();
    }


    public double getKey() {
        return key;
    }

    public ArrayList<Line> getPath() {
        return path;
    }

    class receiveThread extends Thread{
        @Override
        public void run() {
            super.run();
            DataInputStream input = streams.getInput();
            while (true){
                try {
                    x = input.readInt();
                    y = input.readInt();
                    int flag = input.readInt();
                    received = true;

                    if (flag == 1){
                        socket.close();
                        path.clear();
                        isDisposed = true;
                        break;
                    } else if (flag == 2){
                        haveBlock = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    isDisposed = true;
                    break;
                }
            }
        }
    }

    class sendThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (true){
                if (canSend){
                    canSend = false;
                    DataTemplate d = new DataTemplate();
                    d.datas = new SingleData[path.size()];
                    for (int i = 0;i < path.size();i ++){
                        d.datas[i] = new SingleData();
                        d.datas[i].x = path.get(i).x2;
                        d.datas[i].y = path.get(i).y2;
                        d.datas[i].angle = path.get(i).angle;
                        d.datas[i].startTime = path.get(i).startTime;
                    }
                    Gson jsonEncoder = new Gson();
                    String jsonData = jsonEncoder.toJson(d);
                    try {
                        byte[] bytes = jsonData.getBytes();
                        streams.getOutput().writeInt(bytes.length);
                        streams.getOutput().write(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        class DataTemplate {
            public SingleData[] datas;
        }

        class SingleData {
            public double x, y;
            public double angle;
            public double startTime;
        }
    }
}
