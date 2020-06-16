package com.example.android.camera2basic.data;

public class ItemData {
    private int mId;
    private String itemName;
    private String itemLoc;
    private String itemContent;
    private boolean itemFound;
    private String itemColor;
    private String itemSize;

    public ItemData(int id, String itemName, String itemLoc, String pItemSize, String pItemColor) {
        this.mId = id;
        this.itemName = itemName;
        this.itemLoc = itemLoc;
        this.itemSize = pItemSize;
        this.itemColor = pItemColor;
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

    public String getItemColor() {
        return itemColor;
    }

    public void setItemColor(String pItemColor) {
        itemColor = pItemColor;
    }

    public String getItemSize() {
        return itemSize;
    }

    public void setItemSize(String pItemSize) {
        itemSize = pItemSize;
    }
}
