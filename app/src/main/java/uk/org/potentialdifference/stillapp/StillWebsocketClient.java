package uk.org.potentialdifference.stillapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

import uk.org.potentialdifference.stillapp.imageservice.ImageDownloadDelegate;
import uk.org.potentialdifference.stillapp.imageservice.ImageDownloadTask;

/**
 * Created by russell on 08/11/2015.
 */
public class StillWebsocketClient extends WebSocketClient implements ImageDownloadDelegate{

    private static final String TAG = "still-websocket-client";
    private final StillWebsocketDelegate delegate;
    private Context context;
    public StillWebsocketClient(Context context, URI uri, Draft draft, StillWebsocketDelegate delegate){
        super(uri, draft);
        this.context = context;
        this.delegate = delegate;
    }
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.i(TAG, "Websocket onOpen called");
    }

    @Override
    public void onMessage(String s) {
        Log.d(TAG, "websocket message " + s);
        try {
            Log.d(TAG, "message received: " + s);
            JSONObject jsonObj = new JSONObject(s);
            String message = jsonObj.getString("message");
            Log.d(TAG, "got message type: " + message);
            if (message.equals("displayImage")) {
                Log.d(TAG, "setting new display image");
                ImageDownloadTask downloadTask = new ImageDownloadTask(context, this);
                String path = jsonObj.getString("path");
                downloadTask.execute(path);

                Log.d(TAG, "setting new display image to " + uri);
            }
            else if(message.equals("hideImage")){
                delegate.hideImage();
            }
            else if(message.equals("exitShowMode")){
                delegate.exitShowMode();
            }
        } catch (JSONException e) {
            Log.e(TAG, "json exception: " + e.toString());
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        Log.i("Websocket", "Closed " + s);
    }

    @Override
    public void onError(Exception e) {
        Log.i("Websocket", "Error " + e.getMessage());
    }

    @Override
    public void imageDownloadComplete(Bitmap image) {
        delegate.showImage(image);
    }
}
