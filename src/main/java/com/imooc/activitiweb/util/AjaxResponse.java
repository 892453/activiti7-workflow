package com.imooc.activitiweb.util;

/**
 * 定义后端返回给前端的内容
 */


public class AjaxResponse {
    private Integer status; //状态码
    private String msg;     //内容
    private Object obj;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    // 构造函数
    private AjaxResponse(Integer status, String msg, Object obj) {
        this.status = status;
        this.msg = msg;
        this.obj = obj;
    }

    // 返回Ajax返回值的对象
    public static AjaxResponse AjaxData(Integer status, String msg, Object obj) {
        return new AjaxResponse(status, msg, obj);
    }
}
