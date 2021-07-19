package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.constant.GroupRole;
import com.rcloud.server.sealtalk.domain.GroupMembers;
import com.rcloud.server.sealtalk.domain.Users;
import com.rcloud.server.sealtalk.exception.ServiceException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Author: Jianlu.Yu
 * @Date: 2020/9/4
 * @Description:
 * @Copyright (c) 2020, rongcloud.cn All Rights Reserved
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
@Rollback(true) //设置 true 不会真正插入数据库，为false，数据会插入数据库
public class UsersMapperTest {

    @Autowired
    private UsersMapper usersMapper;

    @Test
    public void selectOne() throws ServiceException {

        long beginTime = System.currentTimeMillis();

        Users param = new Users();
        param.setRegion("87");
        param.setPhone("1233");
        System.out.println(usersMapper.selectOne(param));
        long endTime = System.currentTimeMillis();

        System.out.println("time cost："+(endTime-beginTime));


    }

}