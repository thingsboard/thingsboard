package org.thingsboard.server.service.firmware;

public enum FirmwareUpdateStatus {
    QUEUED, INITIATED, DOWNLOADING, DOWNLOADED, VERIFIED, UPDATING, UPDATED, FAILED
}
