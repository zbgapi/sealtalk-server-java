package com.rcloud.server.sealtalk.exchange.domain;


import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Table(name = "ec_user")
public class EcUser implements Serializable {
    @Id
    @Column(name="user_id")
    private String userId;
    @Column(name="nick_name")
    private String nickName;
    @Column(name="login_name")
    private String loginName;
    @Column(name="head_img")
    private String headImg;


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

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getHeadImg() {
        return headImg;
    }

    public void setHeadImg(String headImg) {
        this.headImg = headImg;
    }
}
