package com.rcloud.server.sealtalk.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.domain.Groups;
import com.rcloud.server.sealtalk.domain.Users;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.model.response.APIResultWrap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @Author: Jianlu.Yu
 * @Date: 2020/8/4
 * @Description:
 * @Copyright (c) 2020, rongcloud.cn All Rights Reserved
 */
@Slf4j
public class MiscUtils {

    /**
     * 地区、标示map
     */
    private static Map<String, String> regionMap = new HashMap<>();

    static {
        regionMap.put("86", "zh-CN");
    }


    /**
     * 地区添加前缀 "+"
     *
     * @param region 86
     * @return +86
     */
    public static String addRegionPrefix(String region) {
        if (!region.startsWith(Constants.STRING_ADD)) {
            region = Constants.STRING_ADD + region;
        }
        return region;
    }

    /**
     * 地区去掉前缀 "+"
     *
     * @param region +86
     * @return 86
     */
    public static String removeRegionPrefix(String region) {
        if (region.startsWith(Constants.STRING_ADD)) {
            region = region.substring(1);
        }
        return region;
    }

    public static String hash(String text, int salt) {
        if (StringUtils.isEmpty(text)) {
            return null;
        }
        else {
            text = text + "|" + salt;
            return DigestUtils.sha1Hex(text);
        }
    }


    public static String merge(String content, String key, String code) {
        content = content.replaceAll(key, code);
        return content;
    }

    public static String getRegionName(String region) {
        return regionMap.get(region);
    }


    /**
     * 文本xss处理
     *
     * @param str
     * @param maxLength
     * @return
     */
    public static String xss(String str, int maxLength) {
        String result = "";
        if (StringUtils.isEmpty(str)) {
            return result;
        }
        result = StringEscapeUtils.escapeHtml4(str);
        if (result.length() > maxLength) {
            result = result.substring(0, maxLength);
        }
        return result;

    }

    /**
     * 根据propertyExpression 对结果对象中的ID进行N3D编码
     * <p>
     * propertyExpression 用点 "." 导航，如下
     * <p>
     * Object{
     * userId：     //propertyExpression=userId
     * groups{
     * id:1   // propertyExpression = groups.id
     * }
     * <p>
     * [           //  如果是数组或list同上
     * groups{
     * id   // propertyExpression = groups.id
     * },
     * groups{
     * id
     * }
     * ]
     * }
     * <p>
     * 如果参数propertyExpression 为空默认为 propertyExpression = "id"
     *
     * @param o
     * @param propertyExpressions
     * @return
     */
    public static Object encodeResults(Object o, String... propertyExpressions) throws ServiceException {
        try {
            if (o == null) {
                return null;
            }
            if (propertyExpressions == null || propertyExpressions.length == 0) {
                //默认对ID进行加密
                propertyExpressions = new String[]{"id"};
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(objectMapper.writeValueAsBytes(o));

            for (String propertyExpression : propertyExpressions) {
                processResult(jsonNode, propertyExpression);
            }
            return jsonNode;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(ErrorCode.SERVER_ERROR);
        }
    }


    private static void processResult(JsonNode jsonNode, String propertyExpression) throws ServiceException {
        if (jsonNode.isArray()) {
            Iterator<JsonNode> it = jsonNode.iterator();
            while (it.hasNext()) {
                JsonNode jsonNode1 = it.next();
                processResult(jsonNode1, propertyExpression);
            }
        } else {
            String[] elements = propertyExpression.split("\\.");
            JsonNode targetNode = null;

            if (elements.length == 1) {
                targetNode = jsonNode;
            } else {
                int index = 0;
                for (int i = 0; i < elements.length - 1; i++) {
                    targetNode = jsonNode.get(elements[i]);
                    index = index + elements[i].length() + 1;
                    if (targetNode != null && targetNode.isArray()) {
                        processResult(targetNode, propertyExpression.substring(index));
                        return;
                    }
                    if (targetNode == null || targetNode.isNull()) {
                        return;
                    }
                }
            }
            ObjectNode objectNode = (ObjectNode) targetNode;
            if (objectNode.get(elements[elements.length - 1]) != null) {
                if (!objectNode.get(elements[elements.length - 1]).isNull()) {
                    objectNode.put(elements[elements.length - 1], N3d.encode(objectNode.get(elements[elements.length - 1]).asInt()));
                }
            }

            return;
        }
    }

    /**
     * 单个元素转换成数组
     *
     * @param str
     * @return
     */
    public static String[] one2Array(String str) {
        return new String[]{str};
    }

    public static String[] encodeIds(Integer[] ids) throws ServiceException {
        if(ArrayUtils.isNotEmpty(ids)){
            String[] result = new String[ids.length];

            for(int i=0;i<ids.length;i++){
                result[i] = N3d.encode(ids[i]);
            }

            return result;
        }
        return null;
    }

    public static Integer[] decodeIds(String[] ids) throws ServiceException {
        if(ArrayUtils.isNotEmpty(ids)){
            Integer[] result = new Integer[ids.length];

            for(int i=0;i<ids.length;i++){
                result[i] = N3d.decode(ids[i]);
            }

            return result;
        }
        return null;
    }

    public static String[] encodeIds(List<Integer> ids) throws ServiceException {
        if(!CollectionUtils.isEmpty(ids)){
            String[] result = new String[ids.size()];

            for(int i=0;i<ids.size();i++){
                result[i] = N3d.encode(ids.get(i));
            }

            return result;
        }
        return null;
    }



    public static Integer[] toInteger(String[] memberIds) {

        if(memberIds!=null){
            Integer[] v = new Integer[memberIds.length];
            for(int i=0;i<memberIds.length;i++){
                v[i] = Integer.valueOf(memberIds[i]);
            }
            return v;
        }
        return null;
    }

    public static String toString(Object obj, String defaultValue) {
        if (obj == null) {
            return defaultValue;
        }

        if (obj instanceof String && StringUtils.isEmpty((String) obj)) {
            return defaultValue;
        }

        return obj.toString();
    }

    /**
     * 短8位随机字符串
     *
     * @return 随机字符串
     */
    public static String shortUuid() {
        /*
         利用62个可打印字符，通过随机生成32位UUID，由于UUID都为十六进制，所以将UUID分成8组，每4个为一组，然后通过模62操作，结果作为索引取出字符
         */
        StringBuilder builder = new StringBuilder();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        for (int i = 0; i < 8; i++) {
            String str = uuid.substring(i * 4, i * 4 + 4);
            int x = Integer.parseInt(str, 16);
            x = x % 0x3E + 48;
            if (x > 57) {
                x += 7;
            }
            if (x > 90) {
                x += 6;
            }
            builder.append((char) x);
        }
        return builder.toString();
    }

    public static String hiddenName(String loginName) {
        try {
            if (!StringUtils.isBlank(loginName)) {
                if (RegexUtils.checkEmail(loginName)) {
                    if (loginName.indexOf("@") > 2) {
                        loginName = loginName.substring(0, 2) + "***" + loginName.substring(loginName.indexOf("@"));
                    }
                } else {
                    loginName = loginName.substring(0, 3) + "***" + loginName.substring(loginName.length() - 2);
                }
            }
        } catch (Exception ignore) {
        }

        return loginName;
    }
}
