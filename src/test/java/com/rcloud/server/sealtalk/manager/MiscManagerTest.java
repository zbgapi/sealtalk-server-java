package com.rcloud.server.sealtalk.manager;


import com.alibaba.fastjson.JSONObject;
import com.rcloud.server.sealtalk.exception.ServiceException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
@Rollback(false) //设置 true 不会真正插入数据库，为false，数据会插入数据库
public class MiscManagerTest {

    @Resource
    private MiscManager miscManager;

    @Test
    public void sendSystemMessage() throws ServiceException {
        JSONObject extra = new JSONObject();
        extra.put("userId", "7laqlo4F8N6");
        extra.put("orderId", "1111");
        extra.put("actionType", 1);
        extra.put("currencyName", "usdt");
        extra.put("amount", "1000");
        miscManager.sendSystemMessage("系统消息", "你的OTC订单已经完成", extra.toJSONString(), 2);
    }

    @Test
    public void getSystemMessageList() throws ServiceException {
        JSONObject extra = new JSONObject();
        extra.put("userId", "7laqlo4F8N6");
        extra.put("orderId", "1111");
        extra.put("actionType", 1);
        extra.put("currencyName", "usdt");
        extra.put("amount", "1000");
        miscManager.getSystemMessageList("7laqlo4F8N6", null, null, 1, 2);
    }
}