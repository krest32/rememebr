package com.krest.admin.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AddressEnum {

    VOTEADDR("/cluster/candidate/getTicket", "获取选票路径"),
    TRANSFERTOCANDIDTAEADDR("/cluster/candidate/transferToCandidate", "当前节点转变为候选节点"),
    ISCANDIDATEALIVEADDR("/cluster/candidate/isAlive", "当前候选节点是否存活路径"),
    ISFOLLOWERALIVEADDR("/cluster/follower/isAlive", "探测follower是否存活路径"),
    ISLEADERALIVEADDR("/cluster/leader/isAlive", "探测Leader是否存活路径"),
    GETCLUSTERINFOADDR("/cluster/leader/getClusterInfo", "获取集群信息路径"),
    SYNCCLUSTERINFO("/cluster/leader/sync/clusterInfo", "同步Cluster信息到从节点");


    private String addr;
    private String remark;

}
