package com.example.android.camera2basic.data;

import java.util.ArrayList;

public class DataStore {

    private static DataStore mDataStore;
    private static ArrayList<ItemData> itemDataArrayList;
    private static ArrayList<ItemData> itemDataPickedList;

    private DataStore() {
        //ToDo here
        itemDataArrayList = new ArrayList<>();
        itemDataPickedList = new ArrayList<>();
    }

    public static DataStore getInstance() {
        if (mDataStore == null) {
            mDataStore = new DataStore();
        }
        return mDataStore;
    }

    public static ArrayList<ItemData> getItemDataArrayList(){
        return itemDataArrayList;
    }
    public static void setItemDataArrayList(ArrayList<ItemData> data){
        itemDataArrayList = data;
    }
    public static ArrayList<ItemData> getItemDataPickedList(){
        return itemDataArrayList;
    }
    public static void setItemDataPickedList(ArrayList<ItemData> data){
        itemDataArrayList = data;
    }

}