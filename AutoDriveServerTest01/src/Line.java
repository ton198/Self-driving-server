
public class Line {

    public double x1, x2, y1, y2;
    public double angle, length, k, b;
    public double startTime;
    public Line(double x1, double y1, double x2, double y2){
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.k = (y2-y1)/(x2-x1);
        this.b = y1-(k*x1);
        this.angle = Math.atan(k);
        this.length = Math.pow(Math.pow(y2-y1,2)+Math.pow(x2-x1,2),0.5);
    }
}
