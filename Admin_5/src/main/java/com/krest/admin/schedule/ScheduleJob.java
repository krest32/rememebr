package com.krest.admin.schedule;

import com.krest.admin.cache.ClusterInfo;
import com.krest.admin.entity.*;
import com.krest.admin.utils.HttpUtil;
import com.krest.admin.utils.RaftSelector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 定时任务
 *
 * @author krest
 */
@Slf4j
@Component
public class ScheduleJob {

    /**
     * 定时任务: 先进行启动数据校验
     * 1. 如果是 Leader: 检查 follower 是都存活， 同时同步aliveFollower信息
     * 2. 如果是 follower: 固定时间内没有收到 Leader 的探测报文，就进行反向探测，然后重新注册自己的信息
     * 3. 如果没有角色信息
     *
     * @throws IOException
     */
    @Scheduled(cron = "0/10 * * * * ?")
    public void detectFollower() throws Exception {
        doCheckJob();
        // 作为 leader
        if (ClusterInfo.status == 1) {
            doLeaderJob();
        }

        // 作为 follower
        if (ClusterInfo.status == 2) {
            doFollowerJod();
        }

        // 作为 候选者
        if (ClusterInfo.status == 3) {
            doCandidateJob();
        }

    }

    /**
     * 候选者任务
     *
     * @throws Exception
     */
    private void doCandidateJob() throws Exception {
        ClusterInfo.status = 3;
        ClusterInfo.leader = null;
        // 集群启动后，开始查找 Leader 信息，如果集群内所有的 Leader 都保存一直，那么就像向 Leader 同步数据
        List<ClusterParam> clusterParams = new ArrayList<>();
        for (Node node : ClusterInfo.configFollowers) {
            ClusterParam respClusterParam = RaftSelector.getLeaderInfo(node);
            if (null != respClusterParam && null != respClusterParam.getLeader()) {
                clusterParams.add(respClusterParam);
            }
        }

        if (clusterParams.isEmpty()) {
            // 如果集群内没有 Leader 那么直接开始选举
            ClusterInfo.term += 1;
            RaftSelector.selectLeader(ClusterInfo.term);
        } else {
            int leaderId = 0;
            Map<Integer, Integer> countMap = new HashMap<>();
            for (ClusterParam param : clusterParams) {
                countMap.put(param.getLeader().getId(), countMap.getOrDefault(param.getLeader().getId(), 0) + 1);
            }
            Iterator<Map.Entry<Integer, Integer>> iterator = countMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, Integer> data = iterator.next();
                int kid = data.getKey();
                int num = data.getValue();
                if (num >= clusterParams.size() / 2) {
                    leaderId = kid;
                }
            }

            if (leaderId == 0) {
                if (ClusterInfo.term == 0) {
                    ClusterInfo.term += 1;
                }
                RaftSelector.selectLeader(ClusterInfo.term);
                throw new Exception("集群内，Leader信息不一致");
            } else {
                // 校验通过，开始保存同步 Cluster 信息, 同时更新自己的状态为 follower
                ClusterInfo.leader = clusterParams.get(0).getLeader();
                ClusterInfo.term = clusterParams.get(0).getTerm();
                ClusterInfo.aliveFollowers = clusterParams.get(0).getAliveFollowers();
                ClusterInfo.status = 2;

                // 然后发送注册信息
                registerSelf();
            }
        }
    }

    private void doCheckJob() throws Exception {
        if (ClusterInfo.configFollowers == null || ClusterInfo.configFollowers.isEmpty()) {
            log.error("启动配置的集群信息为空");
            throw new Exception("启动配置的集群信息为空");
        }

        if (ClusterInfo.leader != null && ClusterInfo.currentNode != null) {
            if (ClusterInfo.leader.getId().equals(ClusterInfo.currentNode.getId())) {
                ClusterInfo.status = 1;
            }
        }
    }

    /**
     * follower 执行任务
     */
    private void doFollowerJod() throws Exception {
        // 校验 超时时间
        long curMillions = System.currentTimeMillis();
        if (ClusterInfo.expireTime == null) {
            ClusterInfo.resetExpireTime();
        }

        if (curMillions > ClusterInfo.expireTime) {
            log.info("沒有收到探测报文, follower 反向探测 leader 信息, Leader : " + ClusterInfo.leader);
            boolean isLeaderAlive = RaftSelector.isServiceAlive(ClusterInfo.leader, AddressEnum.ISLEADERALIVEADDR.getAddr());

            if (isLeaderAlive) {
                registerSelf();
            } else {
                // 如果没有从 Leader 获得信息，那么久重置自己状态
                log.info("当前节点为follower, 执行初始化操作，转变为candidate状态");
                doCandidateJob();
            }
        }
    }

    private boolean registerSelf() {
        // 如果存活，就重新注册
        OkRequest request = new OkRequest(
                "http://" + ClusterInfo.leader.getIp() + ":" + ClusterInfo.leader.getPort() + AddressEnum.ISLEADERALIVEADDR.getAddr(),
                new ClusterParam()
        );
        String respJson = HttpUtil.sendPostRequest(request);

        if (!StringUtils.isEmpty(respJson) && respJson.equals(ReturnMsg.OKMSG.getMsg())) {
            log.info("向Leader发送注册信息成功");
            log.info("leader:" + ClusterInfo.leader);
            log.info("curNode:" + ClusterInfo.currentNode);
            return true;
        } else {
            return false;
        }
    }

    private void doLeaderJob() throws Exception {
        Set<Node> aliveFollowers = new CopyOnWriteArraySet<>();
        Iterator<Node> iterator = ClusterInfo.configFollowers.iterator();
        boolean reSelectFlag = false;
        log.info("开始执行Leader节点任务....");

        while (iterator.hasNext()) {
            Node node = iterator.next();
            // 探测从节点 1.是否存活， 2.是否同一个Leader
            OkRequest isFollowerAliveReq = new OkRequest(
                    "http:" + node.getIp() + ":" + node.getPort() + AddressEnum.ISFOLLOWERALIVEADDR.getAddr(),
                    new ClusterParam()
            );
            String respStr = HttpUtil.sendPostRequest(isFollowerAliveReq);

            // 将自己加入到存活列表中, 提奧多自己
            if (node.getId().equals(ClusterInfo.currentNode.getId())) {
                aliveFollowers.add(node);
                continue;
            }

            // 如果返回信息没有问题
            if (!StringUtils.isEmpty(respStr) && respStr.equals(ReturnMsg.OKMSG.getMsg())) {
                aliveFollowers.add(node);
            }

            // 探测节点，但是 Leader 不一致
            if (!StringUtils.isEmpty(respStr) && respStr.equals(ReturnMsg.ERRORMSG.getMsg())) {
                reSelectFlag = true;
                break;
            }
        }

        ClusterInfo.aliveFollowers = aliveFollowers;
        log.info("aliveFollowers:" + ClusterInfo.aliveFollowers);


        if (reSelectFlag) {
            log.info("检测到不同的 Leader 信息, 开始重置系统");
            doCandidateJob();
            log.info("重置系统完成");
        }
    }
}
