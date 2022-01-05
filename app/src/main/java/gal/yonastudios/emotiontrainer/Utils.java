package gal.yonastudios.emotiontrainer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Map;

public class Utils {

    public static String writeResults(Map<String, Float> mapResults){
        Map.Entry<String, Float> entryMax = null;
//        Map.Entry<String, Float> entryMax1 = null;
//        Map.Entry<String, Float> entryMax2 = null;
//        mapResults.put("Disgust", mapResults.get("Disgust")+0.00008f);
//        mapResults.put("Fear", mapResults.get("Fear")+0.000005f);
        for(Map.Entry<String, Float> entry: mapResults.entrySet()){
            if (entryMax == null || entry.getValue().compareTo(entryMax.getValue()) > 0){
                entryMax = entry;
            }
//            else if (entryMax1 == null || entry.getValue().compareTo(entryMax1.getValue()) > 0){
//                entryMax1 = entry;
//            } else if (entryMax2 == null || entry.getValue().compareTo(entryMax2.getValue()) > 0){
//                entryMax2 = entry;
//            }
        }
//        String result = entryMax.getKey().trim() + " " + entryMax.getValue().toString();
        String result = entryMax.getKey().trim();

//                + "\n" +
//                entryMax1.getKey().trim() + " " + entryMax1.getValue().toString() + "\n" +
//                entryMax2.getKey().trim() + " " + entryMax2.getValue().toString() + "\n";
        return result;
    }

    public static int getImageRotation(ImageProxy image){
        int rotation = image.getImageInfo().getRotationDegrees();
        return rotation/90;
    }

    public static void correctPredictEmotions(Map<String, Float> mapResults){
        mapResults.put("Disgust", mapResults.get("Disgust")+0.0012f);
        mapResults.put("Fear", mapResults.get("Fear")+0.00005f);
    }

    public static Bitmap toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
//        yuvImage.compressToJpeg(image.getCropRect(), 100, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    public static Bitmap getGrayScaleBitmap(Bitmap image) {
        // calculate how many bytes our image consists of
        int bytes = image.getByteCount();
        Log.d("bytes length:",""+bytes);
        ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
        image.copyPixelsToBuffer(buffer); // Move the byte data to the buffer

        byte[] temp = buffer.array(); // Get the underlying array containing the data.

        byte[] pixels = new byte[(temp.length / 4)]; // Allocate for 3 byte BGR

        // Copy pixels into place
        for (int i = 0; i < (temp.length / 4); i++) {
            pixels[i] = (byte) (0.299*temp[i * 4 + 1]+0.587*temp[i * 4 + 2]+0.114*temp[i * 4 + 3]);     //0.299*R+0.587*G+0.114*B

            // Alpha is discarded
        }
//
        Bitmap bmp = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
        buffer = ByteBuffer.wrap(pixels);
        bmp.copyPixelsFromBuffer(buffer);

        return bmp;
    }

    public static Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    public static Bitmap changeBitmapContrastBrightness(Bitmap bmp,
                                                        float contrast, float brightness) {
        ColorMatrix cm = new ColorMatrix(new float[] { contrast, 0, 0, 0,
                brightness, 0, contrast, 0, 0, brightness, 0, 0, contrast,
                0, brightness, 0, 0, 0, 1, 0 });

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(),
                bmp.getConfig());/*from  www . j a va  2 s .  c om*/

        Canvas canvas = new Canvas(ret);

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, 0, 0, paint);

        return ret;
    }
}