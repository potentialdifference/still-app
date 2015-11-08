package uk.org.potentialdifference.stillapp;

import android.graphics.Bitmap;

/**
 * Created by russell on 08/11/2015.
 */
public interface StillWebsocketDelegate {

    void showImage(Bitmap image);
    void hideImage();
    void exitShowMode();
}
