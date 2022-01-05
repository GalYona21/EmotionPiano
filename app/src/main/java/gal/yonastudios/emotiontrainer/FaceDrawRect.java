package gal.yonastudios.emotiontrainer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class FaceDrawRect extends GraphicOverlay.Graphic{
    private int mFaceBoundColors = Color.GREEN;
    private float mStrokeWidth = 4.0f;
    private Paint faceBoundsPaint;
    private GraphicOverlay graphicOverlay;
    private Rect faceBounds;

    public FaceDrawRect(GraphicOverlay overlay, Rect faceBounds) {
        super(overlay);
        faceBoundsPaint= new Paint();
        faceBoundsPaint.setColor(mFaceBoundColors);
        faceBoundsPaint.setStyle(Paint.Style.STROKE);
        faceBoundsPaint.setStrokeWidth(mStrokeWidth);
        this.graphicOverlay=overlay;
        this.faceBounds=faceBounds;

        postInvalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        RectF rectF = new RectF(faceBounds);
        rectF.left = translateX(faceBounds.left);
        rectF.right = translateX(faceBounds.right);
        rectF.top = translateY(faceBounds.top);
        rectF.bottom = translateY(faceBounds.bottom);

        canvas.drawRect(rectF, faceBoundsPaint);

    }
}
