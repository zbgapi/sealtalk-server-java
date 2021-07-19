package com.rcloud.server.sealtalk.exchange.domain;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Table(name = "otc_merchant")
public class Merchant implements Serializable {
    @Id
    private Long id;

    @Column(name = "user_id")
    private String userId;
    @Column(name = "nick_name")
    private String nickName;
    @Column(name = "head_url")
    private String headUrl;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getHeadUrl() {
        return headUrl;
    }

    public void setHeadUrl(String headUrl) {
        this.headUrl = headUrl;
    }
}
