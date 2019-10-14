package com.silverback.carman2.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;

public class EdgeDots {

    // Objects
    private Bitmap bitmap;
    private Point point;
    private int id;

    // Constructor
    public EdgeDots(Context context, int resId, Point point, int id) {
        //this.context = context;
        this.id = id;
        this.point = point;
        bitmap = BitmapFactory.decodeResource(context.getResources(), resId);
    }

    public int getDotWidth() {
        return bitmap.getWidth();
    }

    public int getDotHeight() {
        return bitmap.getHeight();
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public int getX() {
        return point.x;
    }

    public int getY() {
        return point.y;
    }

    public int getID() {
        return id;
    }

    public void setX(int x) {
        point.x = x;
    }

    public void setY(int y) {
        point.y = y;
    }

    public void moveX(int x) {
        point.x = point.x + x;
    }

    public void moveY(int y) {
        point.y = point.y + y;
    }
}
