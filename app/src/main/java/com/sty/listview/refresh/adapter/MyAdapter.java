package com.sty.listview.refresh.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Shi Tianyi on 2017/10/8/0008.
 */

public class MyAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<String> datas;

    public MyAdapter(Context context, ArrayList<String> datas){
        this.context = context;
        this.datas = datas;
    }

    @Override
    public int getCount() {
        if(datas != null && datas.size() > 0){
            return datas.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return datas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        TextView textView = new TextView(context);
        textView.setTextSize(18f);
        textView.setPadding(40, 20, 40, 20);
        textView.setText(datas.get(position));

        return textView;
    }
}
