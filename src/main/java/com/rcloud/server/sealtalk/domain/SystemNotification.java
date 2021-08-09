package com.rcloud.server.sealtalk.domain;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Data
@Table(name = "system_notification")
public class SystemNotification implements Serializable {
    @Id
    @GeneratedValue(generator = "JDBC")
    private Integer id;

    @Column(name = "serialNo")
    private Long serialNo;

    @Column(name = "memberId")
    private Integer memberId;

    @Column(name = "content")
    private String content;

    @Column(name = "createdAt")
    private Date createdAt;

    @Column(name = "updatedAt")
    private Date updatedAt;

    @Transient
    private Users users;
}
