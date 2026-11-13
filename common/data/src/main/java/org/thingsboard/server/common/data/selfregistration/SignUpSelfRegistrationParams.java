// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.selfregistration;

import java.util.List;

public record SignUpSelfRegistrationParams (String title,
                                            CaptchaParams captcha,
                                            List<SignUpField> fields,
                                            Boolean showPrivacyPolicy,
                                            Boolean showTermsOfUse) {}
