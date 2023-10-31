import math
import threading
import time
import cv2
import serial
import socket
import json

x = 0
y = 0

flag = 0

target_x = 0
target_y = 0
target_angle = 0
velocity = 700

angle_current = 0
angle_servo = 0
rotate_time = 0
rotate_start = 0
rotate_end = 0

update = False

is_end = False

original_longitude = 0  # 坐标原点对应的经度
original_latitude = 0  # 坐标原点对应的纬度

path = None

ser_gps = serial.Serial("/dev/ttyUSB1", 9600)
ser_car = serial.Serial("/dev/ttyUSB0", 9600)
ser_car.read()
camera = cv2.VideoCapture(0)

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect(("192.168.0.104", 9988))


def control_camera():
    block_cascade = cv2.CascadeClassifier("./training/cascade.xml")
    while True:
        ret, frame = camera.read()
        blocks = block_cascade.detectMultiScale(frame)
        for (x1, y1, w, h) in blocks:
            if w + h > 500:
                global flag
                flag = 2


def send_thread():
    while True:
        sock.send(x.to_bytes(4, "big"))
        sock.send(y.to_bytes(4, "big"))
        sock.send(flag.to_bytes(4, "big"))
        time.sleep(0.01)


def serial_thread():
    while True:
        if not is_end:
            ser_car.write(int(255).to_bytes(4, "little"))

            if time.time() < rotate_end:
                ser_car.write(int(angle_servo + 45).to_bytes(4, "little"))
            else:
                ser_car.write(int(0).to_bytes(4, "little"))
        else:
            ser_car.write(int(0).to_bytes(4, "little"))
            ser_car.write(int(45).to_bytes(4, "little"))


def receive_thread():
    while True:
        b_length = sock.recv(4)
        length = int.from_bytes(b_length, "big")
        b_json = sock.recv(length)
        json_data = str(b_json, "utf-8")
        global path, update
        path = json.loads(json_data)
        update = True


def read_gps_data_thread():
    while True:
        line = ser_gps.readline()
        line.decode("ascii", "ignore")
        line = str(line, "utf-8")
        if line.find("$GNRMC") != -1:
            print(line)

            data = line.split(",")

            # $GNRMC,040208.000,A,4000.31229,N,11616.87528,E,0.00,0.00,120922,,,A*7A

            latitude = float(data[3][0:2]) + float(data[3][2:]) / 60  # ddmm.mmmmm
            longitude = float(data[5][0:3]) + float(data[5][3:]) / 60  # dddmm.mmmmm

            global x, y
            y = (latitude - original_latitude) * 111240
            x = (longitude - original_longitude) * 111240 * math.cos(latitude * math.pi / 180)


def get_rotate_distance(angle):
    return math.cos(angle * math.pi / 180) * 160


thread1 = threading.Thread(target=control_camera)
thread2 = threading.Thread(target=send_thread)
thread3 = threading.Thread(target=serial_thread)
thread4 = threading.Thread(target=receive_thread)
thread5 = threading.Thread(target=read_gps_data_thread)
thread1.start()
thread2.start()
thread3.start()
thread4.start()
thread5.start()

while True:
    if path is not None:
        distance = 0

        for data in path["datas"]:
            if update:
                break

            global target_x, target_y, target_angle
            target_x = data["x"]
            target_y = data["y"]
            target_angle = data["angle"]

            angle_diff = target_angle - angle_current
            if angle_diff != 0:
                distance = get_rotate_distance(angle_diff)
                rotate_time = distance / velocity
                rotate_end = time.time() + rotate_time
                if angle_diff > 0:
                    angle_servo = 45
                elif angle_diff < 0:
                    angle_servo = -45
                time.sleep(rotate_time)
            else:
                angle_servo = 0

            while x - target_x > 200 or y - target_y > 200:
                pass

        if update:
            update = False
            continue
        else:
            flag = 1
            sock.close()
            ser_car.close()
            ser_gps.close()
            path = None


# See PyCharm help at https://www.jetbrains.com/help/pycharm/
