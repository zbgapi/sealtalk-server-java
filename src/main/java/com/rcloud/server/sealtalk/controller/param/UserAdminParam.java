package com.rcloud.server.sealtalk.controller.param;

import lombok.Data;

@Data
public class UserAdminParam {
    private String id;
    /**
     * zbg account id
     */
    private String userId;

    private String nickname;

    private String portraitUri;
}
