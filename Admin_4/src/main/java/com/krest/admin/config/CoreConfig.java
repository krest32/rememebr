package com.krest.admin.config;


import com.krest.admin.entity.Node;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 配置信息获取
 *
 * @author krest
 */
@Data
@ToString
@Component
@ConfigurationProperties(prefix = "remember")
public class CoreConfig {
    Integer serverId;
    List<Node> serverList;
}
