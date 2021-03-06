package com.example.saveapp.face.facGetToken;

import java.util.List;

public class FaceGetListResultBean {
    private int error_code;
    private String error_msg;
    private long log_id;
    private long timestamp;
    private int cached;
    private Result result;

    //private List face_list;


    public int getError_code() {
        return error_code;
    }
    public void setError_code(int error_code) {
        this.error_code = error_code;
    }
    public String getError_msg() {
        return error_msg;
    }
    public void setError_msg(String error_msg) {
        this.error_msg = error_msg;
    }
    public long getLog_id() {
        return log_id;
    }
    public void setLog_id(long log_id) {
        this.log_id = log_id;
    }
    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    public int getCached() {
        return cached;
    }
    public void setCached(int cached) {
        this.cached = cached;
    }

    /*public List getFace_list() {
        return face_list;
    }
    public void setFace_list(List face_list) {
        this.face_list = face_list;
    }
*/
    public Result getResult() {
        return result;
    }
    public void setResult(Result result) {
        this.result = result;
    }
    public static class Result{
        private List face_list;

        public List getFace_list() {
            return face_list;
        }
        public void setFace_list(List face_list) {
            this.face_list = face_list;
        }

 public static class Face_list{
    private String face_token;
private String ctime;
    public String getFace_token() {
        return face_token;
    }
    public void setFace_token(String face_token) {
        this.face_token = face_token;
    }
     public String getCtime() {
         return ctime;
     }
     public void setCtime(String ctime) {
         this.ctime = ctime;
     }
}
    }
}
