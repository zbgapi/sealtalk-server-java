package com.rcloud.server.sealtalk.exchange.domain;


import com.rcloud.server.sealtalk.util.MiscUtils;
import lombok.Data;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Data
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
    @Column(name="country_code")
    private String countryCode;
    @Column(name="sex")
    private Integer sex;

    public String getHiddenName() {
        String nickname = StringUtils.isEmpty(this.nickName) ? this.loginName : this.nickName;
        if (nickname.equalsIgnoreCase(this.loginName)) {
            // 产品要求的
            nickname = MiscUtils.hiddenName(nickname);
        }

        return nickname;
    }
}
