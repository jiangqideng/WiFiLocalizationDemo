package com.jqd.wifilocalizationdemo.model;

import java.util.HashMap;

import com.jqd.wifilocalizationdemo.file.FileManager;

/**
 * @author jiangqideng@163.com
 * @date 2016-6-29 上午12:50:59
 * @description 加载Radio Map，包括bssid，长度、个数
 */
public class RadioMapModel {
	public float radioMap1[][];// 存储指纹库
	public float radioMap2[][];
	public HashMap<String, Integer> bssids; // 记录Bssid的顺序，用来查找
	public int N_fp = 0;// 指纹库的指纹长度
	public int M_fp = 0;// 指纹库的指纹个数

	private volatile static RadioMapModel radioMapModel = null;

	public static RadioMapModel getInstance() {
		if (radioMapModel == null) {
			synchronized (RadioMapModel.class) {
				if (radioMapModel == null) {
					radioMapModel = new RadioMapModel();
				}
			}
		}
		return radioMapModel;
	}

	public void init() {
		radioMap1 = getRadioMap("map1.txt");// 第一个Radio map
		radioMap2 = getRadioMap("map2.txt");// 第二个Radio map
		bssids = getBssids("bssid.txt");// 对应的bssid
	}

	/**
	 * 从assets里的文件中读取数据，string数组，第一个string代表每组指纹的长度，第二个string代表共有多少组数据
	 * 将数据转换成float型，放到一个二维数组 (注意：各个string间由空格分开，不要有回车)
	 */
	private float[][] getRadioMap(String fileName) {
		float radioMap[][] = null;
		String[] strarray = new FileManager().readFileStrings(fileName);
		// 转换成float
		N_fp = Integer.parseInt(strarray[0]);
		M_fp = Integer.parseInt(strarray[1]);
		radioMap = new float[M_fp][N_fp];
		int k = 2;
		for (int i = 0; i < M_fp; i++) {
			for (int j = 0; j < N_fp; j++) {
				try {
					radioMap[i][j] = Float.parseFloat(strarray[k]);
					k++;
				} catch (Exception e) {
				}
			}
		}
		return radioMap;
	}

	private HashMap<String, Integer> getBssids(String fileName) {
		HashMap<String, Integer> bssids = new HashMap<String, Integer>();
		String[] strarray = new FileManager().readFileStrings(fileName);
		for (int i = 0; i < strarray.length; i++) {
			bssids.put(strarray[i], i);
		}
		return bssids;
	}
}
