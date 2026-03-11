package com.easymeeting.entity.vo;


public class ResponseVO<T> {
    private String status;
    private Integer code;
    private String info;
    private T data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }


    public static <T> ResponseVO<T> success(T data) {
        ResponseVO<T> response = new ResponseVO<>();
        response.setStatus("success");
        response.setCode(200);
        response.setData(data);
        return response;
    }

    public static <T> ResponseVO<T> success(String message) {
        ResponseVO<T> response = new ResponseVO<>();
        response.setStatus("success");
        response.setCode(200);
        response.setInfo(message);
        return response;
    }

    public static <T> ResponseVO<T> error(String message) {
        ResponseVO<T> response = new ResponseVO<>();
        response.setStatus("error");
        response.setCode(500);
        response.setInfo(message);
        return response;
    }

}
