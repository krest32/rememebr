package com.krest.admin.utils;

import com.alibaba.fastjson.JSONObject;
import com.krest.admin.cache.ClusterInfo;
import com.krest.admin.entity.AddressEnum;
import com.krest.admin.entity.ClusterParam;
import com.krest.admin.entity.Node;
import com.krest.admin.entity.OkRequest;
import com.krest.admin.entity.ReturnMsg;

import java.io.IOException;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * @author krest
 */
@Slf4j
public class RaftSelector {

    public static synchronized Node selectLeader(Integer term) {
        log.info("开始选举Leader,term:" + term);
        /**
         * 如果存在 Leader, 就判断当前的Leader 是否存活中, 如果存活中，那么就直接返回
         */
        if (ClusterInfo.leader != null) {
            Node leader = ClusterInfo.leader;
            boolean isAlive = isServiceAlive(leader, AddressEnum.ISLEADERALIVEADDR.getAddr());
            if (isAlive) {
                log.info("存在 Leader: " + leader + " , 退出选举流程");
                if (leader.getId().equals(ClusterInfo.currentNode.getId())) {
                    ClusterInfo.leader = leader;
                    ClusterInfo.term = term;
                    ClusterInfo.status = 1;
                }
                return leader;
            }
        }

        /**
         *  开始寻找存活状态的集群节点
         */
        ClusterInfo.aliveFollowers.clear();
        if (ClusterInfo.configFollowers != null && !ClusterInfo.configFollowers.isEmpty()) {
            List<Node> followers = ClusterInfo.configFollowers;
            for (Node node : followers) {
                boolean isAlive = isServiceAlive(node, AddressEnum.ISCANDIDATEALIVEADDR.getAddr());
                if (isAlive) {
                    log.info("存活节点：" + node);
                    ClusterInfo.aliveFollowers.add(node);
                }
            }
        }


        /**
         * 向存活的集群节点 发送投票信息
         */
        Map<String, Integer> voteParam = new HashMap();
        voteParam.put("term", term);
        voteParam.put("serviceId", ClusterInfo.currentNode.getId());

        // 给自己投一票
        Integer ticket = 1;

        /**
         * 轮训，向存活着的节点，发起请求投票信息
         */
        for (Node node : ClusterInfo.aliveFollowers) {
            if (!node.getId().equals(ClusterInfo.currentNode.getId())) {
                String voteTicket = getVoteTicket(node, AddressEnum.VOTEADDR.getAddr(), voteParam);
                log.info("发送请求投票信息 : " + voteParam);
                log.info(node + " 投票结果 : " + voteTicket);
                // 如果投票通过，那么就票数加 1
                if (voteTicket.equals(ReturnMsg.OKMSG.getMsg())) {
                    ticket = ticket + 1;
                }
            }
        }

        // 统计获得的选票，如果大于等于一半，那么就成为新的Leader，否则重新进入到选举
        log.info("当前获得选票数：" + ticket + " 当前存活节点个数：" + ClusterInfo.aliveFollowers.size());
        ClusterInfo.tickets = ticket;
        if (ticket > ClusterInfo.aliveFollowers.size() / 2) {
            log.info("成功被推荐为Leader");
            ClusterInfo.leader = ClusterInfo.currentNode;
            ClusterInfo.status = 1;
        } else {
            log.warn("进入重新选举Leader流程...");
            selectLeader(term);
        }

        // 发布 Leader 信息
        publishEvent();
        return ClusterInfo.leader;
    }

    public static void publishEvent() {
        if (ClusterInfo.status == 1) {
            log.info("Leader 开始广播事件");
            for (Node node : ClusterInfo.aliveFollowers) {
                if (!node.getId().equals(ClusterInfo.currentNode.getId())) {
                    log.info("向 " + node.toString() + "推送信息");
                    String addr = "http://" + node.getIp() + ":" + node.getPort() + AddressEnum.SYNCCLUSTERINFO.getAddr();
                    OkRequest okRequest = new OkRequest(addr, new ClusterParam());
                    HttpUtil.sendPostRequest(okRequest);
                }
            }
        }
    }

    static String getVoteTicket(Node node, String address, Map<String, Integer> voteParam) {
        String leaderAddress = "http://" + node.getIp() + ":" + node.getPort() + address;
        OkRequest okRequest = new OkRequest(leaderAddress, voteParam);
        return HttpUtil.sendPostRequest(okRequest);
    }

    public static boolean isServiceAlive(Node leader, String checkAddress) {
        try {
            String leaderAddress = "http://" + leader.getIp() + ":" + leader.getPort() + checkAddress;
            OkRequest okRequest = new OkRequest(leaderAddress, null);
            String respStr = HttpUtil.sendPostRequest(okRequest);
            return StringUtils.isEmpty(respStr) ? false : respStr.equals(ReturnMsg.OKMSG.getMsg());
        } catch (Exception var5) {
            return false;
        }
    }

    public static ClusterParam getLeaderInfo(Node node) {
        if (node != null) {
            String address = "http://" + node.getIp() + ":" + node.getPort() + AddressEnum.GETCLUSTERINFOADDR.getAddr();
            OkRequest okRequest = new OkRequest(address, node);
            String respStr = HttpUtil.sendPostRequest(okRequest);
            return null == respStr ? null : JSONObject.parseObject(respStr, ClusterParam.class);
        } else {
            return null;
        }
    }
}

