package com.bbk.lling.multitaskdownload.activitys;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.bbk.lling.multitaskdownload.R;
import com.bbk.lling.multitaskdownload.adapters.AppContentAdapter;
import com.bbk.lling.multitaskdownload.beans.AppContent;
import com.bbk.lling.multitaskdownload.db.DownloadFileDAO;
import com.bbk.lling.multitaskdownload.downloador.Constants;
import com.bbk.lling.multitaskdownload.downloador.Downloador;
import com.bbk.lling.multitaskdownload.utils.DownloadUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends Activity {
    public static final String TAG = "MainActivity";
    private AppContentAdapter mAdapter;
    private ListView mListView;
    private List<AppContent> mList;
    private Map<String, Downloador> downloadorMap;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView = (ListView) findViewById(R.id.listview);
        mAdapter = new AppContentAdapter(this);
        // 需要下载的数据。里面都是url连接。
        mList = DownloadUtils.getTestData();
        initStatus();
        mAdapter.setDates(mList);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppContent appContent = mList.get(position);
                if (downloadorMap != null && downloadorMap.containsKey(appContent.getUrl())) {
                    downloadorMap.get(appContent.getUrl()).pause();
                    downloadorMap.remove(appContent.getUrl());
                    appContent.setStatus(AppContent.Status.PAUSED);
                    mAdapter.notifyDataSetChanged();
                } else {
                    Downloador downloador = new Downloador(MainActivity.this, appContent);
                    downloador.download();
                    appContent.setStatus(AppContent.Status.WAITING);
                    mAdapter.notifyDataSetChanged();
                    if (downloadorMap == null) {
                        downloadorMap = new HashMap<String, Downloador>();
                    }
                    downloadorMap.put(appContent.getUrl(), downloador);
                }
            }
        });
        // 通过广播来发送通知。
        IntentFilter intent = new IntentFilter(Constants.DOWNLOAD_MSG);
        registerReceiver(downloadStatusReceiver, intent);


        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }

    }

    /**
     * 初始化下载状态  (重新赋值)
     */
    private void initStatus() {
        // 把历史数据赋值
        List<AppContent> list = DownloadFileDAO.getInstance(this.getApplicationContext()).getAll();
        for (AppContent appContent : list) {
            for (AppContent app : mList) {


                if (app.getUrl().equals(appContent.getUrl())) {
                    app.setStatus(appContent.getStatus());
                    app.setDownloadPercent(appContent.getDownloadPercent());


                    // 如果用户删除了文件，需要用这个判断。
                    if (appContent.getStatus() == AppContent.Status.FINISHED) {
                        String appfile = app.getUrl().substring(app.getUrl().lastIndexOf("/") + 1);
                        if (!has(appfile)) {
                            app.setStatus(AppContent.Status.PENDING);
                            app.setDownloadPercent(0);
                            DownloadFileDAO.getInstance(this).updateDownloadFile(app);
                        }
                    }
                    break;
                }


            }
        }
    }


    public Boolean has(String filename) {
        File downpath = new File(DownloadUtils.getDownloadPath());
        String[] arr = downpath.list();
        String filepath =null;
        for (String string : arr) {
            if (string.equals(filename)) {
                filepath = string;
                break;
            }
        }
        return filepath!=null?true:false;
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(downloadStatusReceiver);
        super.onDestroy();
    }

    private BroadcastReceiver downloadStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 这个intent 在Downloader方法中传值。
            AppContent appContent = intent.getParcelableExtra("appContent");
            if (appContent == null) return;
            int itemIndex = 0;
            // 重新为 mList 赋值
            for (AppContent appContent1 : mList) {
                if (appContent.getUrl().equals(appContent1.getUrl())) {
                    itemIndex = mList.indexOf(appContent1);
                    appContent1.setDownloadPercent(appContent.getDownloadPercent());
                    if (appContent.getDownloadPercent() == 99 || appContent.getDownloadPercent() == 100) {
                        Log.e("thisb-->", appContent.getDownloadPercent() + "");
                        Log.e("thisb-->", appContent.getDownloadPercent() + "");
                    }
                    break;
                }
            }
            updateView(itemIndex);
        }
    };

    private void updateView(int itemIndex) {
        //得到第一个可显示控件的位置，
        int visiblePosition = mListView.getFirstVisiblePosition();
        //只有当要更新的view在可见的位置时才更新，不可见时，跳过不更新
        if (itemIndex - visiblePosition >= 0) {
            //得到要更新的item的view
            View view = mListView.getChildAt(itemIndex - visiblePosition);
            //调用adapter更新界面
            mAdapter.updateView(view, itemIndex);
        }
    }

}
