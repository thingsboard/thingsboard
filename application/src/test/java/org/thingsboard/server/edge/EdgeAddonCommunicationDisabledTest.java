// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edge;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.edge.exception.EdgeConnectionException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.subscription.SubscriptionService;

import static org.mockito.ArgumentMatchers.any;

@DaoSqlTest
public class EdgeAddonCommunicationDisabledTest extends AbstractEdgeTest {

    @MockitoSpyBean
    private SubscriptionService subscriptionService;

    @Test
    public void testAddonEdgeConnectionRejectedWhenCommunicationDisabled() {
        Mockito.when(subscriptionService.edgeEnabled(any(TenantId.class))).thenReturn(false);
        Mockito.when(subscriptionService.getLicenseVersion()).thenReturn(2);

        edgeImitator.expectClose();
        edgeImitator.connect();

        Assert.assertTrue("Edge imitator should be closed due to disabled edge add-on", edgeImitator.waitForClose());
        Assert.assertNotNull("Expected connection error", edgeImitator.getCloseException());
        Assert.assertTrue("Expected EdgeConnectionException but got: " + edgeImitator.getCloseException().getClass(),
                edgeImitator.getCloseException() instanceof EdgeConnectionException);
        Assert.assertTrue("Expected BAD_CREDENTIALS in error message, but was: " + edgeImitator.getCloseException().getMessage(),
                edgeImitator.getCloseException().getMessage() != null && edgeImitator.getCloseException().getMessage().contains("BAD_CREDENTIALS"));
    }
}


