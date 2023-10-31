import com.google.gson.Gson;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerStart {

    public static void main(String[] args) throws IOException {

        String jsonData = Files.readString(Path.of("streetData.json"));
        Gson jsonDecoder = new Gson();
        MapStructure codedMap = jsonDecoder.fromJson(jsonData, MapStructure.class);

        Line[] mapLine = new Line[codedMap.basicLine.length-1];

        for (int i = 0;i < codedMap.basicLine.length - 1;i++){
            mapLine[i] = new Line(codedMap.basicLine[i].x, codedMap.basicLine[i].y, codedMap.basicLine[i+1].x, codedMap.basicLine[i+1].y);
        }

        HandleSocket handler = new HandleSocket(mapLine, codedMap.streetWidth, codedMap.laneNumber);
        ServerSocket server = new ServerSocket(9988);

        while (true){
            Socket socket = server.accept();
            handler.addCar(socket);
        }
    }

    class Point {
        double x;
        double y;
    }

    class MapStructure {
        int streetWidth;
        int laneNumber;
        Point[] basicLine;
    }
}
