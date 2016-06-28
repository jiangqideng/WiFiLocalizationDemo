package com.jqd.wifilocalizationdemo.model;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import com.jqd.wifilocalizationdemo.algorithms.KNNLocalization;
import com.jqd.wifilocalizationdemo.ui.MainActivity;
import com.jqd.wifilocalizationdemo.util.TransformUtil;

/**
 * @author jiangqideng@163.com
 * @date 2016-6-29 上午12:52:40
 * @description WiFi扫描数据的管理
 */
public class WiFiDataManager {
	public static final long WIFI_SCAN_DELAY = 1000;
	private WifiManager wifiManager;
	public List<ScanResult> scanResults = null;
	private Timer wifiScanTimer;
	private TimerTask wifiScanTimerTask;
	public float rssScan[];
	public boolean isNormal = true;

	private volatile static WiFiDataManager wiFiDataManager = null;

	public static WiFiDataManager getInstance() {
		if (wiFiDataManager == null) {
			synchronized (WiFiDataManager.class) {
				if (wiFiDataManager == null) {
					wiFiDataManager = new WiFiDataManager();
				}
			}
		}
		return wiFiDataManager;
	}

	// 初始化WIFI，开启
	public void initWifi() {
		wifiManager = (WifiManager) MainActivity.mainactivity
				.getSystemService(Context.WIFI_SERVICE);
		Toast.makeText(MainActivity.mainactivity, "正在开启WiFi...",
				Toast.LENGTH_SHORT).show();
		wifiManager.setWifiEnabled(true);
		while (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
			// TipsTextView.setText("正在开启WiFi，请稍候");
		}
		// TipsTextView.setText("WiFi已开启");
	}

	// 设置Timer任务，开始Wifi扫描
	public void startScanWifi() {
		MainActivity.mainactivity.registerReceiver(wifiReceiver,
				new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
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
			rssScan = new TransformUtil().scanResults2vector(scanResults,
					RadioMapModel.getInstance().bssids);
			new KNNLocalization().start();
		}
	};
}
