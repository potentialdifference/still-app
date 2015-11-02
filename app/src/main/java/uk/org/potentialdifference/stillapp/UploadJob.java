package uk.org.potentialdifference.stillapp;

import java.util.List;

/**
 * Created by russell on 02/11/2015.
 */
public class UploadJob {
    public UploadJob(byte[] data, String name) {
        this.data = data;
        this.name = name;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private byte[] data;
    private String name;

}
