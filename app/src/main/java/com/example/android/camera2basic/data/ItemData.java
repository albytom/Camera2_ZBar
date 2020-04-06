package com.example.android.camera2basic.data;

public class ItemData {
    private int mId;
    private String itemName;
    private String itemLoc;
    private String itemContent;
    private boolean itemFound;

    public ItemData(int id, String itemName, String itemLoc) {
        this.mId = id;
        this.itemName = itemName;
        this.itemLoc = itemLoc;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemLoc() {
        return itemLoc;
    }

    public void setItemLoc(String itemLoc) {
        this.itemLoc = itemLoc;
    }

    public String getItemContent() {
        return itemContent;
    }

    public void setItemContent(String itemContent) {
        this.itemContent = itemContent;
    }

    public boolean isItemFound() {
        return itemFound;
    }

    public void setItemFound(boolean itemFound) {
        this.itemFound = itemFound;
    }

    public int getId() {
        return mId;
    }

    public void setId(int mId) {
        this.mId = mId;
    }
}
