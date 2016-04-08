package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Random;


/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    protected Bitmap _offScreenBitmap = null;
    protected Bitmap _loadedBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float _minBrushRadius = 20;
    private Rect bounds;
    private long distance = 0;
    private long velocity = 0;
    private int upper = 150;
    private int lower = 20;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.STROKE);
        _paint.setStrokeWidth(45);
        _paint.setStrokeCap(Paint.Cap.ROUND);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    public void setMyImageBitmap(Bitmap bitmap){
        bounds = getBitmapPositionInsideImageView(_imageView);
        _loadedBitmap = Bitmap.createScaledBitmap(bitmap,bounds.width(),bounds.height(),true);
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){

        if(brushType == BrushType.Circle) {
            _paint.setStrokeCap(Paint.Cap.ROUND);
            _brushType = BrushType.Circle;
        } else if (brushType == BrushType.Square){
            _paint.setStrokeCap(Paint.Cap.SQUARE);
            _brushType = BrushType.Square;
        } else if (brushType == BrushType.CircleSplatter){
            _paint.setStrokeCap(Paint.Cap.ROUND);
            _brushType = BrushType.CircleSplatter;
        } else if (brushType == BrushType.Line) {
            _paint.setStrokeCap(Paint.Cap.SQUARE);
            _brushType = BrushType.CircleSplatter;
        } else if (brushType == BrushType.LineSplatter){
            _paint.setStrokeCap(Paint.Cap.ROUND);
            _brushType = BrushType.LineSplatter;
        }
    }
    /**
     * Clears the painting
     */
    public void clearPainting(){
        if(_offScreenCanvas != null) {
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            _offScreenCanvas.drawRect(0, 0, this.getWidth(), this.getHeight(), paint);
            invalidate();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }
        //Toast.makeText(getContext(), ""+scaleX, Toast.LENGTH_LONG).show();
        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        bounds = getBitmapPositionInsideImageView(_imageView);
        canvas.drawRect(bounds, _paintBorder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){

        //Basically, the way this works is to liste for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location
        float currTouchX = motionEvent.getX();
        float currTouchY = motionEvent.getY();

        if(_brushType == BrushType.CircleSplatter) {
            long startTime = SystemClock.elapsedRealtime();
            long _elapsedTime = startTime - _lastPointTime;
            float firstTouchX = currTouchX;
            float firstTouchY = currTouchY;

            try {
                firstTouchX = motionEvent.getHistoricalX(0);
                firstTouchY = motionEvent.getHistoricalY(0);
            } catch (Exception e) {

            }
            distance = (long) Math.sqrt(((currTouchX - firstTouchX) * (currTouchX - firstTouchX)) + ((currTouchY - firstTouchY) * (currTouchY - firstTouchY)));
            if(_elapsedTime == 0){
                _elapsedTime = 10;
            }
            velocity = distance / _elapsedTime;

            _paint.setStrokeWidth(_minBrushRadius * Math.min(15,(1 + velocity)));
        } else {
            _paint.setStrokeWidth(45);
        }

        switch(motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                int historySize = motionEvent.getHistorySize();

                for (int i = 0; i < historySize; i++){
                    float touchX = motionEvent.getHistoricalX(i);
                    float touchY = motionEvent.getHistoricalY(i);

                    if(bounds.contains((int) touchX,(int) touchY)) {
                        int pixel = _loadedBitmap.getPixel((int)touchX-bounds.left,(int)touchY-bounds.top);

                        if(_brushType == BrushType.LineSplatter) {
                            _paint.setColor(pixel);
                            _paint.setAlpha(150);
                            _paint.setStrokeWidth(35);
                            _offScreenCanvas.drawPoint(touchX - (int) (Math.random() * (upper - lower)) + lower, touchY - (int) (Math.random() * (upper - lower)) + lower, _paint);
                            _paint.setStrokeWidth(18);
                            _offScreenCanvas.drawPoint(touchX + (int) (Math.random() * (upper - lower)) + lower, touchY - (int) (Math.random() * (upper - lower)) + lower, _paint);
                            _paint.setStrokeWidth(62);
                            _offScreenCanvas.drawPoint(touchX + (int) (Math.random() * (upper - lower)) + lower, touchY - (int) (Math.random() * (upper - lower)) + lower, _paint);
                            _paint.setStrokeWidth(100);
                            _offScreenCanvas.drawPoint(touchX - (int) (Math.random() * (upper - lower)) + lower, touchY + (int) (Math.random() * (upper - lower)) + lower, _paint);
                            _paint.setStrokeWidth(20);
                            _offScreenCanvas.drawPoint(touchX - (int) (Math.random() * (upper - lower)) + lower, touchY - (int) (Math.random() * (upper - lower)) + lower, _paint);
                            _paint.setStrokeWidth(15);
                            _offScreenCanvas.drawPoint(touchX + (int) (Math.random() * (upper - lower)) + lower, touchY + (int) (Math.random() * (upper - lower)) + lower, _paint);

                        } else {
                            _paint.setColor(pixel);
                            _paint.setAlpha(_alpha);
                            _offScreenCanvas.drawPoint(touchX, touchY, _paint);
                        }
                    }
                }
                if(bounds.contains((int) currTouchX,(int)currTouchY)) {
                    int pixel = _loadedBitmap.getPixel((int) currTouchX-bounds.left,(int)currTouchY-bounds.top);
                    if (_brushType == BrushType.LineSplatter) {
                        _paint.setColor(pixel);
                        _paint.setAlpha(150);
                        _paint.setStrokeWidth(35);
                        _offScreenCanvas.drawPoint(currTouchX - (int) (Math.random() * (upper - lower)) + lower, currTouchY - (int) (Math.random() * (upper - lower)) + lower, _paint);
                        _paint.setStrokeWidth(18);
                        _offScreenCanvas.drawPoint(currTouchX + (int) (Math.random() * (upper - lower)) + lower, currTouchY + (int) (Math.random() * (upper - lower)) + lower, _paint);
                        _paint.setStrokeWidth(62);
                        _offScreenCanvas.drawPoint(currTouchX + (int) (Math.random() * (upper - lower)) + lower, currTouchY - (int) (Math.random() * (upper - lower)) + lower, _paint);
                        _paint.setStrokeWidth(100);
                        _offScreenCanvas.drawPoint(currTouchX - (int) (Math.random() * (upper - lower)) + lower, currTouchY + (int) (Math.random() * (upper - lower)) + lower, _paint);
                        _paint.setStrokeWidth(20);
                        _offScreenCanvas.drawPoint(currTouchX - (int) (Math.random() * (upper - lower)) + lower, currTouchY - (int) (Math.random() * (upper - lower)) + lower, _paint);
                        _paint.setStrokeWidth(15);
                        _offScreenCanvas.drawPoint(currTouchX + (int) (Math.random() * (upper - lower)) + lower, currTouchY + (int) (Math.random() * (upper - lower)) + lower, _paint);

                    } else {
                        _paint.setColor(pixel);
                        _paint.setAlpha(_alpha);
                        _offScreenCanvas.drawPoint(currTouchX, currTouchY, _paint);
                    }
                }

                _lastPointTime = SystemClock.elapsedRealtime();
            break;
            case MotionEvent.ACTION_UP:
                _lastPointTime = -1;
                break;
        }
        invalidate();
        return true;
    }


    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */


    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);


        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

