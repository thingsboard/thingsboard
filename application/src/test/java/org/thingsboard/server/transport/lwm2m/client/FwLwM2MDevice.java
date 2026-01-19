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
package org.thingsboard.server.transport.lwm2m.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.client.LeshanClient;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.argument.Arguments;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.thingsboard.common.util.ThingsBoardExecutors;

import javax.security.auth.Destroyable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicInteger;
import static org.thingsboard.server.dao.service.OtaPackageServiceTest.TARGET_FW_VERSION;
import static org.thingsboard.server.dao.service.OtaPackageServiceTest.TITLE;

@Slf4j
public class FwLwM2MDevice extends BaseInstanceEnabler implements Destroyable {

    private static final List<Integer> supportedResources = Arrays.asList(0, 1, 2, 3, 5, 6, 7, 9);

    private final ScheduledExecutorService scheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor(getClass().getSimpleName() + "-test-scope");

    private final AtomicInteger state = new AtomicInteger(0);

    private final AtomicInteger updateResult = new AtomicInteger(0);

    private LeshanClient leshanClient;
    private String pkgNameDef = "firmware";
    private String pkgName;
    private String pkgVersionDef = "1.0.0";
    private String pkgVersion;

    @Override
    public ReadResponse read(LwM2mServer identity, int resourceId) {
        if (!identity.isSystem())
            log.info("Read on Device resource /{}/{}/{}", getModel().id, getId(), resourceId);
        switch (resourceId) {
            case 3:
                return ReadResponse.success(resourceId, getState());
            case 5:
                return ReadResponse.success(resourceId, getUpdateResult());
            case 6:
                return ReadResponse.success(resourceId, getPkgName());
            case 7:
                return ReadResponse.success(resourceId, getPkgVersion());
            case 9:
                return ReadResponse.success(resourceId, getFirmwareUpdateDeliveryMethod());
            default:
                return super.read(identity, resourceId);
        }
    }

    @Override
    public ExecuteResponse execute(LwM2mServer identity, int resourceId, Arguments arguments) {
        String withArguments = "";
        if (!arguments.isEmpty())
            withArguments = " with arguments " + arguments;
        log.info("Execute on Device resource /{}/{}/{} {}", getModel().id, getId(), resourceId, withArguments);


        switch (resourceId) {
            case 2:
                startUpdating(identity);
                return ExecuteResponse.success();
            default:
                return super.execute(identity, resourceId, arguments);
        }
    }

    @Override
    public WriteResponse write(LwM2mServer identity, boolean replace, int resourceId, LwM2mResource value) {
        log.info("Write on Device resource /{}/{}/{}", getModel().id, getId(), resourceId);

        switch (resourceId) {
            case 0:
                startDownloading();
                return WriteResponse.success();
            case 1:
                startDownloading();
                return WriteResponse.success();
            default:
                return super.write(identity, replace, resourceId, value);
        }
    }

    private int getState() {
        return state.get();
    }

    private int getUpdateResult() {
        return updateResult.get();
    }

    private String getPkgName() {
        this.pkgName = this.pkgName == null ? this.pkgNameDef : this.pkgName;
        return this.pkgName;
    }

    private String getPkgVersion() {
        this.pkgVersion = this.pkgVersion == null ? this.pkgVersionDef : this.pkgVersion;
        return this.pkgVersion;
    }

    private int getFirmwareUpdateDeliveryMethod() {
        return 1;
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

    @Override
    public void destroy() {
        scheduler.shutdownNow();
    }

    private void startDownloading() {
        scheduler.schedule(() -> {
            try {
                state.set(1);
                fireResourceChange(3);
                Thread.sleep(100);
                state.set(2);
                fireResourceChange(3);
            } catch (Exception e) {
            }
        }, 100, TimeUnit.MILLISECONDS);
    }

    private void startUpdating(LwM2mServer identity) {
        scheduler.schedule(() -> {
            try {
                state.set(3);
                fireResourceChange(3);
                Thread.sleep(100);
                updateResult.set(1);
                fireResourceChange(5);
                this.pkgName = TITLE;
                fireResourceChange(6);
                this.pkgVersion = TARGET_FW_VERSION;
                fireResourceChange(7);
                if (this.leshanClient != null) {
                    log.info("Stop/reboot LwM2M client {}", this.leshanClient.getEndpoint(identity));
                    this.leshanClient.stop(false);
                    log.info("Start after update fw LwM2M client {}", this.leshanClient.getEndpoint(identity));
                    this.leshanClient.start();
                    this.pkgName = this.pkgNameDef;
                    this.pkgVersion = this.pkgVersionDef;
                }
            } catch (Exception e) {
            }
        }, 100, TimeUnit.MILLISECONDS);
    }

    protected void setLeshanClient(LeshanClient leshanClient) {
        this.leshanClient = leshanClient;
    }

}
