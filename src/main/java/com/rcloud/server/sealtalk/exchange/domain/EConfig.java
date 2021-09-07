package com.rcloud.server.sealtalk.exchange.domain;

import lombok.Data;

import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Table(name = "ec_config")
public class EConfig implements Serializable {

    @Id
    private String id;

    /**
     * 1 限制api下单市场
     * 2 设置vip等级
     * 3 设置交易区(创新区 推荐区)
     * 4 cmd链接
     * 5 限制api下单市场后配置可下单白名单
     * 6 市场配置
     * 7 币种配置
     */
    private Integer type;

    private String code;

    private String codeName;

    private String value;
}
