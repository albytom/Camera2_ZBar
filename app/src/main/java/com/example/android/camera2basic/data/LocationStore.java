package com.example.android.camera2basic.data;

import java.util.ArrayList;

public class LocationStore {

    private static LocationStore sLocationStore;
    private static ArrayList<LocationItem> sLocationItemArrayList;

    private LocationStore() {
        //ToDo here
        sLocationItemArrayList = new ArrayList<>();
    }

    public static LocationStore getInstance() {
        if (sLocationStore == null) {
            sLocationStore = new LocationStore();
        }
        return sLocationStore;
    }
    public static ArrayList<LocationItem> getLocArrayList() {
        return sLocationItemArrayList;
    }
    public static void addToLocArrayList(LocationItem pBeaconLoc){
        sLocationItemArrayList.add(pBeaconLoc);
    }

    public static void setLocArrayList(ArrayList<LocationItem> pBeaconLoc) {
        sLocationItemArrayList = pBeaconLoc;
    }

    public static LocationItem getItem(String pName) {
            for (LocationItem vLocationItem: sLocationItemArrayList) {
                if (vLocationItem.getName().equalsIgnoreCase(pName)) {
                    return vLocationItem;
                }
            }
            return null;
    }
}
