package com.silverback.carman.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This class is a custom view which draws the cropping area in CropImageActivity, managing to resize
 * the shape of rectangle or circle by the mode the user set(rectangle currently not wokring).
 */
public class DrawEditorView extends View {

    private static final LoggingHelper log = LoggingHelperFactory.create(DrawEditorView.class);

    // Constants
    private static final int CROP_MODE_RECTANGLE = 1;
    private static final int CROP_MODE_CIRCLE = 2;
    private static final int INIT_SIZE = 200; // deault size of EdgeDots

    // Objects
    private Context context;
    private Bitmap mBitmap;

    private Paint paint, canvasPaint;
    private ArrayList<EdgeDots> arrDots;
    private RectF boundaryRectF, cropRectF;
    private Point p1, p3;
    private Point p2, p4;
    private Point initPoint;
    private float[] coords;

    // Fields
    //private float density;
    private float imgViewWidth, imgViewHeight;
    private float offsetX, offsetY;
    private float scale;
    private int radius;
    private int groupId = 2;
    private int dotId = -1;

    private int dotRds; // radius of the dots
    private int cropMode = CROP_MODE_CIRCLE;

    private boolean isBoundary;
    private int toolbarHeight;


    public DrawEditorView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public DrawEditorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        init();
    }
    public DrawEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    // Set the offset of the toolbar height invoked in CropImageActivity. Refac required.
    public void setToolbarHeight(int height) {
        toolbarHeight = height;
    }

    private void init() {

        setFocusable(true);
        paint = new Paint();
        canvasPaint = new Paint();

        Xfermode xfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
        canvasPaint.setColor(Color.parseColor("#AA000000")); // Darken the background.

        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#FFFFFF"));
        paint.setXfermode(xfermode);

        p1 = new Point();
        p2 = new Point();
        p3 = new Point();
        p4 = new Point();

        boundaryRectF = new RectF();
        cropRectF = new RectF();
        coords = new float[4];
        isBoundary = true;

        arrDots = new ArrayList<>();
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

    }

    // View invokes this only once during layout when the size of this view has changed,
    // to have the rectangle centered in the screen.
    @Override
    public void onSizeChanged(int newX, int newY, int oldX, int oldY) {
        //rectPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        log.i("Toolbar height: %s", toolbarHeight);

        // Set the initial points of each dotRds dots.
        p1.x = (newX - INIT_SIZE) / 2;
        p1.y = (newY - toolbarHeight - INIT_SIZE) / 2;
        p2.x = p1.x + INIT_SIZE;
        p2.y = p1.y;
        p3.x = p1.x + INIT_SIZE;
        p3.y = p1.y + INIT_SIZE;
        p4.x = p1.x;
        p4.y = p1.y + INIT_SIZE;

        radius = INIT_SIZE / 2;

        arrDots.add(0, new EdgeDots(context, R.drawable.reddot, p1, 0));
        arrDots.add(1, new EdgeDots(context, R.drawable.reddot, p2, 1));
        arrDots.add(2, new EdgeDots(context, R.drawable.reddot, p3, 2));
        arrDots.add(3, new EdgeDots(context, R.drawable.reddot, p4, 3));

        super.onSizeChanged(newX, newY, oldX, oldY);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawPaint(canvasPaint);
        int cx, cy;
        // Draw the 4 dots on every dotRds surrounding the circle.
        for(EdgeDots dot : arrDots) canvas.drawBitmap(dot.getBitmap(), dot.getX(), dot.getY(), null);

        // Get the size of EdgeDot(any dot is equal in size) in order for the rectangle to move
        // as long as half the width of the dot size for being center-located.
        dotRds = arrDots.get(0).getDotWidth() / 2;

        // When the rect is resized by dragging it on P1, P3(group1), it is transformed changing P2, P4 coords.
        if (groupId == 1) {
            if(p1.x < p3.x && p1.y < p3.y) {
                coords[0] = p1.x + dotRds;
                coords[1] = p1.y + dotRds;
                coords[2] = p3.x + dotRds;
                coords[3] = p3.y + dotRds;

                cx = p1.x + dotRds + radius;
                cy = p3.y + dotRds - radius;
            } else {
                coords[0] = p3.x + dotRds;
                coords[1] = p3.y + dotRds;
                coords[2] = p1.x + dotRds;
                coords[3] = p1.y + dotRds;
                //cropRectF.set(p3.x + dotRds, p3.y + dotRds, p1.x + dotRds, p1.y + dotRds);

                cx = p1.x + dotRds - radius;
                cy = p3.y + dotRds + radius;
            }
        } else {
            if(p2.x > p4.x && p2.y < p4.y){
                coords[0] = p4.x + dotRds;
                coords[1] = p2.y + dotRds;
                coords[2] = p2.x + dotRds;
                coords[3] = p4.y + dotRds;

                cx = p2.x + dotRds - radius;
                cy = p4.y + dotRds - radius;
            } else {
                coords[0] = p2.x + dotRds;
                coords[1] = p4.y + dotRds;
                coords[2] = p4.x + dotRds;
                coords[3] = p2.y + dotRds;

                cx = p2.x + dotRds + radius;
                cy = p4.y + dotRds + radius;
            }
        }

        cropRectF.set(coords[0], coords[1], coords[2], coords[3]);
        isBoundary = boundaryRectF.contains(cropRectF);

        switch(cropMode) {
            case CROP_MODE_RECTANGLE:
                canvas.drawRect(cropRectF, paint);
                break;
            case CROP_MODE_CIRCLE:
                canvas.drawCircle(cx, cy, radius, paint);
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int X = (int)(event.getX() /* * density*/);
        int Y = (int)(event.getY() /* * density*/);
        int action = event.getAction();


        radius = (groupId == 1) ? Math.abs(p2.x - p4.x) / 2 : Math.abs(p1.x - p3.x) / 2;
        isBoundary = boundaryRectF.contains(cropRectF);

        switch(action) {

            case MotionEvent.ACTION_DOWN:
                initPoint = new Point(X, Y);
                dotId = -1;

                // Check if the init point is within any arrDots
                for(EdgeDots dot : arrDots) {
                    int dotX = dot.getX() + dot.getDotWidth() / 2;
                    int dotY = dot.getY() + dot.getDotHeight() / 2;

                    double dotRadius = Math.sqrt((double)((dotX - X) * (dotX - X) + (dotY - Y) * (dotY - Y)));

                    if(dotRadius < dot.getDotWidth()) {
                        dotId = dot.getID();
                        groupId = (dotId == 1 || dotId == 3) ? 2 : 1;
                        break;
                    }
                }

                break;

            case MotionEvent.ACTION_MOVE:
                // Check if the cropping region is within the image.

                // Transform the crop region
                if(dotId > -1) {
                    // Allow to transform any type of rectangle
                    switch(cropMode) {

                        case CROP_MODE_RECTANGLE:
                            arrDots.get(dotId).setX(X);
                            arrDots.get(dotId).setY(Y);

                            break;

                        case CROP_MODE_CIRCLE:
                            // Force to maintain the shape as square by setting the diff to x coords.
                            int prevX = arrDots.get(dotId).getX();
                            int prevY = arrDots.get(dotId).getY();
                            arrDots.get(dotId).setX(X);
                            arrDots.get(dotId).setY(Y);
                            // if p1 and p3 or p2 and p4 are crossed over
                            int diffXY = (groupId == 1) ? arrDots.get(dotId).getX() - prevX :
                                    prevX - arrDots.get(dotId).getX();
                            // Fix getY() to getX() for enforcing the square shape.
                            arrDots.get(dotId).setY(prevY + diffXY); // Fix the shape to square

                            break;
                    }

                    switch(dotId) {
                        case 0:
                            arrDots.get(1).setY(arrDots.get(0).getY());
                            arrDots.get(3).setX(arrDots.get(0).getX());
                            break;
                        case 1:
                            arrDots.get(0).setY(arrDots.get(1).getY());
                            arrDots.get(2).setX(arrDots.get(1).getX());
                            break;
                        case 2:
                            arrDots.get(3).setY(arrDots.get(2).getY());
                            arrDots.get(1).setX(arrDots.get(2).getX());
                            break;
                        case 3:
                            arrDots.get(2).setY(arrDots.get(3).getY());
                            arrDots.get(0).setX(arrDots.get(3).getX());
                            break;
                    }


                    // Move the crop region w/o transforming
                } else {
                    if(initPoint != null) {
                        int diffX = X - initPoint.x;
                        int diffY = Y - initPoint.y;
                        initPoint.x = X;
                        initPoint.y = Y;

                        for (EdgeDots dot : arrDots) {
                            dot.moveX(diffX);
                            dot.moveY(diffY);
                        }
                    }


                }

                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                // The cropping region is foreced to move  within the image.
                //invalidate();
                if(!isBoundary) {
                    if(coords[0] < offsetX) for(EdgeDots dot : arrDots) dot.moveX((int)(offsetX - coords[0] + 1));
                    if(coords[1] < offsetY) for(EdgeDots dot : arrDots) dot.moveY((int)(offsetY - coords[1] + 1));
                    if(coords[2] > imgViewWidth - offsetX) for(EdgeDots dot : arrDots) dot.moveX((int)(imgViewWidth - offsetX - coords[2] - 1));
                    if(coords[3] > imgViewHeight - offsetY) for(EdgeDots dot : arrDots) dot.moveY((int)(imgViewHeight - offsetY - coords[3] - 1));
                    invalidate();
                }

                performClick();
                break;
        }


        invalidate();
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }



    /**
     * setScaleMatrix is to scale up or down the original image source to fit it to the view.
     * @param uri to locate the image source
     * @param imageView to display the image
     *
     *
     */
    // Invoked by ViewTreeObserver.OnPreDrawListener
    // Scale and translate the mBitmap fitting to the ImageView
    //public void setScaleMatrix(Uri uri, int bmX, int bmY, final ImageView imageView){
    public void setScaleMatrix(DisplayMetrics metrics, Uri uri, ImageView imageView){
        //public void setScaleMatrix(Uri uri, ImageView imageView) {

        imgViewWidth = imageView.getWidth();
        imgViewHeight = imageView.getHeight();

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        opts.inScreenDensity = metrics.densityDpi;
        opts.inTargetDensity =  metrics.densityDpi;
        //opts.inDensity = DisplayMetrics.DENSITY_DEFAULT;
        try {
            BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, opts);
        } catch(IOException e) {
            log.e("IOException: %s", e.getMessage());
        }

        // Calculate inSampeSize and downscale the image
        int bmpWidth = opts.outWidth;
        int bmpHeight = opts.outHeight;
        int reqWidth = (int)imgViewWidth / 2;
        int reqHeight = (int)imgViewWidth / 2;
        int inSampleSize = 1;

        if(bmpWidth > reqWidth || bmpHeight > reqHeight) {
            final int halfHeight = bmpHeight / 2;
            final int halfWidth = bmpWidth / 2;
            while((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        // Draw the downscaled bitmap
        opts.inJustDecodeBounds = false;
        opts.inSampleSize = inSampleSize;
        //opts.inScreenDensity = metrics.densityDpi;
        //opts.inTargetDensity =  metrics.densityDpi;
        //opts.inDensity = DisplayMetrics.DENSITY_DEFAULT;

        try {
            mBitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, opts);

        } catch(IOException e) {
            log.e("IOException: %s", e.getMessage());
        }


        // Get the scale when the image scales up or down to fit the screen size(match_parent)
        if(mBitmap == null) {
            return;
        }
        scale = Math.min(imgViewWidth / mBitmap.getWidth(), imgViewHeight / mBitmap.getHeight());


        // Apply the matrix to the ImageView and set the image.
        // isEmulator() is to detect whether the phone is emulator or real one. Required to erase when
        // relealsed.
        Matrix matrix = new Matrix();

        matrix.postScale(scale, scale);
        offsetX = (imgViewWidth - mBitmap.getWidth() * scale) / 2;
        offsetY = (imgViewHeight - mBitmap.getHeight() * scale) / 2;
        matrix.postTranslate(offsetX, offsetY);

        //Matrix.postTranslate() is ignored in Bitmap#createBitmap()
        imageView.setImageMatrix(matrix);
        //imageView.setImageURI(uri);
        imageView.setImageBitmap(mBitmap);

        // Set the image boundary with RectF
        boundaryRectF.set(offsetX, offsetY, imgViewWidth - offsetX, imgViewHeight - offsetY);
    }


    //public Bitmap getCroppedBitmap(Bitmap mBitmap, float x, float y, float scale) {
    public Bitmap getCroppedBitmap() {
        // Should be reprogrammed in such a manner that the out-of-bound region should be auto-cropped
        if(!isBoundary) {
            Toast.makeText(getContext(), "Out of bounds", Toast.LENGTH_SHORT).show();
            return null;
        }

        Matrix cropMatrix = new Matrix();
        float left, top, right, bottom;

        if(groupId == 1) {
            left = (p1.x < p3.x)? (p1.x + dotRds - offsetX) / scale  : (p3.x + dotRds - offsetX) / scale;
            top = (p1.y < p3.y)? (p1.y + dotRds - offsetY) / scale  : (p3.y + dotRds - offsetY) / scale;
            right = (p1.x < p3.x)? (p3.x + dotRds - offsetX) /scale : (p1.x + dotRds - offsetX) / scale;
            bottom = (p1.y < p3.y)? (p3.y + dotRds - offsetY) / scale : (p1.y + dotRds - offsetY) / scale;

        } else {
            left = (p2.x > p4.x)? (p4.x + dotRds - offsetX) / scale : (p2.x + dotRds - offsetX) / scale;
            top = (p2.y < p4.y)? (p2.y + dotRds - offsetY) / scale : (p4.y + dotRds - offsetY) / scale;
            right = (p2.x > p4.x)? (p4.x + dotRds - offsetX) / scale : (p2.x + dotRds - offsetX) /scale;
            bottom = (p2.y < p4.y)? (p4.y + dotRds - offsetY) / scale : (p2.y + dotRds - offsetY) / scale;
        }

        RectF scaledRectF = new RectF(left, top, right, bottom);
        cropMatrix.mapRect(scaledRectF);

        // Target size based on the circle size
        float dest = (radius * 2) / scale;

        return Bitmap.createBitmap(mBitmap, (int)left, (int)top, (int)dest, (int)dest, cropMatrix, false);
    }

    // Draw the area-indicating dots
    class EdgeDots {
        private Bitmap bitmap;
        private Point point;
        private int id;

        // Constructor
        EdgeDots(Context context, int resId, Point point, int id) {
            //this.context = context;
            this.id = id;
            this.point = point;
            bitmap = BitmapFactory.decodeResource(context.getResources(), resId);
        }

        int getDotWidth() {
            return bitmap.getWidth();
        }

        int getDotHeight() {
            return bitmap.getHeight();
        }

        Bitmap getBitmap() {
            return bitmap;
        }

        int getX() {
            return point.x;
        }

        int getY() {
            return point.y;
        }

        int getID() {
            return id;
        }

        void setX(int x) {
            point.x = x;
        }

        void setY(int y) {
            point.y = y;
        }

        void moveX(int x) {
            point.x = point.x + x;
        }

        void moveY(int y) {
            point.y = point.y + y;
        }
    }

}
