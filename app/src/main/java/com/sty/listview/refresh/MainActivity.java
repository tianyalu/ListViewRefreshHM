package com.sty.listview.refresh;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.ListView;

import com.sty.listview.refresh.adapter.MyAdapter;
import com.sty.listview.refresh.view.RefreshListView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private RefreshListView listView;
    private ArrayList<String> datas;
    private Context mContext;
    private MyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide(); //去除标题栏
        setContentView(R.layout.activity_main);
        mContext = this;

        listView = (RefreshListView) findViewById(R.id.listview);

        listView.setRefreshListener(new RefreshListView.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Thread(){
                    public void run() {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        datas.add(0, "我是下拉刷新出来的数据！");

                        runOnUiThread(new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.N)
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                                listView.onRefreshComplete();
                            }
                        });
                    }
                }.start();
            }

            @Override
            public void onLoadMore() {
                new Thread(){
                    public void run(){
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        datas.add("我是加载更多出来的数据!->1");
                        datas.add("我是加载更多出来的数据!->2");
                        datas.add("我是加载更多出来的数据!->3");

                        runOnUiThread(new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.N)
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                                listView.onRefreshComplete();
                            }
                        });
                    }
                }.start();
            }
        });

        datas = new ArrayList<>();
        for(int i = 0; i < 30; i++){
            datas.add("这是一条ListView数据：" + i);
        }

        //设置数据适配器
        adapter = new MyAdapter(mContext, datas);
        listView.setAdapter(adapter);
    }
}
