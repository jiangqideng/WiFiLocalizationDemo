package com.cips.wifilocalizationdemo;

import java.io.InputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static MainActivity mainactivity;
	// private TextView TipsTextView;
	private TextView StatusTextView;
	private TextView resultsTextView;
	private ImageButton myPositionImageButton;
	private ImageView planMapImageView;

	public static final long WIFI_SCAN_DELAY = 1000;
	private static final String TAG = "WifiLocalizationDemo";

	private SensorManager sensorManager;
	private SensorEventListener mSensorListener;
	private SensorEventListener oSensorListener;
	private Sensor msensor;
	private Sensor osensor;
	private WifiManager wifiManager;
	private List<ScanResult> scanResults = null;
	private Timer wifiScanTimer;
	private TimerTask wifiScanTimerTask;

	int width; // 屏幕宽度（像素）
	int height; // 屏幕高度（像素）
	float density; // 屏幕密度（0.75 / 1.0 / 1.5）
	int densityDpi; // 屏幕密度DPI（120 / 160 / 240）

	private boolean isBusy = false;
	private boolean isInit = false;
	private float[] temp_m = new float[3];
	private float[] temp_r = new float[3];

	String bssids[];

	float radioMap1[][];
	int N_fp = 0;// 指纹库的指纹长度
	int M_fp = 0;// 指纹库的指纹个数

	float radioMap2[][];
	float rssScan[] = new float[229];

	Position p0 = new Position(0f, 0f);
	Position p1 = new Position(0f, 0f);
	float result_x0 = 0f;
	float result_y0 = 0f;
	int result_num0 = 0;
	float result_x1 = 0f;
	float result_y1 = 0f;
	int result_num1 = 0;

	int Wifi_Num_Min = 3;// 定位时wifi热点的最小数量

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		mainactivity = this;
		if (!isInit) {

			// TipsTextView = (TextView) findViewById(R.id.textView_Tips);
			StatusTextView = (TextView) findViewById(R.id.textView1);
			resultsTextView = (TextView) findViewById(R.id.textView2);
			myPositionImageButton = (ImageButton) findViewById(R.id.imageButton1);
			planMapImageView = (ImageView) findViewById(R.id.imageView1);
			// 点击定位点图标的监听程序
			myPositionImageButton.setOnClickListener(new OnClickListener() {
				@Override
				// 在当前onClick方法中监听点击Button的动作
				public void onClick(View v) {
					String outString = " x: " + result_x0 + "\n" + " y: "
							+ result_y0;
					new AlertDialog.Builder(mainactivity).setTitle("位置坐标")
							.setMessage(outString)
							.setPositiveButton("确定", null).show();
				}
			});
			DisplayMetrics metric = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(metric);
			width = metric.widthPixels; // 屏幕宽度（像素）
			height = metric.heightPixels; // 屏幕高度（像素）
			density = metric.density; // 屏幕密度（0.75 / 1.0 / 1.5）
			densityDpi = metric.densityDpi; // 屏幕密度DPI（120 / 160 / 240）

			radioMap1 = doGetMap("map1.txt");
			radioMap2 = doGetMap("map2.txt");
			bssids = doGetBssids("bssid.txt");
			doInitSensors();
			doInitWifi();
			doScanWifi();

			isInit = true;
		}
	}

	// 创建点击菜单键对应的菜单
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	// 点击菜单项响应
	public boolean onOptionsItemSelected(MenuItem item) {
		// Log.i("MenuTest:", "ItemSelected");
		switch (item.getItemId()) {
		case R.id.action_abouts:
			// Log.i("MenuTest:", "ItemSelected:1");
			// String outString = R.string.about;
			new AlertDialog.Builder(this)
					.setTitle(R.string.action_abouts)
					.setMessage(
							this.getString(R.string.about) + "\n"
									+ this.getString(R.string.version))
					.setPositiveButton(R.string.ok, null).show();
			break;
		default:
			break;
		}
		return true;
	}

	// 初始化各种Sensors的操作
	public void doInitSensors() {
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		msensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		osensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		mSensorListener = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent event) {
				// TODO Auto-generated method stub
				temp_m[0] = event.values[0];
				temp_m[1] = event.values[1];
				temp_m[2] = event.values[2];
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub
			}
		};
		oSensorListener = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent event) {
				// TODO Auto-generated method stub
				temp_r[0] = event.values[0];
				temp_r[1] = event.values[1];
				temp_r[2] = event.values[2];
				// TipsTextView.setText(String.valueOf(temp_r[0]) + " "
				// + String.valueOf(temp_r[1]) + " "
				// + String.valueOf(temp_r[2]));
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub
			}
		};
		// sensorManager.registerListener(mSensorListener, msensor,
		// SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(oSensorListener, osensor,
				SensorManager.SENSOR_DELAY_UI);
	}

	// 初始化WIFI，开启
	public void doInitWifi() {
		wifiManager = (WifiManager) MainActivity.this
				.getSystemService(Context.WIFI_SERVICE);
		Toast.makeText(MainActivity.this, "正在开启WiFi...", Toast.LENGTH_SHORT)
				.show();
		wifiManager.setWifiEnabled(true);
		while (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
			// TipsTextView.setText("正在开启WiFi，请稍候");
		}
		// TipsTextView.setText("WiFi已开启");
	}

	// 设置Timer任务，开始Wifi扫描
	public void doScanWifi() {
		registerReceiver(wifiReceiver, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		wifiScanTimer = new Timer();
		wifiScanTimerTask = new TimerTask() {
			public void run() {
				wifiManager.startScan();
			}
		};
		wifiScanTimer.schedule(wifiScanTimerTask, 0, WIFI_SCAN_DELAY);
	}

	private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			scanResults = wifiManager.getScanResults();
			// Toast.makeText(MainActivity.this,
			// "共扫描到 "+scanResults.size()+"个wifi热点", Toast.LENGTH_SHORT).show();
			if (scanResults != null) {
				for (int i = 0; i < rssScan.length; i++) {
					rssScan[i] = -200;
				}
				int bsscount = 0;
				for (int i = 0; i < scanResults.size(); i++) {
					int j = 0;
					while (j < bssids.length
							&& !scanResults.get(i).BSSID.equals(bssids[j])) {
						j++;
					}
					if (j < bssids.length) {
						rssScan[j] = scanResults.get(i).level;
						bsscount++;
					}
				}
				if (bsscount < Wifi_Num_Min) {
					Toast.makeText(MainActivity.this, "WIFI热点数量太少，无法定位",
							Toast.LENGTH_SHORT).show();
				}

				// String outString = "扫描到Wifi个数： " + scanResults.size() + " "
				// + "CIPS:" + rssScan[2];
				// StatusTextView.setText(outString);

				new LocalizationAlgorithmTask().execute();
			}
		}
	};

	/**
	 * 从assets里的文件中读取数据，string数组，第一个string代表每组指纹的长度，第二个string代表共有多少组数据
	 * 将数据转换成float型，放到一个二维数组 (注意：各个string间由空格分开，不要有回车)
	 */
	private float[][] doGetMap(String fileName) {
		float radioMap[][];
		try {
			InputStream in = getResources().getAssets().open(fileName);
			int length = in.available();
			byte[] buffer = new byte[length];
			in.read(buffer);
			in.close();
			String txtString = new String(buffer);
			String[] strarray = txtString.split(" ");
			// String out = strarray[0] + " length" + strarray.length;
			// StatusTextView.setText(out);
			// 转换成float
			try {
				N_fp = Integer.parseInt(strarray[0]);
				M_fp = Integer.parseInt(strarray[1]);
			} catch (Exception e) {
			}

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

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new float[0][0];
	}

	private String[] doGetBssids(String fileName) {
		try {
			InputStream in = getResources().getAssets().open(fileName);
			int length = in.available();
			byte[] buffer = new byte[length];
			in.read(buffer);
			in.close();
			String txtString = new String(buffer);
			String[] strarray = txtString.split(" ");
			return strarray;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new String[0];
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

				if (result_x0 == 1
						&& (temp_r[0] > 180 - 45 && temp_r[0] < 180 + 45)
						|| result_y0 == 56
						&& (temp_r[0] > 270 - 45 && temp_r[0] < 270 + 45)
						|| result_x0 == 77
						&& (temp_r[0] > 360 - 45 || temp_r[0] < 0 + 45)
						|| result_y0 == 1
						&& (temp_r[0] > 90 - 45 && temp_r[0] < 90 + 45)) {

					result_num0 = localizationAlogrithm(rssScan, radioMap2, k,
							n_AP, D_Threshold, result_x0, result_y0);
					mapNum = 2;
				} else {
					result_num0 = localizationAlogrithm(rssScan, radioMap1, k,
							n_AP, D_Threshold, result_x0, result_y0);
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
			uiUpdate();

		}
	}

	public void uiUpdate() {
		String outString = "定位结果:\n";

		int l_left = planMapImageView.getLeft();
		int l_right = planMapImageView.getRight();
		int l_bottom = planMapImageView.getBottom();
		int l_top = planMapImageView.getTop();

		outString = outString + "num: " + result_num0 + " x: " + result_x0
				+ " y: " + result_y0;
		outString = outString + "\nwidth： " + width + " height: " + height
				+ " density: " + density + " densityDip: " + densityDpi;
		outString = outString + "\n上下左右： " + l_top + " " + l_bottom + " "
				+ l_left + " " + l_right;
		// TipsTextView.setText(outString);

		float map_width = 63;
		float map_height = 85;
		float offest_x = 3;
		float offset_y = 3;
		float ui_x = l_left + (l_right - l_left) * (57 - result_y0 + offest_x)
				/ map_width;
		float ui_y = l_top + (l_bottom - l_top) * (79 - result_x0 + offset_y)
				/ map_height;

		myPositionImageButton.setX(ui_x - myPositionImageButton.getWidth() / 2);
		myPositionImageButton.setY(ui_y - myPositionImageButton.getHeight());

		String outString2 = "定位坐标：\nx: " + result_x0 + "\ny: " + result_y0;
		resultsTextView.setText(outString2);

		outString2 = "    当前Wifi个数:" + scanResults.size();
		StatusTextView.setText(outString2);

	}

	/**
	 * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
	 */
	public static float dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (dpValue * scale + 0.5f);
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
			// //////设定搜索范围
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
