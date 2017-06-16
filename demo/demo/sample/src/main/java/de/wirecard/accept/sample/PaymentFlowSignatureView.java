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
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class PaymentFlowSignatureView extends View {

    private Paint signaturePaint;
    private Bitmap signatureBitmap;
    private Canvas signatureCanvas;
    private SVPath signaturePath;
    private Paint signatureBitmapPaint;

    private ArrayList<SVPath> paths = new ArrayList<SVPath>();

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
        signaturePath = new SVPath();
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

    public void serialize(Bundle bundle) {
        if(bundle == null) return;
        ArrayList<SVPath> temp = new ArrayList<PaymentFlowSignatureView.SVPath>();
        temp.addAll(paths);
        temp.remove(signaturePath);

        bundle.putSerializable("PATHS", new SVPathContainer(temp));
    }

    public void showSignature(Bundle bundle) {
        if(bundle == null) return;
        SVPathContainer con = (SVPathContainer) bundle.getSerializable("PATHS");
        if(con == null) return;
        paths.clear();
        paths.addAll(con.getPaths());
        for(SVPath p: paths) {
            p.redraw();
        }
        signaturePath = new SVPath();
        paths.add(signaturePath);
    }

    public static class SVPathContainer implements Serializable {
        private static final long serialVersionUID = 1L;
        protected ArrayList<SVPath> paths = new ArrayList<SVPath>();
        public ArrayList<SVPath> getPaths() {
            return paths;
        }

        public SVPathContainer(ArrayList<SVPath> paths) {
            this.paths.clear();
            this.paths.addAll(paths);
        }
    }

    private static class SVPathAction implements Serializable {
        private static final long serialVersionUID = 1L;
        private int actionType;
        private static final int ACTION_MOVE = 1;
        private static final int ACTION_LINE = 2;
        private static final int ACTION_QUAD = 3;

        private float p1x = 0;
        private float p2x = 0;
        private float p1y = 0;
        private float p2y = 0;

        public float getP1X() { return p1x; }
        public float getP1Y() { return p1y; }
        public float getP2X() { return p2x; }
        public float getP2Y() { return p2y; }

        public int getActionType() {
            return actionType;
        }

        public SVPathAction( int actionType, float p1x, float p1y) {
            this.actionType = actionType;
            this.p1x = p1x;
            this.p1y = p1y;
        }

        public SVPathAction( int actionType, float p1x, float p1y, float p2x, float p2y) {
            this(actionType, p1x, p1y);
            this.p2x = p2x;
            this.p2y = p2y;
        }
    }

    private static class SVPath extends Path implements Serializable {
        private static final long serialVersionUID = 1L;
        private ArrayList<SVPathAction> actions = new ArrayList<SVPathAction>();

        public boolean redraw() {
            boolean dirty = false;
            for(SVPathAction action: actions) {
                if(action.getActionType() == SVPathAction.ACTION_LINE) {
                    super.lineTo(action.getP1X(), action.getP1Y());
                    dirty = true;
                } else if(action.getActionType() == SVPathAction.ACTION_MOVE) {
                    super.moveTo(action.getP1X(), action.getP1Y());
                } else if(action.getActionType() == SVPathAction.ACTION_QUAD) {
                    super.quadTo(action.getP1X(), action.getP1Y(), action.getP2X(), action.getP2Y());
                    dirty = true;
                }
            }
            return dirty;
        }

        @Override
        public void moveTo(float x, float y) {
            actions.add(new SVPathAction(SVPathAction.ACTION_MOVE, x, y));
            super.moveTo(x, y);
        }

        @Override
        public void lineTo(float x, float y) {
            actions.add(new SVPathAction(SVPathAction.ACTION_LINE, x, y));
            super.lineTo(x, y);
        }

        @Override
        public void quadTo(float x1, float y1, float x2, float y2) {
            super.quadTo(x1, y1, x2, y2);
            actions.add(new SVPathAction(SVPathAction.ACTION_QUAD, x1, y1, x2, y2));
        }
    }
}
