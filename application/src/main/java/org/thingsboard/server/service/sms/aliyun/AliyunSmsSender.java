/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

//import com.aliyun.tea.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.sms.config.AliyunSmsProviderConfiguration;
import org.thingsboard.rule.engine.api.sms.exception.SmsException;
import org.thingsboard.rule.engine.api.sms.exception.SmsSendException;
import org.thingsboard.server.service.sms.AbstractSmsSender;

@Slf4j
public class AliyunSmsSender extends AbstractSmsSender {

    private String accessKeyId;
    private String accessKeySecret;
    private String signName;
    private String templateCode;

//    private String numberFrom;
//    private String toRegion = "cn";

    public AliyunSmsSender(AliyunSmsProviderConfiguration config) {
        if (StringUtils.isEmpty(config.getAccessKeyId()) || StringUtils.isEmpty(config.getAccessKeySecret()) || StringUtils.isEmpty(config.getSignName()) || StringUtils.isEmpty(config.getTemplateCode())) {
            throw new IllegalArgumentException("Invalid aliyun sms provider configuration: accountSid, accountToken and numberFrom should be specified!");
        }
        this.accessKeyId = config.getAccessKeyId();
        this.accessKeySecret = config.getAccessKeySecret();
        this.signName = config.getSignName();
        this.templateCode = config.getTemplateCode();
        //this.numberFrom = config.getNumberFrom();
        //this.toRegion = config.getToRegion();
    }

    public static com.aliyun.dysmsapi20170525.Client createClient(String accessKeyId, String accessKeySecret) throws Exception {
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                // 必填，您的 AccessKey ID
                .setAccessKeyId(accessKeyId)
                // 必填，您的 AccessKey Secret
                .setAccessKeySecret(accessKeySecret);
        // 访问的域名
        config.endpoint = "dysmsapi.aliyuncs.com";
        return new com.aliyun.dysmsapi20170525.Client(config);
    }


    @Override
    public int sendSms(String numberTo, String message) throws SmsException {
        numberTo = this.validatePhoneNumber(numberTo);
        message = this.prepareMessage(message);

        try {
//            log.info("lsyer-accessKeyId: "+this.accessKeyId);
//            log.info("lsyer-accessKeySecret: "+this.accessKeySecret);
//            log.info("lsyer-signName: "+this.signName);
//            log.info("lsyer-templateCode: "+this.templateCode);
//            log.info("lsyer-numberTo: "+numberTo);
//            log.info("lsyer-msg: "+message);
            com.aliyun.dysmsapi20170525.Client client = this.createClient(this.accessKeyId, this.accessKeySecret);
            com.aliyun.dysmsapi20170525.models.SendSmsRequest sendSmsRequest = new com.aliyun.dysmsapi20170525.models.SendSmsRequest()
                    .setSignName(this.signName)
                    .setTemplateCode(this.templateCode)
                    .setPhoneNumbers(numberTo)
                    .setTemplateParam("{\"code\":\"" + message + "\"}");
            com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
            com.aliyun.dysmsapi20170525.models.SendSmsResponse resp = client.sendSmsWithOptions(sendSmsRequest, runtime);
            com.aliyun.dysmsapi20170525.models.SendSmsResponseBody respBody = resp.getBody();

//            log.info("lsyer-rsp: "+com.aliyun.teautil.Common.toJSONString(resp));
            if("OK" == respBody.getCode())
                return 1;
            else
                return 0;
        } catch (Exception e) {
            throw new SmsSendException("Failed to send SMS message - " + e.getMessage(), e);
        }
    }

    @Override
    public void destroy() {

    }
}
