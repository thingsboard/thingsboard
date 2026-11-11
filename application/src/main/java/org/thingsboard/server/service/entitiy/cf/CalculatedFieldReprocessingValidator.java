// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.cf;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ArgumentsBasedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.dao.job.JobService;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

import java.util.Map;
import java.util.Optional;

import static org.thingsboard.server.common.data.job.JobStatus.PENDING;
import static org.thingsboard.server.common.data.job.JobStatus.QUEUED;
import static org.thingsboard.server.common.data.job.JobStatus.RUNNING;

@Component
@RequiredArgsConstructor
public class CalculatedFieldReprocessingValidator {

    public static final String NO_ARGUMENTS = "no arguments defined.";
    public static final String NO_TELEMETRY_ARGS = "at least one time series based argument ('Latest telemetry' or 'Time series rolling') should be specified.";
    public static final String NO_OUTPUT = "no output defined.";
    public static final String INVALID_OUTPUT_TYPE = "output type 'Attribute' is not supported.";

    private final JobService jobService;
    private final ActorSystemContext systemContext;

    public CfReprocessingValidationResult validate(CalculatedField calculatedField) {
        return checkJobStatus(calculatedField.getTenantId(), calculatedField.getId())
                .or(() -> calculatedField.getConfiguration() instanceof ArgumentsBasedCalculatedFieldConfiguration argBasedCfg ?
                        checkArguments(argBasedCfg.getArguments()) : Optional.empty())
                .or(() -> checkExpression(calculatedField))
                .or(() -> checkOutput(calculatedField.getConfiguration().getOutput()))
                .orElse(CfReprocessingValidationResult.valid());
    }

    private Optional<CfReprocessingValidationResult> checkJobStatus(TenantId tenantId, CalculatedFieldId calculatedFieldId) {
        Job job = jobService.findLatestJobByKey(tenantId, calculatedFieldId.getId().toString());
        if (job != null && job.getStatus().isOneOf(QUEUED, PENDING, RUNNING)) {
            return Optional.of(CfReprocessingValidationResult.invalid("Calculated field reprocessing is already " + job.getStatus().name().toLowerCase(), job.getStatus()));
        }
        return Optional.empty();
    }

    private Optional<CfReprocessingValidationResult> checkArguments(Map<String, Argument> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return Optional.of(CfReprocessingValidationResult.invalid(NO_ARGUMENTS));
        }
        boolean containsTelemetry = arguments.values().stream()
                .anyMatch(arg -> ArgumentType.TS_LATEST.equals(arg.getRefEntityKey().getType()) ||
                                 ArgumentType.TS_ROLLING.equals(arg.getRefEntityKey().getType()));

        if (!containsTelemetry) {
            return Optional.of(CfReprocessingValidationResult.invalid(NO_TELEMETRY_ARGS));
        }
        return Optional.empty();
    }

    private Optional<CfReprocessingValidationResult> checkExpression(CalculatedField calculatedField) {
        CalculatedFieldCtx ctx = new CalculatedFieldCtx(calculatedField, systemContext);
        try {
            ctx.init();
        } catch (Exception e) {
            return Optional.of(CfReprocessingValidationResult.invalid(e.getMessage()));
        } finally {
            ctx.close();
        }
        return Optional.empty();
    }

    private Optional<CfReprocessingValidationResult> checkOutput(Output output) {
        if (output == null) {
            return Optional.of(CfReprocessingValidationResult.invalid(NO_OUTPUT));
        }
        if (OutputType.ATTRIBUTES.equals(output.getType())) {
            return Optional.of(CfReprocessingValidationResult.invalid(INVALID_OUTPUT_TYPE));
        }
        return Optional.empty();
    }

    public record CfReprocessingValidationResult(boolean isValid, String message, JobStatus lastJobStatus) {

        private static final CfReprocessingValidationResult VALID = new CfReprocessingValidationResult(true, null, null);

        public static CfReprocessingValidationResult valid() {
            return VALID;
        }

        public static CfReprocessingValidationResult invalid(String message) {
            return new CfReprocessingValidationResult(false, "Calculated field cannot be reprocessed: " + message, null);
        }

        public static CfReprocessingValidationResult invalid(String message, JobStatus jobStatus) {
            return new CfReprocessingValidationResult(false, message, jobStatus);
        }

    }

}
