
import serial
import re

if __name__ == "__main__":
    ser = serial.Serial("/dev/ttyUSB0", 9600)

    while True:
        line = ser.readline()
        line.decode("ascii", "ignore")
        line = str(line, "utf-8")
        if line.find("$GNRMC") != -1:
            print(line)

            data = line.split(",")

            # $GNRMC,040208.000,A,4000.31229,N,11616.87528,E,0.00,0.00,120922,,,A*7A

            _latitude = float(data[3][0:2]) + float(data[3][2:]) / 60  # ddmm.mmmmm
            _longitude = float(data[5][0:3]) + float(data[5][3:]) / 60  # dddmm.mmmmm
            print("N:", _latitude)
            print("E:", _longitude)

