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
        return itemDataPickedList;
    }
    public static void setItemDataPickedList(ArrayList<ItemData> data){
        for(ItemData itemData: data){
            itemDataPickedList.add(itemData.getId(), itemData);
        }
    }
    public static void setItemDataPickedList(ItemData data){
            itemDataPickedList.set(data.getId(), data);
    }

    public static void addItemDataPickedList(ItemData data){
        itemDataPickedList.add(data);
    }

    public static boolean hasItemDataPickedList(ItemData data){
        if (itemDataPickedList.size() > 0) {
            for(ItemData itemData: itemDataPickedList){
                if(itemData.getId() == data.getId()){
                    return true;
                }
            }
        }
        return false;
    }


    public static boolean isItemPending(){
        if (itemDataPickedList.size() > 0) {
            for(ItemData itemData: itemDataPickedList){
                if(!itemData.isItemFound()){
                    return true;
                }
            }
        }
        return false;
    }
    public static ArrayList<ItemData> getPendingItems(){
        ArrayList<ItemData> pendingData = new ArrayList<>();
        for(ItemData itemData: itemDataPickedList){
            if (!itemData.isItemFound()) {
                pendingData.add(itemData);
            }
        }
        return pendingData;
    }

    public static void clearData(){
        itemDataPickedList.clear();
        itemDataArrayList.clear();
    }
}