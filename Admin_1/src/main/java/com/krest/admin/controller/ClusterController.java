package com.krest.admin.controller;

import com.krest.admin.cache.ClusterInfo;
import com.krest.admin.entity.AddressEnum;
import com.krest.admin.entity.ClusterParam;
import com.krest.admin.entity.ReturnMsg;
import com.krest.admin.utils.RaftSelector;
import com.sun.org.apache.regexp.internal.RE;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author krest
 */
@Slf4j
@RequestMapping("cluster")
@RestController
public class ClusterController {

    @PostMapping({"candidate/getTicket"})
    public String getTicket(@RequestBody Map<String, Integer> voteParam) {
        /**
         * 只有满足下面的条件，才能获得选票
         * 1. term = 当前节点的 term
         * 2. serviceId 小于当前节点的 serviceId
         */
        if (voteParam.get("term") != null
                && voteParam.get("term") <= ClusterInfo.term
                && voteParam.get("serviceId") != null
                && voteParam.get("serviceId") < ClusterInfo.currentNode.getId()) {
            return ReturnMsg.OKMSG.getMsg();
        }
        return ReturnMsg.ERRORMSG.getMsg();
    }

    @PostMapping("candidate/transferToCandidate")
    public void transferToCandidate() {
        ClusterInfo.status = 3;
        ClusterInfo.leader = null;
        ClusterInfo.aliveFollowers.clear();
    }

    @PostMapping("candidate/isAlive")
    public String isCandidateAlive(@RequestBody ClusterParam requestCluster) {
        return ReturnMsg.OKMSG.getMsg();
    }

    /**
     * 判断 Follower 是否存活
     * 如果 leader 信息不一致，返回 false
     * 如果一直，那种重置失效时间，然后返回true
     *
     * @param param
     * @return
     */
    @PostMapping("follower/isAlive")
    public String isFollowerAlive(@RequestBody ClusterParam param) {
        if (ClusterInfo.status == 3) {
            return ReturnMsg.CANDIDATE.getMsg();
        }
        if (ClusterInfo.status == 2) {
            if (!param.getLeader().getId().equals(ClusterInfo.leader.getId())) {
                log.info("检测到来自不同Leader的探测报文, param:" + param);
                if (ClusterInfo.tickets <= param.getTickets()) {
                    log.info("变更新的leader: " + param.getLeader());
                    ClusterInfo.leader = param.getLeader();
                    ClusterInfo.term = param.getTerm();
                    ClusterInfo.tickets = param.getTickets();
                    ClusterInfo.aliveFollowers = param.getAliveFollowers();
                    ClusterInfo.status = 2;
                    return ReturnMsg.OKMSG.getMsg();
                } else {
                    log.info("拒绝");
                    return ReturnMsg.ERRORMSG.getMsg();
                }
            }

            ClusterInfo.resetExpireTime();
            return ReturnMsg.OKMSG.getMsg();
        }
        return ReturnMsg.ERRORMSG.getMsg();
    }


    @PostMapping("leader/isAlive")
    public String isLeaderAlive(@RequestBody ClusterParam clusterParam) {
        // 校验请求信息
        if (clusterParam == null || clusterParam.getLeader() == null) {
            return ReturnMsg.ERRORMSG.getMsg();
        }
        // 校验当前节点信息
        if (ClusterInfo.leader == null) {
            return ReturnMsg.ERRORMSG.getMsg();
        }
        if (clusterParam.getLeader().getId().equals(ClusterInfo.leader.getId())) {
            ClusterInfo.aliveFollowers.add(clusterParam.getCurrentNode());
            // 向集群同步自己的信息
            RaftSelector.publishEvent();
            return ReturnMsg.OKMSG.getMsg();
        }

        return ReturnMsg.ERRORMSG.getMsg();
    }

    @PostMapping({"leader/getClusterInfo"})
    public ClusterParam getClusterInfo() {
        return new ClusterParam();
    }

    @PostMapping({"leader/sync/clusterInfo"})
    public String syncClusterInfo(@RequestBody ClusterParam param) {
        // 如果出现2个leader，那么选票更高的成为新的leader
        if (ClusterInfo.status == 1 && ClusterInfo.tickets >= param.getTickets()) {
            log.info("拒绝接收其他Leader同步数据，leader:" + param.getLeader());
            return ReturnMsg.ERRORMSG.getMsg();
        }

        // 如果出现两个Leader, 选票相同, nodeId > 当前node id
        if (ClusterInfo.status == 1
                && ClusterInfo.tickets.equals(param.getTickets())
                && param.getCurrentNode().getId() > ClusterInfo.currentNode.getId()) {
            log.info("拒绝接收其他Leader同步数据，leader:" + param.getLeader());
            return ReturnMsg.ERRORMSG.getMsg();
        }


        if (ClusterInfo.leader != null && ClusterInfo.leader.getId() != param.getLeader().getId()) {
            log.info("开始变更 leader 信息: " + ClusterInfo.leader.getId() + " -> " + param.getLeader().getId());
        }
        ClusterInfo.leader = param.getLeader();
        ClusterInfo.term = param.getTerm();
        ClusterInfo.aliveFollowers = param.getAliveFollowers();
        ClusterInfo.status = 2;
        ClusterInfo.tickets = param.getTickets();
        ClusterInfo.resetExpireTime();

        log.info("开始接收Leader同步数据，leader:" + param.getLeader());
        // 然后重新向 Leader 节点注册
        return ReturnMsg.OKMSG.getMsg();
    }
}
