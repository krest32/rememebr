package com.krest.admin.entity;

import lombok.Data;
import lombok.ToString;

/**
 * 集群节点实体类
 *
 * @author krest
 */
@Data
@ToString
public class Node {
    /**
     * 集群节点的Id
     */
    Integer id;
    /**
     * IP地址
     */
    String ip;
    /**
     * 端口
     */
    String port;
}
