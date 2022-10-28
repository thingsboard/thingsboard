/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.sms.aliyun;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.sms.config.AliyunSmsProviderConfiguration;
import org.thingsboard.rule.engine.api.sms.exception.SmsException;
import org.thingsboard.rule.engine.api.sms.exception.SmsSendException;
import org.thingsboard.server.service.sms.AbstractSmsSender;

public class AliyunSmsSender extends AbstractSmsSender {

    private String accessKeyId;
    private String accessKeySecret;
    private String signName;
    private String templateCode;

    private String numberFrom;
    private String toRegion = "cn";

    public AliyunSmsSender(AliyunSmsProviderConfiguration config) {
        if (StringUtils.isEmpty(config.getAccessKeyId()) || StringUtils.isEmpty(config.getAccessKeySecret()) || StringUtils.isEmpty(config.getSignName()) || StringUtils.isEmpty(config.getTemplateCode())) {
            throw new IllegalArgumentException("Invalid aliyun sms provider configuration: accountSid, accountToken and numberFrom should be specified!");
        }
        this.accessKeyId = config.getAccessKeyId();
        this.accessKeySecret = config.getAccessKeySecret();
        this.signName = config.getSignName();
        this.templateCode = config.getTemplateCode();
        this.numberFrom = config.getNumberFrom();
        this.toRegion = config.getToRegion();
    }

    @Override
    public int sendSms(String numberTo, String message) throws SmsException {
        numberTo = this.validatePhoneNumber(numberTo);
        message = this.prepareMessage(message);
        //可自助调整超时时间
        System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
        System.setProperty("sun.net.client.defaultReadTimeout", "10000");

        //初始化acsClient，<accessKeyId>和<accessSecret>在短信控制台查询
        DefaultProfile profile = DefaultProfile.getProfile("ap-southeast-1", this.accessKeyId, this.accessKeySecret);
        IAcsClient client = new DefaultAcsClient(profile);
        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);

        request.setSysMethod(MethodType.POST);
        request.setSysDomain("dysmsapi.ap-southeast-1.aliyuncs.com");//域名，请勿修改
        request.setSysVersion("2018-05-01");//API版本号，请勿修改
        if(this.toRegion == "cn"){
            request.setSysAction("SendMessageWithTemplate");//API名称
            request.putQueryParameter("To", numberTo);//接收号码，格式为：国际码+号码("8615200000000")，必填
            request.putQueryParameter("From", this.signName);//必填:短信签名-可在短信控制台中找到
            request.putQueryParameter("TemplateCode", this.templateCode);//必填:短信模板-可在短信控制台中找到
            //可选:模板中的变量替换JSON串,如模板内容为"亲爱的${name},您的验证码为${code}"时,此处的值为
            //request.putQueryParameter("TemplateParam", "{\"name\":\"Tom\", \"code\":\"123\"}");
            request.putQueryParameter("TemplateParam", "{\"msg\":\"" + message + "\"}");
            //request.putQueryParameter("SmsUpExtendCode", this.numberFrom);//选填-上行短信扩展码(无特殊需求用户请忽略此字段)
        }else{
            request.setSysAction("SendMessageToGlobe");//API名称
            request.putQueryParameter("To", numberTo);//接收号码，格式为：国际码+号码，必填
            //request.putQueryParameter("From", this.numberFrom);//发送方号码，选填
            request.putQueryParameter("Message", message);//短信内容，必填
        }

        try {
            CommonResponse response = client.getCommonResponse(request);
            //System.out.println(response.getData());
            return Integer.valueOf(response.getHttpStatus());
        } catch (Exception e) {
            throw new SmsSendException("Failed to send SMS message - " + e.getMessage(), e);
        }
    }

    @Override
    public void destroy() {

    }
}
