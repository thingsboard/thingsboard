/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.ContactBased;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
public abstract class TbAbstractGetEntityDetailsNode<C extends TbAbstractGetEntityDetailsNodeConfiguration, I extends UUIDBased> extends TbAbstractNodeWithFetchTo<C> {

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        ctx.checkTenantEntity(msg.getOriginator());
        var msgDataAsObjectNode = FetchTo.DATA.equals(fetchTo) ? getMsgDataAsObjectNode(msg) : null;
        withCallback(getDetails(ctx, msg, msgDataAsObjectNode),
                ctx::tellSuccess,
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    protected abstract String getPrefix();

    protected abstract ListenableFuture<? extends ContactBased<I>> getContactBasedFuture(TbContext ctx, TbMsg msg);

    protected void checkIfDetailsListIsNotEmptyOrThrow(C configuration) throws TbNodeException {
        if (configuration.getDetailsList().isEmpty()) {
            throw new TbNodeException("No entity details selected!");
        }
    }

    private ListenableFuture<TbMsg> getDetails(TbContext ctx, TbMsg msg, ObjectNode messageData) {
        ListenableFuture<? extends ContactBased<I>> contactBasedFuture = getContactBasedFuture(ctx, msg);
        return Futures.transformAsync(contactBasedFuture, contactBased -> {
            if (contactBased == null) {
                return Futures.immediateFuture(msg);
            }
            var msgMetaData = msg.getMetaData().copy();
            setProperties(contactBased, messageData, msgMetaData);
            return Futures.immediateFuture(transformMessage(msg, messageData, msgMetaData));
        }, MoreExecutors.directExecutor());
    }

    private void setProperties(ContactBased<I> contactBased, ObjectNode messageData, TbMsgMetaData msgMetaData) {
        String prefix = getPrefix();
        String property;
        String value;
        for (var entityDetails : config.getDetailsList()) {
            switch (entityDetails) {
                case ID:
                    property = prefix + "id";
                    value = contactBased.getId().getId().toString();
                    setDetail(property, value, messageData, msgMetaData);
                    break;
                case TITLE:
                    property = prefix + "title";
                    value = contactBased.getName();
                    setDetail(property, value, messageData, msgMetaData);
                    break;
                case ADDRESS:
                    property = prefix + "address";
                    value = contactBased.getAddress();
                    setDetail(property, value, messageData, msgMetaData);
                    break;
                case ADDRESS2:
                    property = prefix + "address2";
                    value = contactBased.getAddress2();
                    setDetail(property, value, messageData, msgMetaData);
                    break;
                case CITY:
                    property = prefix + "city";
                    value = contactBased.getCity();
                    setDetail(property, value, messageData, msgMetaData);
                    break;
                case COUNTRY:
                    property = prefix + "country";
                    value = contactBased.getCountry();
                    setDetail(property, value, messageData, msgMetaData);
                    break;
                case STATE:
                    property = prefix + "state";
                    value = contactBased.getState();
                    setDetail(property, value, messageData, msgMetaData);
                    break;
                case EMAIL:
                    property = prefix + "email";
                    value = contactBased.getEmail();
                    setDetail(property, value, messageData, msgMetaData);
                    break;
                case PHONE:
                    property = prefix + "phone";
                    value = contactBased.getPhone();
                    setDetail(property, value, messageData, msgMetaData);
                    break;
                case ZIP:
                    property = prefix + "zip";
                    value = contactBased.getZip();
                    setDetail(property, value, messageData, msgMetaData);
                    break;
                case ADDITIONAL_INFO:
                    if (contactBased.getAdditionalInfo().hasNonNull("description")) {
                        property = prefix + "additionalInfo";
                        value = contactBased.getAdditionalInfo().get("description").asText();
                        setDetail(property, value, messageData, msgMetaData);
                    }
                    break;
            }
        }
    }

    private void setDetail(String property, String value, ObjectNode messageData, TbMsgMetaData msgMetaData) {
        if (value == null) {
            return;
        }
        if (FetchTo.METADATA.equals(fetchTo)) {
            msgMetaData.putValue(property, value);
        }
        if (FetchTo.DATA.equals(fetchTo)) {
            messageData.put(property, value);
        }
    }

}
