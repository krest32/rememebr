package com.krest.admin.cache;

import com.krest.admin.entity.Node;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 集群信息
 *
 * @Author Krest
 */
@Data
public class ClusterInfo {
    /**
     * Leader 信息
     */
    public static Node leader;


    /**
     * 当前节点
     */
    public static Node currentNode;


    /**
     * 任期信息，默认为 0，当前 Leader 奔溃后，term+1，然后开始下届选举
     */
    public static Integer term = 0;
    /**
     * 当前节点状态,节点启动，默认为候选者，
     * 1,2 不参与选举
     * 3 参与选举，如果 1,2 参与选举，需要先转变为候选者
     * 1. leader, 2. follower， 3. candidate
     */
    public static Integer status = 3;

    /**
     * Leader 选举选票
     */
    public static Integer tickets = 0;
    /**
     * 从节点 自动探测时间
     */
    public static Long expireTime;
    /**
     * 配置的集群节点
     */
    public static List<Node> configFollowers = new ArrayList<>();

    /**
     * 配置的集群节点
     */
    public static Set<Node> aliveFollowers = new CopyOnWriteArraySet<>();


    public static void resetExpireTime() {
        expireTime = System.currentTimeMillis() + 15 * 1000;
    }
}
