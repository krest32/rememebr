package com.krest.admin.initialize;

import com.krest.admin.cache.ClusterInfo;
import com.krest.admin.config.CoreConfig;
import com.krest.admin.entity.Node;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Iterator;

@Slf4j
@Component
public class ServerInit implements InitializingBean {

    @Autowired
    CoreConfig config;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.config.getServerList().isEmpty()) {
            throw new Exception("未配置集群节点信息");
        } else {
            Iterator<Node> iterator = this.config.getServerList().iterator();

            while (iterator.hasNext()) {
                Node node = iterator.next();
                if (this.config.getServerId().equals(node.getId())) {
                    ClusterInfo.currentNode = node;
                }
            }
            ClusterInfo.configFollowers.addAll(this.config.getServerList());
        }
    }
}
