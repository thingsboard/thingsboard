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

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.QuerySendDetailsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.QuerySendDetailsResponse;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.dysmsapi.transform.v20170525.SendSmsResponseUnmarshaller;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.http.HttpResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;

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

    private String numberFrom;
    
    private String accessKeyId;
    private String accessKeySecret;
    private String signName;
    private String templateCode;
    
    private String product;
    private String domain;

    public AliyunSmsSender(AliyunSmsProviderConfiguration config) {
        if (StringUtils.isEmpty(config.getAccessKeyId()) || StringUtils.isEmpty(config.getAccessKeySecret()) || StringUtils.isEmpty(config.getSignName()) || StringUtils.isEmpty(config.getTemplateCode())) {
            throw new IllegalArgumentException("Invalid aliyun sms provider configuration: accountSid, accountToken and numberFrom should be specified!");
        }
        this.numberFrom = config.getAccessKeyId();
        this.accessKeyId = config.getAccessKeySecret();
        this.accessKeySecret = config.getSignName();
        this.signName = config.getTemplateCode();
        
        //产品名称:云通信短信API产品,开发者无需替换
        this.product = "Dysmsapi";
        //产品域名,开发者无需替换
        this.domain = "dysmsapi.aliyuncs.com";
    }

    @Override
    public int sendSms(String numberTo, String message) throws SmsException {
        numberTo = this.validatePhoneNumber(numberTo);
        message = this.prepareMessage(message);
        try {            
            //可自助调整超时时间
            System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
            System.setProperty("sun.net.client.defaultReadTimeout", "10000");

            //初始化acsClient,暂不支持region化
            IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", this.accessKeyId, this.accessKeySecret);
            DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", this.product, this.domain);
            IAcsClient acsClient = new DefaultAcsClient(profile);

            //组装请求对象-具体描述见控制台-文档部分内容
            SendSmsRequest request = new SendSmsRequest();
            //必填:待发送手机号
            request.setPhoneNumbers(numberTo);
            //必填:短信签名-可在短信控制台中找到
            request.setSignName(this.signName);
            //必填:短信模板-可在短信控制台中找到
            request.setTemplateCode(this.templateCode);
            //可选:模板中的变量替换JSON串,如模板内容为"亲爱的${name},您的验证码为${code}"时,此处的值为
            //request.setTemplateParam("{\"name\":\"Tom\", \"code\":\"123\"}");
            request.setTemplateParam("{\"msg\":\"" + message + "\"}");

            //选填-上行短信扩展码(无特殊需求用户请忽略此字段)
            //request.setSmsUpExtendCode("90997");

            //可选:outId为提供给业务方扩展字段,最终在短信回执消息中将此值带回给调用者
            //request.setOutId("yourOutId");

            //hint 此处可能会抛出异常，注意catch
            SendSmsResponse sendSmsResponse = acsClient.getAcsResponse(request);
        	
            return Integer.valueOf(sendSmsResponse.getCode());
        } catch (Exception e) {
            throw new SmsSendException("Failed to send SMS message - " + e.getMessage(), e);
        }
    }

    @Override
    public void destroy() {

    }
}
