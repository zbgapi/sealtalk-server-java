package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.domain.Groups;
import com.rcloud.server.sealtalk.domain.Users;
import com.rcloud.server.sealtalk.exception.ServiceException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
@Rollback(true) //设置 true 不会真正插入数据库，为false，数据会插入数据库
public class GroupsMapperTest {

    @Autowired
    private GroupsMapper groupsMapper;

    @Test
    public void selectGroupsForAdmin() throws ServiceException {

        long beginTime = System.currentTimeMillis();


        System.out.println(groupsMapper.selectGroupsForAdmin(null, null, null, null));
        long endTime = System.currentTimeMillis();

        System.out.println("time cost："+(endTime-beginTime));


    }

}