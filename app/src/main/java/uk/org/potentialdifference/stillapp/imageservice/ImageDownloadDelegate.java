package uk.org.potentialdifference.stillapp.imageservice;

import android.graphics.Bitmap;

/**
 * Created by russell on 07/11/2015.
 */
public interface ImageDownloadDelegate {
    void imageDownloadComplete(Bitmap image);
}
