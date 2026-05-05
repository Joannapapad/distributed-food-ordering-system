package org.foodApp;

import org.foodApp.Client.Filter;

import java.io.Serializable;

public class Request implements Serializable {
    private static final long serialVersionUID = 96874965622323666L;

    private static int idCounter = 0; // Shared counter for all instances
    private int request_id = 0;
    private Object data;
    private Object data2;
    private RequestType requestType;
    private Filter filter;

    public Request(Object data, RequestType requestType) {
        this.request_id = generateId();
        this.data = data;
        this.data2 = data;
        this.requestType = requestType;
    }

    private int generateId() {
        return idCounter++;
    }

    public int getRequest_id() {
        return request_id;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public Object getData2() {
        return data2;
    }

    public void setData2(Object data2) {
        this.data2 = data2;
    }
}
