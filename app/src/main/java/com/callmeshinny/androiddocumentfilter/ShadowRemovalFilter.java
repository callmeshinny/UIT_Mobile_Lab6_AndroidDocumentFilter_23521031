package com.callmeshinny.androiddocumentfilter;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShadowRemovalFilter {

    public interface FilterCallback {
        void onComplete(Bitmap bitmap);
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public void apply(Bitmap inputBitmap, FilterCallback callback) {
        executorService.execute(() -> {
            Bitmap outputBitmap = null;

            Mat sourceRgba = new Mat();
            Mat sourceRgb = new Mat();
            Mat hsvMat = new Mat();
            Mat kernel = new Mat();
            Mat dilated = new Mat();
            Mat background = new Mat();
            Mat diff = new Mat();
            Mat inverted = new Mat();
            Mat normalized = new Mat();
            Mat resultHsv = new Mat();
            Mat resultRgb = new Mat();
            Mat resultRgba = new Mat();

            List<Mat> hsvChannels = new ArrayList<>();

            try {
                Bitmap workingBitmap = inputBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Utils.bitmapToMat(workingBitmap, sourceRgba);

                Imgproc.cvtColor(sourceRgba, sourceRgb, Imgproc.COLOR_RGBA2RGB);
                Imgproc.cvtColor(sourceRgb, hsvMat, Imgproc.COLOR_RGB2HSV);

                Core.split(hsvMat, hsvChannels);
                Mat valueChannel = hsvChannels.get(2);

                kernel = Mat.ones(new Size(7, 7), CvType.CV_8U);
                Imgproc.dilate(valueChannel, dilated, kernel);
                Imgproc.medianBlur(dilated, background, 21);

                Core.absdiff(valueChannel, background, diff);
                Core.bitwise_not(diff, inverted);
                Core.normalize(inverted, normalized, 0, 255, Core.NORM_MINMAX);

                hsvChannels.set(2, normalized);
                Core.merge(hsvChannels, resultHsv);

                Imgproc.cvtColor(resultHsv, resultRgb, Imgproc.COLOR_HSV2RGB);
                Imgproc.cvtColor(resultRgb, resultRgba, Imgproc.COLOR_RGB2RGBA);

                outputBitmap = Bitmap.createBitmap(
                        resultRgba.cols(),
                        resultRgba.rows(),
                        Bitmap.Config.ARGB_8888
                );
                Utils.matToBitmap(resultRgba, outputBitmap);
            } catch (Exception exception) {
                outputBitmap = null;
            } finally {
                for (Mat channel : hsvChannels) {
                    channel.release();
                }

                sourceRgba.release();
                sourceRgb.release();
                hsvMat.release();
                kernel.release();
                dilated.release();
                background.release();
                diff.release();
                inverted.release();
                normalized.release();
                resultHsv.release();
                resultRgb.release();
                resultRgba.release();
            }

            Bitmap finalBitmap = outputBitmap;
            new Handler(Looper.getMainLooper()).post(() -> callback.onComplete(finalBitmap));
        });
    }
}
