import java.io.IOException;
import java.net.Socket;
import java.util.*;

class HandleSocket {

    public double CAR_SPEED = 50;


    private HashMap<Double, DataContainer> map;
    private ArrayList<DataContainer> unhandledCar;
    private Random random;

    private Line[] street;
    private ArrayList<Line> roadBlock;
    private int width;
    private int laneNum;

    HandleSocket(Line[] street, int width, int laneNum){
        map = new HashMap<>();
        random = new Random();
        this.street = street;
        this.width = width;
        this.laneNum = laneNum;
        unhandledCar = new ArrayList<>(10);
        roadBlock = new ArrayList<>(10);
        new CheckBlockThread().start();
        new Handler().start();
    }
    double addCar(Socket socket) throws IOException {
        double r = random.nextInt(9999999);
        r += System.currentTimeMillis();
        DataContainer data = new DataContainer(socket, r);
        unhandledCar.add(data);
        return r;
    }

    class CheckBlockThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (true){
                Object[] datas = map.values().toArray();
                for (Object data : datas){
                    if (((DataContainer)data).haveBlock){
                        map.get(((DataContainer) data).getKey()).haveBlock = false;
                        double x = ((DataContainer) data).x, y = ((DataContainer) data).y;
                        Line str = street[getStreet(x, y)];
                        double x1 = x + 100 * Math.cos(str.angle);
                        double y1 = y + 100 * Math.sin(str.angle);
                        Line block = new Line(x, y, x1, y1);
                        roadBlock.add(block);
                    }
                }
            }
        }
    }

    class Handler extends Thread {
        @Override
        public void run() {
            super.run();
            while (true) {
                if (unhandledCar.size() > 0) {
                    DataContainer lastCar = unhandledCar.get(0);
                    unhandledCar.remove(0);
                    ArrayList<Line> path = lastCar.getPath();

                    while (!lastCar.received);
                    int x = lastCar.x;
                    int y = lastCar.y;

                    double costTime = 0;

                    for (int i = getStreet(x, y); i < street.length; i++) {

                        double totalTime = 0;
                        Line fastLine = null;
                        for (int j = 0; j < laneNum; j++) {

                            double[] pos = getLanePos(street[i].length, i, j);

                            Line tryLine = new Line(x, y, pos[0], pos[1]);
                            tryLine.startTime = costTime;

                            Object[] datas = map.values().toArray();

                            boolean detour = false;
                            for (Line barrier : roadBlock){
                                if (isCrossNoTime(barrier, tryLine)){
                                    detour = true;
                                    break;
                                }
                            }

                            if (detour){
                                continue;
                            }

                            for (int dataCounter = 0; dataCounter < datas.length; dataCounter++) {
                                if (((DataContainer)datas[dataCounter]).isDisposed) {
                                    map.remove(((DataContainer)datas[dataCounter]).getKey());
                                    continue;
                               }
                                ArrayList<Line> lineArray = ((DataContainer) datas[dataCounter]).getPath();
                                for (int lineCounter = 0; lineCounter < lineArray.size(); lineCounter++) {
                                    Line l = lineArray.get(lineCounter);
                                    if (isCross(l, tryLine)) {
                                        tryLine.startTime += 0.5;
                                        dataCounter = 0;
                                        lineCounter = 0;
                                    }
                                }
                            }

                            double currentTime = tryLine.length / CAR_SPEED + (tryLine.startTime - costTime);
                            if (currentTime < totalTime || totalTime == 0) {
                                totalTime = currentTime;
                                fastLine = tryLine;
                            }
                        }
                        costTime += totalTime;
                        path.add(fastLine);
                    }

                    map.put(lastCar.getKey(), lastCar);
                }
            }
        }

    }


    private boolean isCross(Line line1, Line line2){
        boolean isL1P1Up = line1.y1 > line1.x1*line2.k + line2.b;
        boolean isL1P2Up = line1.y2 > line1.x2*line2.k + line2.b;
        if (isL1P1Up != isL1P2Up){
            boolean isL2P1Up = line2.y1 > line2.x1*line1.k + line1.b;
            boolean isL2P2Up = line2.y2 > line2.x2*line1.k + line1.b;
            if (isL2P1Up != isL2P2Up){
                //cross
                double[] res = getCrossPoint(line1, line2);
                //获取距离
                Line temp1 = new Line(res[0], res[1], line1.x1, line1.y1);
                double pointTime1 = line1.startTime + temp1.length*CAR_SPEED;
                Line temp2 = new Line(res[0], res[1], line2.x1, line2.y1);
                double pointTime2 = line2.startTime + temp2.length*CAR_SPEED;
                if (Math.abs(pointTime1-pointTime2) < 1){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isCrossNoTime(Line line1, Line line2){
        boolean isL1P1Up = line1.y1 > line1.x1*line2.k + line2.b;
        boolean isL1P2Up = line1.y2 > line1.x2*line2.k + line2.b;
        if (isL1P1Up != isL1P2Up){
            boolean isL2P1Up = line2.y1 > line2.x1*line1.k + line1.b;
            boolean isL2P2Up = line2.y2 > line2.x2*line1.k + line1.b;
            if (isL2P1Up != isL2P2Up){
                return true;
            }
        }
        return false;
    }

    private double[] getCrossPoint(Line line1, Line line2){
        double[] result = new double[2];
        result[0] = (line1.b-line2.b)/(line2.k-line1.k);
        result[1] = line1.k * result[0] + line1.b;
        return result;
    }

    private int getLaneNum(int x, int y, int streetId){
        Line startLine = street[streetId];
        double k1 = -1 / startLine.k;
        double b1 = startLine.y1 - startLine.x1 * k1;

        double x1 = startLine.x1 + Math.cos(Math.atan(k1)) * width / 2;
        double y1 = startLine.y1 + Math.sin(Math.atan(k1)) * width / 2;
        double x2 = startLine.x1 - Math.cos(Math.atan(k1)) * width / 2;
        double y2 = startLine.y1 - Math.sin(Math.atan(k1)) * width / 2;

        double k2 = -1 / k1;
        double b2 = y - x * k2;
        double gx = (b2-b1)/(k2-k1);
        double gy = k1*gx + b1;

        int lanes;

        if (k1 >= 1){
            //起跑线与水平夹角大于45°，在1，3象限
            double _l = Math.abs(y1 - y2);
            if (startLine.x2 > startLine.x1){
                //线路在起跑线的右边
                lanes = laneNum - (int)((gy - y1) / _l * laneNum + 0.5);
            } else {
                //线路在起跑线左边
                lanes = (int)((gy - y1) / _l * laneNum + 0.5);
            }
        } else if (k1 >= 0) {
            //起跑线与水平夹角小于45°，在1，3象限
            double _l = Math.abs(x1-x2);
            if (startLine.y2 > startLine.y1){
                //线路在起跑线上面
                lanes = (int)((gx-x1)/_l * laneNum + 0.5);
            } else {
                lanes = laneNum - (int)((gx-x1)/_l * laneNum + 0.5);
            }
        } else if (k1 >= -1){
            //小于45°，在2，4象限
            double _l = Math.abs(x1 - x2);
            if (startLine.y2 > startLine.y1){
                //线路在起跑线上面
                lanes = (int)((gx - x2) / _l * laneNum + 0.5);
            } else {
                lanes = laneNum - (int)((gx - x2) / _l * laneNum + 0.5);
            }
        } else {
            //大于45°，在2，4象限
            double _l = Math.abs(y1 - y2);
            if (startLine.x2 > startLine.x1){
                //线路在起跑线的右边
                lanes = laneNum - (int)((gy - y2) / _l * laneNum + 0.5);
            } else {
                //线路在起跑线左边
                lanes = (int)((gy - y2)/ _l * laneNum + 0.5);
            }
        }
        return lanes;
    }

    private double[] getLanePos(double distanceToStart, int streets, int lanes){

        double[] result = new double[2];

        Line startLine = street[streets];
        double centerX = startLine.x1 + distanceToStart*Math.cos(Math.atan(startLine.k));
        double centerY = startLine.y1 + distanceToStart*Math.sin(Math.atan(startLine.k));
        if (startLine.k >= 1){
            //起跑线与水平夹角大于45°，在1，3象限
            if (startLine.x2 > startLine.x1){
                //线路在起跑线的右边
                result[0] = centerX + (0.5*laneNum-lanes)*(double)(width/laneNum)*Math.cos(Math.atan(-1/startLine.k));
                result[1] = centerY + (0.5*laneNum-lanes)*(double)(width/laneNum)*Math.sin(Math.atan(-1/startLine.k));
            } else {
                //线路在起跑线左边
                result[0] = centerX + (lanes-0.5*laneNum)*(double)(width/laneNum)*Math.cos(Math.atan(-1/startLine.k));
                result[1] = centerY + (lanes-0.5*laneNum)*(double)(width/laneNum)*Math.sin(Math.atan(-1/startLine.k));
            }
        } else if (startLine.k >= 0){
            if (startLine.y1 > startLine.y2){
                //小于45°，线路在右下
                result[0] = centerX + (0.5*laneNum-lanes)*(double)(width/laneNum)*Math.cos(Math.atan(-1/startLine.k));
                result[1] = centerY + (0.5*laneNum-lanes)*(double)(width/laneNum)*Math.sin(Math.atan(-1/startLine.k));
            } else {
                result[0] = centerX + (lanes-0.5*laneNum)*(double)(width/laneNum)*Math.cos(Math.atan(-1/startLine.k));
                result[1] = centerY + (lanes-0.5*laneNum)*(double)(width/laneNum)*Math.sin(Math.atan(-1/startLine.k));
            }
        } else if (startLine.k >= -1){
            if (startLine.y2 > startLine.y1){
                result[0] = centerX + (lanes-0.5*laneNum)*(double)(width/laneNum)*Math.cos(Math.atan(-1/startLine.k));
                result[1] = centerY + (0.5*laneNum-lanes)*(double)(width/laneNum)*Math.sin(Math.atan(-1/startLine.k));
            } else {
                result[1] = centerX + (lanes-0.5*laneNum)*(double)(width/laneNum)*Math.cos(Math.atan(-1/startLine.k));
                result[0] = centerY + (0.5*laneNum-lanes)*(double)(width/laneNum)*Math.sin(Math.atan(-1/startLine.k));
            }
        } else {
            if (startLine.x2 > startLine.x1){
                result[0] = centerX + (lanes-0.5*laneNum)*(double)(width/laneNum)*Math.cos(Math.atan(-1/startLine.k));
                result[1] = centerY + (0.5*laneNum-lanes)*(double)(width/laneNum)*Math.sin(Math.atan(-1/startLine.k));
            } else {
                result[1] = centerX + (lanes-0.5*laneNum)*(double)(width/laneNum)*Math.cos(Math.atan(-1/startLine.k));
                result[0] = centerY + (0.5*laneNum-lanes)*(double)(width/laneNum)*Math.sin(Math.atan(-1/startLine.k));
            }
        }
        return result;
    }

    private int getStreet(double x, double y){
        for (int i = 0;i < street.length;i++) {
            Line l = street[i];
            double k1 = -1 / l.k;
            double k2 = l.k;
            double b1_1 = l.y1 - k1*l.x1;
            double b1_2 = l.y2 - k1*l.x2;
            double b2_1 = l.b + width / Math.cos(l.angle);
            double b2_2 = l.b - width / Math.cos(l.angle);

            if ((y > k1*x+b1_1 && y < k1*x+b1_2) || (y < k1*x+b1_1 && y > k1*x+b1_2)){
                if ((x > (y-b2_1)/k2 && x < (y-b2_2)/k2) || (x < (y-b2_1)/k2 && x > (y-b2_2)/k2)){
                    return i;
                }
            }
        }
        return -1;
    }

    public static int bytes2Int(byte[] b){
        int ans = 0;
        for (int i = 0;i < 4;i++){
            ans <<= 8;
            ans |= (b[3-i]&0xff);
        }
        return ans;
    }
}
