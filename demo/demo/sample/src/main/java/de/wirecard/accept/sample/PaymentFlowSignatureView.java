/**
 *  Copyright (c) 2015 Wirecard. All rights reserved.
 *
 *  Accept SDK for Android
 *
 */
package de.wirecard.accept.sample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.io.ByteArrayOutputStream;

public class PaymentFlowSignatureView extends View {

    private Paint signaturePaint;
    private Bitmap signatureBitmap;
    private Canvas signatureCanvas;
    private Path signaturePath;
    private Paint signatureBitmapPaint;

    private boolean wasTouched = false;

    public PaymentFlowSignatureView(Context context) {
        super(context);
        initPainting();
    }

    public PaymentFlowSignatureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPainting();
    }

    public PaymentFlowSignatureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPainting();
    }

    private void initPainting() {
        signatureBitmapPaint = new Paint(Paint.DITHER_FLAG);
        signaturePaint = new Paint();
        signaturePaint.setDither(true);
        signaturePaint.setAntiAlias(true);
        signaturePaint.setColor(Color.BLUE);
        signaturePaint.setStrokeCap(Paint.Cap.ROUND);
        signaturePaint.setStyle(Paint.Style.STROKE);
        signaturePaint.setStrokeJoin(Paint.Join.ROUND);
        signaturePaint.setStrokeWidth(3);
        signaturePath = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        signatureBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        signatureCanvas = new Canvas(signatureBitmap);
    }

    public boolean isSomethingDrawn() {
        return wasTouched;
    }

    public void clear() {
        wasTouched = false;
        if (signatureBitmap != null) {
            signatureBitmap = Bitmap.createBitmap(signatureBitmap.getWidth(), signatureBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            signatureCanvas = new Canvas(signatureBitmap);
        }
    }

    public Bitmap getSignatureBitmap() {
        if ( signatureBitmap == null ) {
            return null;
        }
        return Bitmap.createBitmap(signatureBitmap);
    }

    public byte[] compressSignatureBitmapToPNG() {
        final Bitmap scaledBitmap = Bitmap.createScaledBitmap(signatureBitmap, signatureBitmap.getWidth()/2, signatureBitmap.getHeight()/2, true);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        final byte[] result =  outputStream.toByteArray();
        return result;
    }

    @Override
    protected void onDraw(Canvas viewCanvas) {
        super.onDraw(viewCanvas);
        viewCanvas.drawBitmap(signatureBitmap, 0, 0, signatureBitmapPaint);
        viewCanvas.drawPath(signaturePath, signaturePaint);
    }

    private float lastX, lastY;
    private static final float TOUCH_TOLERANCE = 4;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handleMotionEventActionDown(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                handleMotionEventActionMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                handleMotionEventActionUp();
                invalidate();
                break;
        }
        return true;
    }

    private void handleMotionEventActionDown(float x, float y) {
        signaturePath.reset();
        signaturePath.moveTo(x, y);
        lastX = x;
        lastY = y;
    }

    private void handleMotionEventActionMove(float x, float y) {
        float dx = Math.abs(x - lastX);
        float dy = Math.abs(y - lastY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            signaturePath.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);
            lastX = x;
            lastY = y;
        }
    }

    private void handleMotionEventActionUp() {
        signaturePath.lineTo(lastX, lastY);
        signatureCanvas.drawPath(signaturePath, signaturePaint);
        wasTouched = true;
        signaturePath.reset();
    }
}
