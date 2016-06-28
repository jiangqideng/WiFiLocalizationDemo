package com.jqd.wifilocalizationdemo.algorithms;

import android.os.AsyncTask;
import android.util.Log;

import com.jqd.wifilocalizationdemo.model.RadioMapModel;
import com.jqd.wifilocalizationdemo.model.SensorsDataManager;
import com.jqd.wifilocalizationdemo.model.WiFiDataManager;
import com.jqd.wifilocalizationdemo.ui.MainActivity;

/**
 * @author jiangqideng@163.com
 * @date 2016-6-29 上午12:49:54
 * @description 基于knn的定位算法
 */
public class KNNLocalization {
	private boolean isBusy = false;
	float result_x0 = 0f;
	float result_y0 = 0f;
	int result_num0 = 0;

	public void start() {
		new LocalizationAlgorithmTask().execute();
	}

	private class LocalizationAlgorithmTask extends
			AsyncTask<String, Void, Integer> {
		/**
		 * The system calls this to perform work in a worker thread and delivers
		 * it the parameters given to AsyncTask.execute()
		 */
		protected Integer doInBackground(String... urls) {
			int mapNum = 0;
			if (!isBusy) {
				isBusy = true;
				int k = 7;
				int n_AP = 20;
				float D_Threshold = 12f;

				// 选择map1还是map2
				float[] temp_r = SensorsDataManager.getInstance().temp_r;
				if (result_x0 == 1
						&& (temp_r[0] > 180 - 45 && temp_r[0] < 180 + 45)
						|| result_y0 == 56
						&& (temp_r[0] > 270 - 45 && temp_r[0] < 270 + 45)
						|| result_x0 == 77
						&& (temp_r[0] > 360 - 45 || temp_r[0] < 0 + 45)
						|| result_y0 == 1
						&& (temp_r[0] > 90 - 45 && temp_r[0] < 90 + 45)) {

					result_num0 = localizationAlogrithm(
							WiFiDataManager.getInstance().rssScan,
							RadioMapModel.getInstance().radioMap2, k, n_AP,
							D_Threshold, result_x0, result_y0);
					mapNum = 2;
				} else {
					result_num0 = localizationAlogrithm(
							WiFiDataManager.getInstance().rssScan,
							RadioMapModel.getInstance().radioMap1, k, n_AP,
							D_Threshold, result_x0, result_y0);
					mapNum = 1;
				}
			} else {
				result_num0 = result_num0 + 100;// 好像永远不会busy啊，进不来这里，无所谓了
			}
			isBusy = false;
			return mapNum;
		}

		/**
		 * The system calls this to perform work in the UI thread and delivers
		 * the result from doInBackground()
		 */
		protected void onPostExecute(Integer results) {
			// StatusTextView.setText("Map: " + results);
			MainActivity.mainactivity.uiUpdate();

		}
	}

	public int localizationAlogrithm(float rss[], float radioMap[][], int K,
			int n_AP, float D_Threshold, float x0, float y0) {
		int M = radioMap.length;
		int N = rss.length;
		Log.i("定位算法的N和M", "M = " + String.valueOf(M) + "N= " + N);
		// 选出前8个最大的rss及其index
		int index[] = new int[N];
		for (int i = 0; i < N; i++) {
			index[i] = i;
		}
		for (int i = 0; i < n_AP; i++) {
			for (int j = 0; j < N - 1 - i; j++) {
				if (rss[j] > rss[j + 1]) {
					float tmp = rss[j];
					rss[j] = rss[j + 1];
					rss[j + 1] = tmp;
					int temp = index[j];
					index[j] = index[j + 1];
					index[j + 1] = temp;
				}
			}
		}// 这时rss数组的右端为最大的rss值，数组index的右端为相应的index

		// 设定搜索范围
		int searchRange[] = new int[M];
		int L_search;

		if (x0 == 0 && y0 == 0) {// 初始定位，没有先验信息，全局搜索
			L_search = M;
			for (int i = 0; i < M; i++) {
				searchRange[i] = i;
			}
		} else {
			// 缩减搜索范围
			L_search = 0;
			for (int i = 0; i < M; i++) {
				if (Math.abs(radioMap[i][N] - x0)
						+ Math.abs(radioMap[i][N + 1] - y0) <= D_Threshold) {
					searchRange[L_search] = i;
					L_search++;
				}
			}
			if (L_search < K) {
				L_search = M;
				for (int i = 0; i < M; i++) {
					searchRange[i] = i;
				}
			}
		}

		Log.i("搜索范围L_search", "L_search = " + L_search);

		// 计算曼哈顿距离
		float mDistance[] = new float[L_search];
		for (int i = 0; i < L_search; i++) {
			mDistance[i] = 0;// 初始化为0
			for (int j = 0; j < n_AP; j++) {
				mDistance[i] = mDistance[i]
						+ Math.abs(rss[N - 1 - j]
								- radioMap[searchRange[i]][index[N - 1 - j]]);
			}
			mDistance[i] = mDistance[i] / n_AP;
		}
		// 选出K个距离最小的指纹，对searchRange和mDistance一起排序
		for (int i = 0; i < K; i++) {
			for (int j = 0; j < L_search - 1 - i; j++) {
				if (mDistance[j] < mDistance[j + 1]) {
					float tmp = mDistance[j];
					mDistance[j] = mDistance[j + 1];
					mDistance[j + 1] = tmp;
					int temp = searchRange[j];
					searchRange[j] = searchRange[j + 1];
					searchRange[j + 1] = temp;
				}
			}
		}// 现在数组mDistance的右端是最小的K个距离，searchRange数组的右端是相应的指纹在radiomap中的index

		// 计算位置
		float x = 0;
		float y = 0;

		for (int i = 0; i < K; i++) {
			x += radioMap[searchRange[L_search - 1 - i]][N]; // radiomap的长度一定是N+2的，多一个x和一个y
			y += radioMap[searchRange[L_search - 1 - i]][N + 1];
		}

		x = x / K;
		y = y / K;

		x = 0.7f * x + 0.3f * x0;
		y = 0.7f * y + 0.3f * y0;

		Position p_temp = new Position(x, y);
		result_x0 = x;
		result_y0 = y;
		result_num0 = p_temp.num;

		MainActivity.mainactivity.result_x0 = result_x0;
		MainActivity.mainactivity.result_y0 = result_y0;
		MainActivity.mainactivity.result_num0 = result_num0;
		Log.i("位置", "X = " + String.valueOf(x) + "N= " + String.valueOf(y));
		return p_temp.num;
	}

	public class Position {
		float x, y;
		int num;
		int xInPix, yInPix;

		public Position(float x, float y) {
			this.x = x;
			this.y = y;
			num = (int) (x + y);
			xInPix = num;
			yInPix = num;
		}

		public Position(int num) {
			this.num = num;
			x = num;
			y = num;
			xInPix = num;
			xInPix = num;
		}

	}

}
