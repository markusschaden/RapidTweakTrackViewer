package com.rapidtweak.track3dviewer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;

import java.util.List;

/**
 * Created by Markus on 19.10.2015.
 */
public class TrackSurfaceView extends View {
    private List<Double[]> points;
    private Paint pathPaint;
    private Path path = new Path();
    private int centerX = 1000;
    private int centerY = 600;

    //These two constants specify the minimum and maximum zoom
    private static float MIN_ZOOM = 1f;
    private static float MAX_ZOOM = 5f;

    private float scaleFactor = 1.f;
    private ScaleGestureDetector detector;

    //These constants specify the mode that we're in
    private static int NONE = 0;
    private static int DRAG = 1;
    private static int ZOOM = 2;

    private int mode;

    //These two variables keep track of the X and Y coordinate of the finger when it first
    //touches the screen
    private float startX = 0f;
    private float startY = 0f;

    //These two variables keep track of the amount we need to translate the canvas along the X
    //and the Y coordinate
    private float translateX = 0f;
    private float translateY = 0f;

    //These two variables keep track of the amount we translated the X and Y coordinates, the last time we
    //panned.
    private float previousTranslateX = 0f;
    private float previousTranslateY = 0f;

    private boolean dragged = false;
    private float displayWidth;
    private float displayHeight;


    public TrackSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        pathPaint = new Paint();
        pathPaint.setAntiAlias(true);
        pathPaint.setDither(true);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeJoin(Paint.Join.MITER);
        pathPaint.setStrokeCap(Paint.Cap.SQUARE);
        pathPaint.setColor(Color.RED);
        pathPaint.setStrokeWidth(16);
        pathPaint.setAlpha(100);

        path.moveTo(centerX, centerY);

        detector = new ScaleGestureDetector(getContext(), new ScaleListener());

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        displayWidth = display.getWidth();
        displayHeight = display.getHeight();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction() & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN:
                mode = DRAG;

                //We assign the current X and Y coordinate of the finger to startX and startY minus the previously translated
                //amount for each coordinates This works even when we are translating the first time because the initial
                //values for these two variables is zero.
                startX = event.getX() - previousTranslateX;
                startY = event.getY() - previousTranslateY;
                break;

            case MotionEvent.ACTION_MOVE:
                translateX = event.getX() - startX;
                translateY = event.getY() - startY;

                //We cannot use startX and startY directly because we have adjusted their values using the previous translation values.
                //This is why we need to add those values to startX and startY so that we can get the actual coordinates of the finger.
                double distance = Math.sqrt(Math.pow(event.getX() - (startX + previousTranslateX), 2) +
                                Math.pow(event.getY() - (startY + previousTranslateY), 2)
                );

                if(distance > 0) {
                    dragged = true;
                }

                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                mode = ZOOM;
                break;

            case MotionEvent.ACTION_UP:
                mode = NONE;
                dragged = false;

                //All fingers went up, so let's save the value of translateX and translateY into previousTranslateX and
                //previousTranslate
                previousTranslateX = translateX;
                previousTranslateY = translateY;
                break;

            case MotionEvent.ACTION_POINTER_UP:
                mode = DRAG;

                //This is not strictly necessary; we save the value of translateX and translateY into previousTranslateX
                //and previousTranslateY when the second finger goes up
                previousTranslateX = translateX;
                previousTranslateY = translateY;
                break;
        }

        detector.onTouchEvent(event);

        //We redraw the canvas only in the following cases:
        //
        // o The mode is ZOOM
        //        OR
        // o The mode is DRAG and the scale factor is not equal to 1 (meaning we have zoomed) and dragged is
        //   set to true (meaning the finger has actually moved)
        if ((mode == DRAG && scaleFactor != 1f && dragged) || mode == ZOOM) {
            invalidate();
        }

        return true;
    }

    public void setPoints(List<Double[]> points) {
        this.points = points;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();

        //We're going to scale the X and Y coordinates by the same amount
        canvas.scale(scaleFactor, scaleFactor);

        //If translateX times -1 is lesser than zero, let's set it to zero. This takes care of the left bound
        if((translateX * -1) < 0) {
            translateX = 0;
        }

        //This is where we take care of the right bound. We compare translateX times -1 to (scaleFactor - 1) * displayWidth.
        //If translateX is greater than that value, then we know that we've gone over the bound. So we set the value of
        //translateX to (1 - scaleFactor) times the display width. Notice that the terms are interchanged; it's the same
        //as doing -1 * (scaleFactor - 1) * displayWidth
        else if((translateX * -1) > (scaleFactor - 1) * displayWidth) {
            translateX = (1 - scaleFactor) * displayWidth;
        }

        if(translateY * -1 < 0) {
            translateY = 0;
        }

        //We do the exact same thing for the bottom bound, except in this case we use the height of the display
        else if((translateY * -1) > (scaleFactor - 1) * displayHeight) {
            translateY = (1 - scaleFactor) * displayHeight;
        }

        //We need to divide by the scale factor here, otherwise we end up with excessive panning based on our zoom level
        //because the translation amount also gets scaled according to how much we've zoomed into the canvas.
        canvas.translate(translateX / scaleFactor, translateY / scaleFactor);



        int centerY = canvas.getHeight() / 2;
        int centerX = canvas.getWidth() / 2;

        Path path = new Path();
        path.moveTo(centerX, centerY);

        for (int i = 0; i < points.size(); i++) {
            Double[] d = points.get(i);
            path.lineTo((float) (d[0] + centerX), (float) (d[1] + centerY));
        }
        /*if (path != null) {
            Path path = new Path();
            synchronized (points) {
                path.moveTo(centerX, centerY);
                for (Double[] d : points) {
                    //path.moveTo((float) d[0].doubleValue()+centerX, (float) d[1].doubleValue()+centerY);
                    path.rLineTo((float)d[0].doubleValue(), (float)d[1].doubleValue());
                    Log.i("Path", (float) d[0].doubleValue() + ", " + (float) d[1].doubleValue());
                }
            }
            canvas.drawPath(path, pathPaint);
        }*/
        canvas.drawPath(path, pathPaint);

        canvas.restore();
    }

    public void addCoordinate(double x, double y) {

        points.add(new Double[]{x, y});
        //path.lineTo((float) (x + centerX), (float) (y + centerY));
    }


    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_ZOOM, Math.min(scaleFactor, MAX_ZOOM));
            return true;
        }
    }
}
