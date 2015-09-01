package com.twh.nativeapptest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.http.util.EncodingUtils;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.twh.nativeapptest.MainActivity.MyCheckBoxAdapter.ViewHolder;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {
	

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager; 
	
	//app array
	public String[] TEST_ITEMS = {"CPU", "内存", "流量", "电量"};
	public String CONFIG_FILE_PATH = "/data/data/com.twh.nativeapptest/files/config.json";
	public static String POST_URL = "http://192.168.199.180:3001/livedata";
	public String CONFIG_NAME = "config.json";
	public ArrayList<HashMap<String, Object>> appArray = null;
    public ArrayList<HashMap<String,Object>> testItemsArray = null;
    public HashMap<Integer, Boolean> isSelected = null;
    public ArrayList<HashMap<String, Object>> resultArray = new ArrayList<HashMap<String,Object>>();
	public ArrayList<View> viewArray = null;
    public Button startBtn = null;
    public Intent serviceIntent = null;
    public boolean isServiceOn = false; 
    public static String postUrl = "";
    public boolean hasConfigFile = true;
    public String configs = "";
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		LayoutInflater inflater = getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_main_dummy, (ViewGroup) findViewById(R.id.underTestAppListView));
		serviceIntent = new Intent(MainActivity.this, NativeTestServices.class);
		viewArray = new ArrayList<View>();
		viewArray.add(view);
		viewArray.add(view);
		viewArray.add(view);
		viewArray.add(view);
		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
        
		//create config at first time
		hasConfigFile = checkConfigfile();
		
		if(hasConfigFile){
			configs = readConfigFile();
		}else{
			JSONObject config = new JSONObject();
			try {
				config.put("post_url", POST_URL);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			writeConfigFile(config.toString().replace("\\", ""));
			configs = readConfigFile();
		}
	
		/**根据返回成功的信息，取出里面的字符串转换为json进行取值*/  
		//获取一些配置
		try {
			JSONObject obj = new JSONObject(configs);
			postUrl = obj.getString("post_url");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		menu.add(Menu.NONE, Menu.FIRST + 1, 5, "edit config");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
        case Menu.FIRST + 1:
        	showEditConfigDialog();
            break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			Fragment fragment = new DummySectionFragment();
			Bundle args = new Bundle();
			args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount() {
			// Show 4 total pages.
			return 4;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_section1).toUpperCase(l);
			case 1:
				return getString(R.string.title_section2).toUpperCase(l);
			case 2:
				return getString(R.string.title_section3).toUpperCase(l);
			case 3:
				return getString(R.string.title_section4).toUpperCase(l);
			}
			return null;
		}
	}

	/**
	 * A dummy fragment representing a section of the app, but that simply
	 * displays dummy text.
	 */
	public class DummySectionFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "section_number";

		public DummySectionFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			int sectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
			
			View rootView = null;
			//根据页面不同显示不同的样式
			switch(sectionNumber){
			case 0:
				rootView = initAppListView(inflater, container);
				break;
			case 1:
				rootView = initTestItemsListView(inflater, container);
				break;
			case 2:
				rootView = initResultsListView(inflater, container);
				break;
			case 3:
				rootView = initSettingsListView(inflater, container);
				break;
			}
			return rootView;
		}
	}
	
	
	//生成4个独立页面，第一个被测应用和所有应用的list
	public View initAppListView(LayoutInflater inflater, ViewGroup container)
	{
		View view = inflater.inflate(R.layout.fragment_main_dummy, container, false);
		showAllPackages(view);
		viewArray.set(0, view);
		return view;
	}
	
	//生成一个要选择测试项目的list
	public View initTestItemsListView(LayoutInflater inflater, ViewGroup container)
	{
		View view = inflater.inflate(R.layout.fragment_test_items_dummy, container, false);
		showTestItemsListView(view);
		viewArray.set(1, view);
		return view;
	}
	
	//生成一个测试应用结束后的结果页面
	public View initResultsListView(LayoutInflater inflater, ViewGroup container)
	{
		View view = inflater.inflate(R.layout.fragment_result_dummy, container, false);
		viewArray.set(2, view);
		showResultListView(view, resultArray);
		return view;
	}
	
	//生成一个设置页面
	public View initSettingsListView(LayoutInflater inflater, ViewGroup container)
	{
		View view = inflater.inflate(R.layout.fragment_test_items_dummy, container, false);
		viewArray.set(3, view);
		return view;
	}
	
	//第一个view，显示所有的app
	public void showAllPackages(final View view)
	{
		PackageManager pm;
		List<PackageInfo> appInfo;
		ListView allPackageLv = null;
		allPackageLv = (ListView) view.findViewById(R.id.allAppsListView);

		pm = getPackageManager();
		appInfo = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
		// getInstalledPackages(flags):flage的参数有多种

		Iterator<PackageInfo> it = appInfo.iterator();
		while (it.hasNext()) {
			PackageInfo app = (PackageInfo) it.next();
			// if ((app.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) ==
			// 0) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("icon", app.applicationInfo.loadIcon(pm));
			map.put("appName", app.applicationInfo.loadLabel(pm));
			map.put("packageName", app.packageName);
			if (appArray == null)
				appArray = new ArrayList<HashMap<String, Object>>();
			appArray.add(map);
			// }
		}

		showPackagesListView(allPackageLv, appArray);
		allPackageLv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				
				ArrayList<HashMap<String, Object>> temp = new ArrayList<HashMap<String,Object>>();
				temp.add(appArray.get(arg2));
				showCurrentUnderTestApp(view, temp);
				String appName = (String) appArray.get(arg2).get("appName");
				HashMap<String, Object> map2 = new HashMap<String, Object>();
				map2.put("appName", appName);
				map2.put("time", getCurrentTime());
				resultArray.add(map2);
				showResultListView(viewArray.get(2), resultArray);
			}
		});

	}
	
	//显示当前选中的被测应用，这个list里最多显示一个被测应用
	public void showCurrentUnderTestApp(View view , ArrayList<HashMap<String, Object>> oneApp)
	{
		ListView currentAppLv = null;
		currentAppLv = (ListView) view.findViewById(R.id.underTestAppListView);
		showPackagesListView(currentAppLv, oneApp);
	}

	//提取出来的公用的，显示listview的方法
	public void showPackagesListView(ListView lv, ArrayList<HashMap<String, Object>> arrayList)
	{
		SimpleAdapter adapter = new SimpleAdapter(this, arrayList, R.layout.packitem, new String[] { "icon", "appName","packageName" }, 
				new int[] { R.id.icon, R.id.appName, R.id.packageName });
		lv.setAdapter((ListAdapter) adapter);
		adapter.setViewBinder(new ViewBinder() {
			public boolean setViewValue(View view, Object data,
					String textRepresentation) {
				if (view instanceof ImageView && data instanceof Drawable) {
					ImageView iv = (ImageView) view;
					iv.setImageDrawable((Drawable) data);
					return true;
				} else if (view instanceof TextView && data instanceof String) {
					TextView tv = (TextView) view;
					tv.setText((String) data);
					return true;
				} else
					return false;
			}
		});
	}
	
	//show test items
	public void showTestItemsListView(View view)
	{
		ListView itemLv = null;
		itemLv = (ListView) view.findViewById(R.id.testItemListView);
		startBtn = (Button) view.findViewById(R.id.startBtn);
		MyCheckBoxAdapter adapter = new MyCheckBoxAdapter(this);
		itemLv.setAdapter((ListAdapter) adapter);
		itemLv.setItemsCanFocus(false);
		itemLv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		itemLv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int postion,
					long id) {
				// TODO Auto-generated method stub
				ViewHolder vholder = (ViewHolder)view.getTag();
				vholder.cBox.toggle();
				isSelected.put(postion, vholder.cBox.isChecked());
			}
		});
		startBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if(isServiceOn){
					isServiceOn = false;
					stopService(serviceIntent);
					startBtn.setText(R.string.start_test);
				}else{
					isServiceOn = true;
					startService(serviceIntent);
					startBtn.setText(R.string.stop_test);
				}
			}
		});
	}
	
	//这里写死了数据，暂时只对以下四个内容感兴趣
	public ArrayList<HashMap<String, Object>> getItemsData()
	{
		testItemsArray = new ArrayList<HashMap<String,Object>>();
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("item_name", TEST_ITEMS[0]);
		testItemsArray.add(map);
		map = new HashMap<String, Object>();
		map.put("item_name", TEST_ITEMS[1]);
		testItemsArray.add(map);
		map = new HashMap<String, Object>();
		map.put("item_name", TEST_ITEMS[2]);
		testItemsArray.add(map);
		map = new HashMap<String, Object>();
		map.put("item_name", TEST_ITEMS[3]);
		testItemsArray.add(map);
		
		return testItemsArray;
	}
	
	//实现listview中添加checkbox
	//大致逻辑是：checkbox不监听点击事件，单条listview监听事件，点击listview之后再手动设置checkbox的状态
	public class MyCheckBoxAdapter extends BaseAdapter
	{
        private LayoutInflater mInflater;
        private ArrayList<HashMap<String, Object>> mData;
        
        
        public MyCheckBoxAdapter(Context context)
        {
        	mInflater = LayoutInflater.from(context);
        	init();
        }
		
        
        private void init()
        {
        	mData = getItemsData();
        	isSelected = new HashMap<Integer, Boolean>();
        	int i;
        	for(i=0; i<mData.size(); i++){
        		isSelected.put(i, false);
        	}
        }
        
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return mData.size();
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View view, ViewGroup viewParent) {
			// TODO Auto-generated method stub
			ViewHolder holder = null;
			if(view == null){
				holder = new ViewHolder();
				view = mInflater.inflate(R.layout.test_items, null);
				holder.item = (TextView)view.findViewById(R.id.itemNameTextView);
				holder.cBox = (CheckBox)view.findViewById(R.id.checkBox);
				view.setTag(holder);
			}else{
				holder = (ViewHolder)view.getTag();
			}
			holder.item.setText(mData.get(position).get("item_name").toString());
			holder.cBox.setChecked(isSelected.get(position));
			return view;
		}
		
		public final class ViewHolder
		{
			public TextView item;
			public CheckBox cBox;
		}
	}
	
	//显示测试结果页面
	public void showResultListView(View view, ArrayList<HashMap<String, Object>> arrayList)
	{
		ListView resultsLv = null;
		resultsLv = (ListView) view.findViewById(R.id.resultListView);
		if(arrayList != null && resultsLv != null){
			SimpleAdapter adapter = new SimpleAdapter(this, arrayList, R.layout.result, new String[] {  "appName","time" }, 
					new int[] { R.id.appNameResultTextView, R.id.appTestTimeTextView });
			resultsLv.setAdapter((ListAdapter) adapter);
			resultsLv.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
						long arg3) {
					// TODO Auto-generated method stub
					
				}
			});
		}
	
	}
	
	//获取当前时间
	public String getCurrentTime()
	{
		String time = "";
		Calendar calendar = Calendar.getInstance();
		time = calendar.get(Calendar.YEAR)+"/"+calendar.get(Calendar.MONTH)+"/"+calendar.get(Calendar.DAY_OF_MONTH)+" "
			+calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE);
		return time;
	}
	
	//检查是否存在config文件
	public boolean checkConfigfile()
	{
		try{
			File f = new File(CONFIG_FILE_PATH);
			if(!f.exists()){
				return false;
			}
		}catch (Exception e) {
			// TODO: handle exception
			return false;
		}
//		showToast("true");
		return true;
		
	}
	
	public void showToast(String msg)
	{
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}
	
	//写入config文件
	public void writeConfigFile(String conf)
	{
		String fileName = CONFIG_NAME;
		String message = conf ;

		try{ 
			FileOutputStream fout = openFileOutput(fileName, MODE_PRIVATE);
			byte [] bytes = message.getBytes(); 
			fout.write(bytes); 
			fout.close(); 
			showToast("Saved!");
		} 
		catch(Exception e){ 
			e.printStackTrace(); 
		} 

	}
	
	//读取config文件里的内容
	public String readConfigFile()
	{
		String res=""; 
        try{ 
         FileInputStream fin = openFileInput(CONFIG_NAME); 
         int length = fin.available(); 
         byte [] buffer = new byte[length]; 
         fin.read(buffer);     
         res = EncodingUtils.getString(buffer, "UTF-8"); 
         fin.close();     
        } 
        catch(Exception e){ 
         e.printStackTrace(); 
        } 
        return res; 
	}
	
	//获取当前的post url
	public static String getPostUrl()
	{
		return postUrl;
	}
	
	//打开一个弹出窗口，编辑config文件
	public void showEditConfigDialog()
	{
		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.edit_config, (ViewGroup) findViewById(R.id.edit_config_dialog));
		final EditText et1 = (EditText)layout.findViewById(R.id.edit_config_et);
		et1.setText(readConfigFile());
		new AlertDialog.Builder(this).setTitle("Edit config").setView(layout)
		.setPositiveButton("save",new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				writeConfigFile(et1.getText().toString());
				/**根据返回成功的信息，取出里面的字符串转换为json进行取值*/  
				try {
					JSONObject obj = new JSONObject(et1.getText().toString());
					postUrl = obj.getString("post_url").replace("\\", "");
					System.out.print("dfdf");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		})
		.setNegativeButton("cancel", null)
		.show();
	}
}
