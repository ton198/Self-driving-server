import io

import cv2
import os
import shutil

if __name__ == "__main__":
    i = 0
    save_number = 0
    lost_number = 0
    exception = 0
    file = open("./images/description_positive.txt", "w")
    while True:
        try:
            i += 1
            _pic = cv2.imread("./images/" + str(i) + ".jpg")
            pic = cv2.resize(_pic, (100, 100))

            cv2.imshow("show", _pic)

            k = cv2.waitKey()
            if k == ord('y'):
                # shutil.move("./images/" + str(i) + ".jpg", "./images/handled_" + str(save_number) + ".jpg")
                cv2.imwrite("./images/handled_" + str(save_number) + ".jpg", pic)
                file.write("handled_" + str(save_number) + ".jpg 1 0 0 100 100\n")
                print(f"本张图片操作为 入选 ，是入选的第 {save_number} 张图片，已处理 {i} 张图片")
                save_number += 1
            elif k == ord('n'):
                # shutil.move("./images/" + str(i) + ".jpg", "./images/delete_" + str(lost_number) + ".jpg")
                print(f"本张图片操作为 落选 ，是落选的第 {lost_number} 张图片，已处理 {i} 张图片")
                lost_number += 1
            elif k == ord('q'):
                cv2.destroyAllWindows()
                file.close()
                quit(0)
            # cv2.destroyWindow(str(i))
        except:
            exception += 1
            if exception > 1000:
                cv2.destroyAllWindows()
                file.close()
                quit(0)
            else:
                continue
