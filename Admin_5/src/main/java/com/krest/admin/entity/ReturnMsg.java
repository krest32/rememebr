package com.krest.admin.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * 接口返回信息枚举类
 *
 * @author krest
 */
@ToString
@AllArgsConstructor
@Getter
public enum ReturnMsg {
    OKMSG("ok"),
    ERRORMSG("error"),
    CANDIDATE("candidate");
    private String msg;


}
