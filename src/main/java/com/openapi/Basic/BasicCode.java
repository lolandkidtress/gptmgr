package com.openapi.Basic;

/**
 * Created by James on 16/5/23.
 */
public enum BasicCode {
    success(10200, "成功"), //

    data_not_found(10300, "无数据"),
    data_exist(10300, "数据已存在"),

    parameters_incorrect(10400, "参数格式不正确"),
    AUTH_FAIL(10401, "无权限访问"),

    error(10500,"失败"),
    code_invalid(10501,"验证码错误"),

    quota_over_limit(10600,"次数已用尽");


    //20000 参数方法返回值错误



    public String note;
    public Integer code;

    private BasicCode(Integer code, String note) {
        this.note = note;
        this.code = code;
    }

    public Integer getCode(){
        return this.code;
    }

    public String getNote(){
        return this.note;
    }
}
