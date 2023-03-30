package com.krest.admin.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * OkHttp3 request 请求
 *
 * @author Lenovo
 */
@Data
@AllArgsConstructor
public class OkRequest {
    String targetUrl;
    Object requestData;
}
