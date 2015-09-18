package com.twh.nativeapptest;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.net.TrafficStats;
import android.os.BatteryManager;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

public class NativeTestServices extends Service {

	private static final int UPDATE_PIC = 0x100;
	private static String MEMORY_TEXT = "memory: ";
	private static String CPU_TEXT = "cpu: ";
	//此处需要优化

	private int statusBarHeight;// ×´Ì¬À¸¸ß¶È
	private View view;// Í¸Ã÷´°Ìå
	private TextView flow_text = null;
	private TextView cpu_text = null;
	private TextView memory_text = null;
    private TextView battery_text = null;
	private HandlerUI handler = null;
	private Thread updateThread = null;
	private boolean viewAdded = false;// Í¸Ã÷´°ÌåÊÇ·ñÒÑ¾­ÏÔÊ¾
	private boolean viewHide = false; // ´°¿ÚÒþ²Ø
	private WindowManager windowManager;
	private WindowManager.LayoutParams layoutParams;
	private UpdateUI update;
	private long preTotalBytes;
	private long currentTotalBytes;
	private long preTotalCpuTime = 0;
	private long preAppProcessCpuTime = 0;
	private long currentTotalCpuTime = 0;
	private long currentAppProcessCpuTime = 0;
	private IntentFilter mIntentFilter;
	private List<NameValuePair> device_info;
	private String currentFlow;
	private String currentCpu;
	private String currentMemory;
	private static Thread t1;
	private Date date;
	
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		createFloatView();
		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
		device_info = new ArrayList<NameValuePair>();
		device_info = getDeviceInfo();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		System.out.println("------------------onStart");
		viewHide = false;
		refresh();
		// 注册消息处理器
	    if(mIntentReceiver != null && mIntentFilter != null) registerReceiver(mIntentReceiver, mIntentFilter);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		removeView();
		handler.removeCallbacksAndMessages(null);
		System.out.println("------------------stop");
		// 注销消息处理器
		if(mIntentReceiver != null)	unregisterReceiver(mIntentReceiver);
	    
	}

	//声明消息处理过程
	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			//要看看是不是我们要处理的消息
			if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {          
				//电池电量，数字
				Log.d("Battery", "" + intent.getIntExtra("level", 0));              
				//电池最大容量
				Log.d("Battery", "" + intent.getIntExtra("scale", 0));              
				//电池伏数
				Log.d("Battery", "" + intent.getIntExtra("voltage", 0));              
				//电池温度
				Log.d("Battery", "" + intent.getIntExtra("temperature", 0));

				//电池状态，返回是一个数字
				// BatteryManager.BATTERY_STATUS_CHARGING 表示是充电状态
				// BatteryManager.BATTERY_STATUS_DISCHARGING 放电中
				// BatteryManager.BATTERY_STATUS_NOT_CHARGING 未充电
				// BatteryManager.BATTERY_STATUS_FULL 电池满
				Log.d("Battery", "" + intent.getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN));

				//充电类型 BatteryManager.BATTERY_PLUGGED_AC 表示是充电器，不是这个值，表示是 USB
				Log.d("Battery", "" + intent.getIntExtra("plugged", 0));

				//电池健康情况，返回也是一个数字
				//BatteryManager.BATTERY_HEALTH_GOOD 良好
				//BatteryManager.BATTERY_HEALTH_OVERHEAT 过热
				//BatteryManager.BATTERY_HEALTH_DEAD 没电
				//BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE 过电压
				//BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE 未知错误
				Log.d("Battery", "" + intent.getIntExtra("health", BatteryManager.BATTERY_HEALTH_UNKNOWN));
			}
		}
	};

	/**
	 * ¹Ø±ÕÐü¸¡´°
	 */
	public void removeView() {
		if (viewAdded) {
			windowManager.removeView(view);
			viewAdded = false;
		}
		if(updateThread != null){
			updateThread.interrupt();
			updateThread=null;
		}
		if(update != null)
		{
			update.stopUpdateUI();
		}
	}
	
	private void createFloatView() {
		handler = new HandlerUI();
		update = new UpdateUI();
		updateThread = new Thread(update);
		updateThread.start(); // ¿ªÆôÏß³Ì
		preTotalBytes = TrafficStats.getTotalRxBytes();
		view = LayoutInflater.from(this).inflate(R.layout.float_window, null);
		flow_text = (TextView) view.findViewById(R.id.networkTextView);
		memory_text = (TextView) view.findViewById(R.id.memoryTextView);
		cpu_text = (TextView) view.findViewById(R.id.cpuTextView);
		battery_text = (TextView) view.findViewById(R.id.batteryTextView);
		windowManager = (WindowManager) this.getSystemService(WINDOW_SERVICE);
		/*
		 * LayoutParams.TYPE_SYSTEM_ERROR£º±£Ö¤¸ÃÐü¸¡´°ËùÓÐViewµÄ×îÉÏ²ã
		 * LayoutParams.FLAG_NOT_FOCUSABLE:¸Ã¸¡¶¯´°²»»á»ñµÃ½¹µã£¬µ«¿ÉÒÔ»ñµÃÍÏ¶¯
		 * PixelFormat.TRANSPARENT£ºÐü¸¡´°Í¸Ã÷
		 */
		layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, LayoutParams.TYPE_SYSTEM_ERROR,LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSPARENT);
		// layoutParams.gravity = Gravity.RIGHT|Gravity.BOTTOM; //Ðü¸¡´°¿ªÊ¼ÔÚÓÒÏÂ½ÇÏÔÊ¾
		layoutParams.gravity = Gravity.RIGHT | Gravity.TOP;

		/**
		 * ¼àÌý´°ÌåÒÆ¶¯ÊÂ¼þ
		 */
		view.setOnTouchListener(new OnTouchListener() {
			float[] temp = new float[] { 0f, 0f };

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
				int eventaction = event.getAction();
				switch (eventaction) {
				case MotionEvent.ACTION_DOWN: // °´ÏÂÊÂ¼þ£¬¼ÇÂ¼°´ÏÂÊ±ÊÖÖ¸ÔÚÐü¸¡´°µÄXY×ø±êÖµ
					temp[0] = event.getX();
					temp[1] = event.getY();
					break;

				case MotionEvent.ACTION_MOVE:
					refreshView((int) (event.getRawX() - temp[0]), (int) (event.getRawY() - temp[1]));
					break;
				}
				return true;
			}
		});


	}

	/**
	 * Ë¢ÐÂÐü¸¡´°
	 * 
	 * @param x
	 *            ÍÏ¶¯ºóµÄXÖá×ø±ê
	 * @param y
	 *            ÍÏ¶¯ºóµÄYÖá×ø±ê
	 */
	private void refreshView(int x, int y) {
		// ×´Ì¬À¸¸ß¶È²»ÄÜÁ¢¼´È¡£¬²»È»µÃµ½µÄÖµÊÇ0
		if (statusBarHeight == 0) {
			View rootView = view.getRootView();
			Rect r = new Rect();
			rootView.getWindowVisibleDisplayFrame(r);
			statusBarHeight = r.top;
		}

		layoutParams.x = x;
		// yÖá¼õÈ¥×´Ì¬À¸µÄ¸ß¶È£¬ÒòÎª×´Ì¬À¸²»ÊÇÓÃ»§¿ÉÒÔ»æÖÆµÄÇøÓò£¬²»È»ÍÏ¶¯µÄÊ±ºò»áÓÐÌø¶¯
		layoutParams.y = y - statusBarHeight;// STATUS_HEIGHT;
		refresh();
	}

	/**
	 * Ìí¼ÓÐü¸¡´°»òÕß¸üÐÂÐü¸¡´° Èç¹ûÐü¸¡´°»¹Ã»Ìí¼ÓÔòÌí¼Ó Èç¹ûÒÑ¾­Ìí¼ÓÔò¸üÐÂÆäÎ»ÖÃ
	 */
	private void refresh() {
		if (viewAdded) {
			windowManager.updateViewLayout(view, layoutParams);
		} else {
			windowManager.addView(view, layoutParams);
			viewAdded = true;
		}
	}

    //refresh flowrate
	private void refreshFlow()
	{
		long totalRxBytesPerSecond;
		currentTotalBytes = TrafficStats.getTotalRxBytes();
		System.out.println("=====currentTotalBytes: " + currentTotalBytes);
		totalRxBytesPerSecond = currentTotalBytes - preTotalBytes;
		System.out.println("=====totalRxBytesPerSecond: " + totalRxBytesPerSecond);
		preTotalBytes = currentTotalBytes;
		System.out.println("=====preTotalBytes: " + preTotalBytes);
		//	Log.i("flow", totalRxBytesPerSecond+"");
		if(totalRxBytesPerSecond < 1024){
			flow_text.setText(totalRxBytesPerSecond + "B/S");
		}else if(totalRxBytesPerSecond >= 1024 && totalRxBytesPerSecond < 1024*1024){
			flow_text.setText(totalRxBytesPerSecond/1024 + "K/S");
		}else{
			flow_text.setText(totalRxBytesPerSecond/1024/1024 + "M/S");
		}
		currentFlow = totalRxBytesPerSecond/1024 + "";//   kb per second
	}
	
	
	//refresh memory info
	private void refreshMemoryInfo()
	{
		String m = getAppMemoryInfo();
		memory_text.setText(MEMORY_TEXT + m +"KB");
		currentMemory = m;
	}
	
	
	//refresh cpu info
    private void refreshCpuInfo()
    {
    	String c = getCurrentCpu();
    	cpu_text.setText(CPU_TEXT + c + "%");
    	currentCpu = c;
    }
	
	
	
	/**
	 * ½ÓÊÜÏûÏ¢ºÍ´¦ÀíÏûÏ¢
	 * 
	 * @author twh
	 * 
	 */
	class HandlerUI extends Handler {
		public HandlerUI() {

		}

		public HandlerUI(Looper looper) {
			super(looper);
		}

		/**
		 * ½ÓÊÕÏûÏ¢
		 */
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			// refresh all 
			if (msg.what == UPDATE_PIC) {
				refreshFlow();
				refreshMemoryInfo();
				refreshCpuInfo();
				postInfosToServer();
				if (!viewHide)
					refresh();
			} else {
				super.handleMessage(msg);
			}

		}

	}
	
	//return memoryinfo string
