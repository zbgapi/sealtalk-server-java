package com.rcloud.server.sealtalk.exchange.service;

import com.rcloud.server.sealtalk.configuration.SealtalkConfig;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.exchange.dao.EcUserMapper;
import com.rcloud.server.sealtalk.exchange.dao.MerchantMapper;
import com.rcloud.server.sealtalk.exchange.domain.EcUser;
import com.rcloud.server.sealtalk.exchange.domain.Merchant;
import javafx.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class EcUserService {

    @Resource
    private EcUserMapper ecUserMapper;

    @Resource
    private MerchantMapper merchantMapper;
    @Resource
    protected SealtalkConfig sealtalkConfig;

    public EcUser getUser(String userId) throws ServiceException {
        EcUser model = new EcUser();
        model.setUserId(userId);
        EcUser ecUser = ecUserMapper.selectOne(model);
        if (ecUser == null) {
            throw new ServiceException(ErrorCode.USER_NOT_EXIST);
        }

        if (!StringUtils.isEmpty(ecUser.getHeadImg()) && !ecUser.getHeadImg().startsWith("http")) {
            ecUser.setHeadImg(getImgHost(1) + ecUser.getHeadImg());
        }
        Merchant model2 = new Merchant();
        model2.setUserId(userId);
        Merchant merchant = merchantMapper.selectOne(model2);
        if (merchant != null) {
            ecUser.setNickName(merchant.getNickName());
            if (!StringUtils.isEmpty(merchant.getHeadUrl())) {
                if (merchant.getHeadUrl().startsWith("http")) {
                    ecUser.setHeadImg(merchant.getHeadUrl());
                } else {
                    ecUser.setHeadImg(getImgHost(2) + merchant.getHeadUrl());
                }
            }
        }

        return ecUser;
    }

    private String getImgHost(int type) {
        String name = type == 1 ? "zbg-admin" : "zbg-website-otc";
        if (!"prod".equals(sealtalkConfig.getConfigEnv())) {
            name = name + "-test";
        }

        return "https://" + name + ".oss-cn-beijing.aliyuncs.com/";
    }

    public List<EcUser> getUsers(List<String> userIdList) throws ServiceException {
        if (userIdList.isEmpty()) {
            throw new ServiceException(ErrorCode.PARAM_ERROR);
        }

        Example model = new Example(EcUser.class);
        model.createCriteria().andIn("userId", userIdList);

        List<EcUser> userList = ecUserMapper.selectByExample(model);

        if (userList.isEmpty()) {
            throw new ServiceException(ErrorCode.PARAM_ERROR);
        }
        for (EcUser ecUser : userList) {
            if (!StringUtils.isEmpty(ecUser.getHeadImg()) && !ecUser.getHeadImg().startsWith("http")) {
                ecUser.setHeadImg(getImgHost(1) + ecUser.getHeadImg());
            }
        }
        Example model2 = new Example(Merchant.class);
        model2.createCriteria().andIn("userId", userIdList);
        List<Merchant> merchantList = merchantMapper.selectByExample(model2);
        for (Merchant merchant : merchantList) {
            for (EcUser ecUser : userList) {
                if (merchant.getUserId().equals(ecUser.getUserId())) {
                    ecUser.setNickName(merchant.getNickName());
                    if (!StringUtils.isEmpty(merchant.getHeadUrl())) {
                        if (merchant.getHeadUrl().startsWith("http")) {
                            ecUser.setHeadImg(merchant.getHeadUrl());
                        } else {
                            ecUser.setHeadImg(getImgHost(2) + merchant.getHeadUrl());
                        }
                    }
                    break;
                }
            }
        }

        return userList;
    }
}
