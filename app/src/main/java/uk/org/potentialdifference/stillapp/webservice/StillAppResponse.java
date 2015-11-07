package uk.org.potentialdifference.stillapp.webservice;

/**
 * Created by russell on 07/11/2015.
 */
public class StillAppResponse {

    private String status;
    private String code;
    private String message;


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }




    public StillAppResponse(){

    }
    public StillAppResponse(String status, String code, String message){
        this.status = status;
        this.code = code;
        this.message = message;

    }

    @Override
    public String toString() {
        return "status: "+status+"/r/ncode: "+code+"/r/nmessage:"+message;
    }
}
