package com.example.android.camera2basic.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.example.android.camera2basic.R;
import com.example.android.camera2basic.data.ItemData;

import java.util.ArrayList;

public class ItemGridAdapter extends RecyclerView.Adapter<ItemGridAdapter.ViewHolder> {

    private ArrayList<ItemData> mData;
    private LayoutInflater mInflater;

    // data is passed into the constructor
    public ItemGridAdapter(Context context, ArrayList<ItemData> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    // inflates the cell layout from xml when needed
    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_item, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each cell
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.iTitleTv.setText(mData.get(position).getItemName());
        holder.iLocTv.setText(mData.get(position).getItemLoc());
        holder.iContentTv.setText(mData.get(position).getItemContent());
        holder.itemFound.setChecked(mData.get(position).isItemFound());
    }

    // total number of cells
    @Override
    public int getItemCount() {
        return mData.size();
    }
    // stores and recycles views as they are scrolled off screen
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView iTitleTv, iLocTv, iContentTv;
        CheckBox itemFound;

        ViewHolder(View itemView) {
            super(itemView);
            iTitleTv = itemView.findViewById(R.id.i_title_tv);
            iLocTv = itemView.findViewById(R.id.i_loc_tv);
            iContentTv = itemView.findViewById(R.id.i_content_tv);
            itemFound = itemView.findViewById(R.id.item_found);
        }
    }
}