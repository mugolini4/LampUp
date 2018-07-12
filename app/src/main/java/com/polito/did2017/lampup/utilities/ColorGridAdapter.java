package com.polito.did2017.lampup.utilities;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;

import com.polito.did2017.lampup.R;

import java.util.ArrayList;

/**
 * Created by marco on 20/06/2018.
 */

public class ColorGridAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<Integer> colors;

    public ColorGridAdapter(Context context, ArrayList<Integer> colors) {
        this.context = context;
        this.colors = colors;
    }

    @Override
    public int getCount() {
        return colors.size();
    }

    @Override
    public Object getItem(int i) {
        return colors.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if(view==null) {
            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = li.inflate(R.layout.item_color, null);
        }

        ImageButton circle = view.findViewById(R.id.color);
        circle.setBackgroundColor(colors.get(i));

//        if(view==null) {
//            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            view = li.inflate(R.layout.item_light_color, null);
//        }
//
//        View circle = view.findViewById(R.id.light_color);
//        GradientDrawable light = (GradientDrawable) context.getResources().getDrawable(R.drawable.shape_background_light_color);
//        light.setColor(colors.get(i));
//        circle.setBackground(light);

        return view;
    }
}
