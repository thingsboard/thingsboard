package org.thingsboard.server.service.security;

import com.google.common.util.concurrent.FutureCallback;
import org.thingsboard.server.actors.plugin.ValidationResult;
import org.thingsboard.server.actors.plugin.ValidationResultCode;
import org.thingsboard.server.extensions.api.exception.AccessDeniedException;
import org.thingsboard.server.extensions.api.exception.EntityNotFoundException;
import org.thingsboard.server.extensions.api.exception.InternalErrorException;
import org.thingsboard.server.extensions.api.exception.UnauthorizedException;

/**
 * Created by ashvayka on 31.03.18.
 */
public class ValidationCallback<T> implements FutureCallback<ValidationResult> {

    private final T response;
    private final FutureCallback<T> action;

    public ValidationCallback(T response, FutureCallback<T> action) {
        this.response = response;
        this.action = action;
    }

    @Override
    public void onSuccess(ValidationResult result) {
        ValidationResultCode resultCode = result.getResultCode();
        if (resultCode == ValidationResultCode.OK) {
            action.onSuccess(response);
        } else {
            Exception e;
            switch (resultCode) {
                case ENTITY_NOT_FOUND:
                    e = new EntityNotFoundException(result.getMessage());
                    break;
                case UNAUTHORIZED:
                    e = new UnauthorizedException(result.getMessage());
                    break;
                case ACCESS_DENIED:
                    e = new AccessDeniedException(result.getMessage());
                    break;
                case INTERNAL_ERROR:
                    e = new InternalErrorException(result.getMessage());
                    break;
                default:
                    e = new UnauthorizedException("Permission denied.");
                    break;
            }
            onFailure(e);
        }
    }

    @Override
    public void onFailure(Throwable e) {
        action.onFailure(e);
    }
}
