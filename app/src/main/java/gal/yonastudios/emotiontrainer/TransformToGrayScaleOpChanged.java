package gal.yonastudios.emotiontrainer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Bitmap.Config;

import org.tensorflow.lite.support.common.SupportPreconditions;
import org.tensorflow.lite.support.image.ColorSpaceType;
import org.tensorflow.lite.support.image.ImageOperator;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

public class TransformToGrayScaleOpChanged implements ImageOperator {
//    private static final float[] BITMAP_RGBA_GRAYSCALE_TRANSFORMATION = new float[]{0.299F, 0.587F, 0.114F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F};

    public TransformToGrayScaleOpChanged() {
    }

    public TensorImage apply(TensorImage image) {
        if (image.getColorSpaceType() == ColorSpaceType.GRAYSCALE) {
            return image;
        } else {
            SupportPreconditions.checkArgument(image.getColorSpaceType() == ColorSpaceType.RGB, "Only RGB images are supported in TransformToGrayscaleOp, but not " + image.getColorSpaceType().name());
            int h = image.getHeight();
            int w = image.getWidth();
            Bitmap bmpGrayscale = Bitmap.createBitmap(w, h, Config.ARGB_8888);
            Canvas canvas = new Canvas(bmpGrayscale);
            Paint paint = new Paint();

            ColorMatrixColorFilter cm = new ColorMatrixColorFilter(new float[]{0.299F,0.587F,0.114F,0,0,
                    0.299F,0.587F,0.114F,0,0,
                    0.299F,0.587F,0.114F,0,0,
                    0,0,0,1,0
            });

            paint.setColorFilter(cm);
            canvas.drawBitmap(image.getBitmap(), 0.0F, 0.0F, paint);
            int[] intValues = new int[w * h];
            bmpGrayscale.getPixels(intValues, 0, w, 0, 0, w, h);
            int[] shape = new int[]{1, h, w, 1};

            for(int i = 0; i < intValues.length; ++i) {
                intValues[i] = intValues[i] >> 16 & 255;
            }

            TensorBuffer buffer = TensorBuffer.createFixedSize(shape, image.getDataType());
            buffer.loadArray(intValues, shape);
            image.load(buffer, ColorSpaceType.GRAYSCALE);
            return image;
        }
    }

    public int getOutputImageHeight(int inputImageHeight, int inputImageWidth) {
        return inputImageHeight;
    }

    public int getOutputImageWidth(int inputImageHeight, int inputImageWidth) {
        return inputImageWidth;
    }

    public PointF inverseTransform(PointF point, int inputImageHeight, int inputImageWidth) {
        return point;
    }
}
