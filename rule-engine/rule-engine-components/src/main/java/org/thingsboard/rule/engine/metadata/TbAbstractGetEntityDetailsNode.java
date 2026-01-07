/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.rule.engine.util.ContactBasedEntityDetails;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.ContactBased;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
public abstract class TbAbstractGetEntityDetailsNode<C extends TbAbstractGetEntityDetailsNodeConfiguration, I extends UUIDBased> extends TbAbstractNodeWithFetchTo<C> {

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        var msgDataAsObjectNode = TbMsgSource.DATA.equals(fetchTo) ? getMsgDataAsObjectNode(msg) : null;
        withCallback(getDetails(ctx, msg, msgDataAsObjectNode),
                ctx::tellSuccess,
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    protected abstract String getPrefix();

    protected abstract ListenableFuture<? extends ContactBased<I>> getContactBasedFuture(TbContext ctx, TbMsg msg);

    protected void checkIfDetailsListIsNotEmptyOrElseThrow(List<ContactBasedEntityDetails> detailsList) throws TbNodeException {
        if (detailsList == null || detailsList.isEmpty()) {
            throw new TbNodeException("At least one entity detail should be selected!");
        }
    }

    private ListenableFuture<TbMsg> getDetails(TbContext ctx, TbMsg msg, ObjectNode messageData) {
        ListenableFuture<? extends ContactBased<I>> contactBasedFuture = getContactBasedFuture(ctx, msg);
        return Futures.transformAsync(contactBasedFuture, contactBased -> {
            if (contactBased == null) {
                return Futures.immediateFuture(msg);
            }
            var msgMetaData = msg.getMetaData().copy();
            fetchEntityDetailsToMsg(contactBased, messageData, msgMetaData);
            return Futures.immediateFuture(transformMessage(msg, messageData, msgMetaData));
        }, MoreExecutors.directExecutor());
    }

    private void fetchEntityDetailsToMsg(ContactBased<I> contactBased, ObjectNode messageData, TbMsgMetaData msgMetaData) {
        String value = null;
        for (var entityDetail : config.getDetailsList()) {
            switch (entityDetail) {
                case ID:
                    value = contactBased.getId().getId().toString();
                    break;
                case TITLE:
                    value = contactBased.getName();
                    break;
                case ADDRESS:
                    value = contactBased.getAddress();
                    break;
                case ADDRESS2:
                    value = contactBased.getAddress2();
                    break;
                case CITY:
                    value = contactBased.getCity();
                    break;
                case COUNTRY:
                    value = contactBased.getCountry();
                    break;
                case STATE:
                    value = contactBased.getState();
                    break;
                case EMAIL:
                    value = contactBased.getEmail();
                    break;
                case PHONE:
                    value = contactBased.getPhone();
                    break;
                case ZIP:
                    value = contactBased.getZip();
                    break;
                case ADDITIONAL_INFO:
                    if (contactBased.getAdditionalInfo().hasNonNull("description")) {
                        value = contactBased.getAdditionalInfo().get("description").asText();
                    }
                    break;
            }
            if (value == null) {
                continue;
            }
            setDetail(entityDetail.getRuleEngineName(), value, messageData, msgMetaData);
        }
    }

    private void setDetail(String property, String value, ObjectNode messageData, TbMsgMetaData msgMetaData) {
        String fieldName = getPrefix() + property;
        if (TbMsgSource.METADATA.equals(fetchTo)) {
            msgMetaData.putValue(fieldName, value);
        } else if (TbMsgSource.DATA.equals(fetchTo)) {
            messageData.put(fieldName, value);
        }
    }

}
