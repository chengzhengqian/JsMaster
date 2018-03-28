package com.serendipity.chengzhengqian.jsmaster;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class WindowsListAdaptor extends BaseAdapter implements ListAdapter {
    private ArrayList<String> list = new ArrayList<String>();
    private MainActivity context;

    public WindowsListAdaptor(ArrayList<String> s, MainActivity ctx) {
        list=s;
        context=ctx;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }
    private Button createWindowsBtn(String text){
        final Button b=new Button(context);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setTextColor(Color.GREEN);
        b.setText(text);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                context.toggleMenus(view);
                context.setCurrrentView((String) b.getText());
            }
        });
        return b;
    }
    private Button closeWindowBtn(final String text){
        final Button b=new Button(context);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setTextColor(Color.RED);
        b.setText("   x   ");
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                context.toggleMenus(view);
                LinearLayout l=context.windows.remove(text);
                context.mainLayout.removeView(l);
                context.setCurrrentView(context.jsLogTag);
            }
        });
        return b;
    }
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LinearLayout l=new LinearLayout(context);
        String name=list.get(i);

        l.addView(createWindowsBtn(name));
        if(context.windows.get(name)!=null){
            l.addView(closeWindowBtn(name));
        }


        return l;
    }
}
