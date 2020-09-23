package com.example.android.camera2basic.data;

import com.example.android.camera2basic.util.GlobalConstants;

import java.util.ArrayList;

public class DataStore {

    private static DataStore mDataStore;
    private static ArrayList<ItemData> itemDataArrayList;
    //private static ArrayList<ItemData> itemDataPickedList;
    private static int mPosition = 0;

    private DataStore() {
        //ToDo here
        itemDataArrayList = new ArrayList<>();
        //itemDataPickedList = new ArrayList<>();
    }

    public static DataStore getInstance() {
        if (mDataStore == null) {
            mDataStore = new DataStore();
        }
        return mDataStore;
    }

    public static ArrayList<ItemData> getItemDataArrayList() {
        return itemDataArrayList;
    }

    public static void setItemDataArrayList(ArrayList<ItemData> data) {
        itemDataArrayList = data;
    }

    public static ArrayList<ItemData> getItemDataPickedList() {
        return itemDataArrayList;
    }

    public static void setItemDataPickedList(ArrayList<ItemData> data) {
        for (ItemData itemData : data) {
            itemDataArrayList.add(itemData.getId(), itemData);
        }
    }

    public static ItemData getCurrentItem() {
        int count = getmPosition();
        if (GlobalConstants.PENDING) {
            for (int i = count; i < itemDataArrayList.size() - 1; i++) {
                setmPosition(i);
                if (!itemDataArrayList.get(i).isItemFound()) {
                    return itemDataArrayList.get(i);
                }
            }
        }
        return itemDataArrayList.get(mPosition);
    }

    public static void setItemDataPickedList(ItemData data) {
        itemDataArrayList.set(data.getId(), data);
    }

    /*public static void addItemDataPickedList(ItemData data){
        itemDataPickedList.add(data);
    }*/

    public static boolean hasItemDataPickedList(ItemData data) {
        if (itemDataArrayList.size() > 0) {
            for (ItemData itemData : itemDataArrayList) {
                if (itemData.getId() == data.getId()) {
                    return true;
                }
            }
        }
        return false;
    }


    public static boolean isItemPending() {
        if (itemDataArrayList.size() > 0) {
            for (ItemData itemData : itemDataArrayList) {
                if (!itemData.isItemFound()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static ArrayList<ItemData> getPendingItems() {
        ArrayList<ItemData> pendingData = new ArrayList<>();
        for (ItemData itemData : itemDataArrayList) {
            if (!itemData.isItemFound()) {
                pendingData.add(itemData);
            }
        }
        return pendingData;
    }

    public static int getPendingItemsCount() {
        int count = 0;
        for (ItemData itemData : itemDataArrayList) {
            if (!itemData.isItemFound()) {
                count++;
            }
        }
        return count;
    }

    public static int getCount() {
        if (GlobalConstants.PENDING) {
            return getPendingItemsCount();
        } else {
            return getItemDataArrayList().size();
        }
    }

    public static int getmPosition() {
        return mPosition;
    }

    public static void setmPosition(int position) {
        mPosition = position;
    }

    public static void clearData() {
        //itemDataPickedList.clear();
        itemDataArrayList.clear();
    }
}