package uk.org.potentialdifference.stillapp;

/**
 * Created by henry on 15/10/15.
 */
public class ImageData {

    public byte[] data;
    public String email;
    public String face;

    ImageData (byte[] data, String email, String face) {
        this.data = data;
        this.email = email;
        this.face = face;
    }

//    byte[] getData() {
//        return this.data;
//    }
//
//    String getEmail() {
//        return this.email;
//    }
//
//    String face() {
//        return this.face;
//    }
}
