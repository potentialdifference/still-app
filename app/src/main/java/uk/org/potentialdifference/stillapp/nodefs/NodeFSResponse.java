package uk.org.potentialdifference.stillapp.nodefs;

import org.w3c.dom.Node;

/**
 * Created by russell on 26/10/2015.
 */
public class NodeFSResponse {

    private String status;
    private String code;
    private String message;
    private Object data;

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

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }



    public NodeFSResponse(){

    }
    public NodeFSResponse(String status, String code, String message, Object data){
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    @Override
    public String toString() {
        return "status: "+status+"/r/ncode: "+code+"/r/nmessage:"+message+"/r/ndata"+data.toString();
    }
}
