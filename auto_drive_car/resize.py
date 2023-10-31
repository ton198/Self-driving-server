import os

import cv2

if __name__ == "__main__":

    for root, dirs, _file in os.walk("./images/"):
        for file in _file:

            print("成功写入：" + str(file))

            pic = cv2.imread("./images/" + str(file))
            res = cv2.resize(pic, (30, 30))

            cv2.imwrite("./images/" + str(file), res)