//	private String getMemoryInfo(){ 
//		ProcessBuilder cmd; 
//		String result = new String(); 
//
//		try{ 
//			String[] args = {"/system/bin/cat", "/proc/meminfo"}; 
//			cmd = new ProcessBuilder(args); 
//
//			Process process = cmd.start(); 
//			InputStream in = process.getInputStream(); 
//			byte[] re=new byte[1024]; 
//			while (in.read(re)!=-1) 
//			{ 
//				System.out.println(new String(re)); 
//				result = result + new String(re); 
//
//			} 
//			in.close(); 
//		} 
//		catch(IOException ex){ 
//			ex.printStackTrace(); 
//		} 
//		return result; 
//
//
//	} 
	
//	//return cpu info string
//	private String getCPUinfo() 
//	{ 
//		ProcessBuilder cmd; 
//		String result=""; 
//
//		try{ 
////			String[] args = {"/system/bin/cat", "/proc/cpuinfo"}; 
//			String[] args = {"/system/bin/cat", "/proc/stat"};
//			
//			cmd = new ProcessBuilder(args); 
//
//			Process process = cmd.start(); 
//			InputStream in = process.getInputStream(); 
//			byte[] re = new byte[1024]; 
//			while(in.read(re) != -1){ 
//				System.out.println(new String(re)); 
//				result = result + new String(re); 
//			} 
//			in.close(); 
//		} catch(IOException ex){ 
//			ex.printStackTrace(); 
//		} 
//		return result; 
//	} 

	private String getCurrentCpu()
	{
//		int pid = 0;
		int pid = android.os.Process.myPid(); 
		int percent = 0;
		preTotalCpuTime = currentTotalCpuTime;
		preAppProcessCpuTime = currentAppProcessCpuTime;
		currentTotalCpuTime = getTotalCpuTime();
		currentAppProcessCpuTime = getCurrentAppProcessCpuTime(pid);
		if((preAppProcessCpuTime - preTotalCpuTime) == 0){
			percent = 0;
		}else{
			percent = (int) (100 * (currentAppProcessCpuTime - preAppProcessCpuTime) / (currentTotalCpuTime - preTotalCpuTime));
		}
		System.out.println("currentTotalCpuTime: " + currentTotalCpuTime);
		System.out.println("currentAppProcessCpuTime: " + currentAppProcessCpuTime);
		System.out.println("preAppProcessCpuTime: " +preAppProcessCpuTime);
		System.out.println("preTotalCpuTime: " + preTotalCpuTime);
		return percent + "";
		
	}
	
	
	private long getTotalCpuTime()
	{
		String[] cpuInfos = null;  
        try{  
            BufferedReader reader = new BufferedReader(new InputStreamReader(  
                       new FileInputStream("/proc/stat")), 1000);  
            String load = reader.readLine();  
            reader.close();  
            cpuInfos = load.split(" ");  
        }catch(IOException ex){  
            return 0;  
        }  
        long totalCpu = 0;  
        try{  
            totalCpu = Long.parseLong(cpuInfos[2])  
                       + Long.parseLong(cpuInfos[3]) + Long.parseLong(cpuInfos[4])  
                       + Long.parseLong(cpuInfos[6]) + Long.parseLong(cpuInfos[5])  
                       + Long.parseLong(cpuInfos[7]) + Long.parseLong(cpuInfos[8]);  
        }catch(ArrayIndexOutOfBoundsException e){  
            return 0;  
        }
        return totalCpu;
	}
	
	private long getCurrentAppProcessCpuTime(int pid)
	{
		
		String[] cpuInfos = null;
		try{  
//            int pid = android.os.Process.myPid();  
            BufferedReader reader = new BufferedReader(new InputStreamReader(  
                       new FileInputStream("/proc/" + pid + "/stat")), 1000);  
            String load = reader.readLine();  
            reader.close();  
            cpuInfos = load.split(" ");  
        }catch(IOException e){  
        	return 0;
        }  
        long appCpuTime = 0;  
        try{  
            appCpuTime = Long.parseLong(cpuInfos[13]) + Long.parseLong(cpuInfos[14]) + Long.parseLong(cpuInfos[15]) + Long.parseLong(cpuInfos[16]);  
        }catch(ArrayIndexOutOfBoundsException e){  
        	return 0;
        }
		return appCpuTime;
	}
	
	
	private String getAppMemoryInfo() {
		int pid = android.os.Process.myPid();  
	    ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    int[] pids = new int[] {pid};
        Debug.MemoryInfo[] memoryInfo = mActivityManager.getProcessMemoryInfo(pids);
        int memorySize = memoryInfo[0].dalvikPrivateDirty;
        return memorySize+"";
	}
	
	private long getAvalibleMemory()
	{
			long MEM_UNUSED;
			ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
			ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
			am.getMemoryInfo(mi);
			MEM_UNUSED = mi.availMem / 1024;
			return MEM_UNUSED;
	}
	
	private void postInfosToServer(){
		List <NameValuePair> params = new ArrayList <NameValuePair>();
		params.add(new BasicNameValuePair("cpu", currentCpu));
		params.add(new BasicNameValuePair("memory", currentMemory));
		params.add(new BasicNameValuePair("flow", currentFlow));
		date = new Date();
		params.add(new BasicNameValuePair("timestamp_local", date.getTime()+""));
		/*for(int i=0; i<device_info.size(); i++){
			params.add(new BasicNameValuePair(device_info.get(i).getName(), device_info.get(i).getValue()));
		}*/
		params.add(new BasicNameValuePair(device_info.get(0).getName(), device_info.get(0).getValue()));
		params.add(new BasicNameValuePair(device_info.get(1).getName(), device_info.get(1).getValue()));
		params.add(new BasicNameValuePair(device_info.get(2).getName(), device_info.get(2).getValue()));
		params.add(new BasicNameValuePair(device_info.get(3).getName(), device_info.get(3).getValue()));
		RunThread thread1 = new RunThread("send url", MainActivity.getPostUrl() , params);
//		System.out.println("url: " + MainActivity.getPostUrl());
		t1 = new Thread(thread1);
		t1.start();
	}
	
	
	private static void postRequestWithUrlAndParams(String url, List<NameValuePair> params)
    {  
		String strResult = "";
        HttpPost httpRequest = new HttpPost(url);   //建立HTTP POST联机
//        List <NameValuePair> params = new ArrayList <NameValuePair>();   //Post运作传送变量必须用NameValuePair[]数组储存 
//        params.add(new BasicNameValuePair("str", "I am Post String"));   
        try {
			httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
			HttpResponse httpResponse;
			httpResponse = new DefaultHttpClient().execute(httpRequest);
			if(httpResponse.getStatusLine().getStatusCode() == 200){
				strResult = EntityUtils.toString(httpResponse.getEntity());
			}else{
				strResult = "error";
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			//t1.stop();
		}
    }  
    
	private List<NameValuePair> getDeviceInfo(){
		List<NameValuePair> infos = new ArrayList<NameValuePair>();
		Build bd = new Build();  
		TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        /*StringBuilder sb = new StringBuilder();
        sb.append("\nDeviceId(IMEI) = " + tm.getDeviceId());
        sb.append("\nDeviceSoftwareVersion = " + tm.getDeviceSoftwareVersion());
        sb.append("\nLine1Number = " + tm.getLine1Number());
        sb.append("\nNetworkCountryIso = " + tm.getNetworkCountryIso());
        sb.append("\nNetworkOperator = " + tm.getNetworkOperator());
        sb.append("\nNetworkOperatorName = " + tm.getNetworkOperatorName());
        sb.append("\nNetworkType = " + tm.getNetworkType());
        sb.append("\nPhoneType = " + tm.getPhoneType());
        sb.append("\nSimCountryIso = " + tm.getSimCountryIso());
        sb.append("\nSimOperator = " + tm.getSimOperator());
        sb.append("\nSimOperatorName = " + tm.getSimOperatorName());
        sb.append("\nSimSerialNumber = " + tm.getSimSerialNumber());
        sb.append("\nSimState = " + tm.getSimState());
        sb.append("\nSubscriberId(IMSI) = " + tm.getSubscriberId());
        sb.append("\nVoiceMailNumber = " + tm.getVoiceMailNumber());*/
		infos.add(new BasicNameValuePair("device_model", bd.MODEL));
		infos.add(new BasicNameValuePair("device_id", tm.getDeviceId()));
		infos.add(new BasicNameValuePair("device_sdk_version", android.os.Build.VERSION.SDK));
		infos.add(new BasicNameValuePair("device_system_version", android.os.Build.VERSION.RELEASE));
		infos.add(new BasicNameValuePair("device_network_type", "" + tm.getNetworkType()));
		infos.add(new BasicNameValuePair("device_phone_type", "" + tm.getPhoneType()));
		return infos; 
	}
	
	
	/**
	 * ¸üÐÂÐü¸¡´°µÄÐÅÏ¢
	 * 
	 * @author twh
	 * 
	 */
	class UpdateUI implements Runnable {

		private Thread mthread = new Thread();
		@Override
		public void run() 
		{
			// TODO Auto-generated method stub
			// Èç¹ûÃ»ÓÐÖÐ¶Ï¾ÍÒ»Ö±ÔËÐÐ
			while ( mthread != null && !mthread.isInterrupted()) 
			{
				Message msg = handler.obtainMessage();
				msg.what = UPDATE_PIC; // ÉèÖÃÏûÏ¢±êÊ¶
				handler.sendMessage(msg);
				// ÐÝÃß1s
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}


		public void stopUpdateUI()
		{
			mthread.interrupt();
			mthread = null;
		}
	}
	
	//线程里去跑发送请求、处理请求
	public static class RunThread implements Runnable {
		@SuppressWarnings("unused")
		private String name;
		private String url;
		private List<NameValuePair> params;
		public RunThread(String name, String url, List<NameValuePair> params) {
			this.name = name;
			this.url = url;
			this.params = params;
		} 

		public void run() {
			postRequestWithUrlAndParams(url, params);
		} 
	}
	
	
}
