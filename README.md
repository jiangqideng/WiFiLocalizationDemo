## WiFiLocalizationDemo

###简介

一个使用wifi进行室内定位的Android应用程序。手机扫描附近WiFi热点，将信号强度与数据库进行对比，实现knn算法，确定当前位置并显示。

###欢迎界面

![main](https://raw.githubusercontent.com/jiangqideng/resources/master/device-2016-06-29-010909.png)

###主界面

![main](https://raw.githubusercontent.com/jiangqideng/resources/master/device-2016-03-01-214949.png)

###原理介绍

程序内部记录了指纹库，即位置坐标与信号特征的对应关系，通过KNN算法即可根据当前的信号特征预测实际位置。

###其他

本项目目前只能在天津大学26楼使用，将不定期更新，逐渐增加以下功能：
+ 地图导入与指纹库的制作，可以考虑在线上传和下载数据
+ 使用传感器增加惯性导航功能，混合定位可以达到更流畅、更高精度的效果。
