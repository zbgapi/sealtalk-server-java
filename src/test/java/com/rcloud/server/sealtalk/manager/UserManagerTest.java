package com.rcloud.server.sealtalk.manager;

import com.rcloud.server.sealtalk.exception.ServiceException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
@Rollback(false) //设置 true 不会真正插入数据库，为false，数据会插入数据库
public class UserManagerTest {

    @Resource
    private UserManager userManager;
    @Test
    public void login() throws ServiceException {
        System.out.println(userManager.login("86", "7xFCdHlLFiq", "pwd123456"));
    }
}