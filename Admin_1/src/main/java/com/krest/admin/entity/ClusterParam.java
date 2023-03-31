package com.krest.admin.entity;

import com.krest.admin.cache.ClusterInfo;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 请求参数
 *
 * @author dux
 */
@Data
public class ClusterParam {
    
    Node leader;
    Integer term;
    Integer tickets;
    Integer status;
    Node currentNode;
    Set<Node> aliveFollowers;

    public ClusterParam() {
        this.leader = ClusterInfo.leader;
        this.term = ClusterInfo.term;
        this.aliveFollowers = ClusterInfo.aliveFollowers;
        this.status = ClusterInfo.status;
        this.currentNode = ClusterInfo.currentNode;
        this.tickets = ClusterInfo.tickets;
    }



    public Boolean compareLeader(Node node) {
        if (this.leader == null || this.leader.getId() == null) {
            return true;
        }
        if (null != node && null != node.getId() && this.leader.getId().equals(node.getId())) {
            return true;
        }
        return false;
    }
}
