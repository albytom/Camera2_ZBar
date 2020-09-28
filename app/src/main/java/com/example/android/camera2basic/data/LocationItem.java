package com.example.android.camera2basic.data;

public class LocationItem {


  private int ky;
  private int kx;
  private String name;

    public LocationItem(int pY, int pX, String pName) {
        ky = pY;
        kx = pX;
        name = pName;
    }


    public int getKy() {
        return ky;
    }

    public void setKy(int pKy) {
        ky = pKy;
    }

    public int getKx() {
        return kx;
    }

    public void setKx(int pKx) {
        kx = pKx;
    }

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }
}
