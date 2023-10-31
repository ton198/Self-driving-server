import os

if __name__ == "__main__":
    neg = open("./description_positive.txt", "w")
    for root, dirs, _file in os.walk("./images/"):
        for file in _file:
            neg.write("./images/" + str(file) + " 1 0 0 30 30" + "\n")
            print("成功写入：" + str(file))
    neg.close()
