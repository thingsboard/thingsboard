package org.thingsboard.server.actors.application;

import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.common.data.id.TenantId;

public class ApplicationActor extends ContextAwareActor {
    protected final TenantId tenantId;
    protected final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private ApplicationActor(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext);
        this.tenantId = tenantId;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        logger.debug("[{}] Received message: {}", tenantId, message);
        if(message instanceof RuleDeleteMessage) {
            logger.debug("msg type RuleDeleteMessage -->");
            systemContext.getApplicationService().updateApplicationOnRuleDelete(((RuleDeleteMessage) message).getRuleId(), tenantId);
        } else if(message instanceof DashboardDeleteMessage){
            logger.debug("msg type DashboardDeleteMessage -->");
            systemContext.getApplicationService().updateApplicationOnDashboardDelete(((DashboardDeleteMessage) message).getDashboardId(), tenantId);
        } else {
            logger.debug("[{}][{}] Unknown msg type.", tenantId, message.getClass().getName());
        }
    }

    public static class ActorCreator extends ContextBasedCreator<ApplicationActor> {

        private static final long serialVersionUID = 1L;

        private final TenantId tenantId;

        public ActorCreator(ActorSystemContext context, TenantId tenantId) {
            super(context);
            this.tenantId = tenantId;
        }

        @Override
        public ApplicationActor create() throws Exception {
            return new ApplicationActor(context, tenantId);
        }
    }
}
