/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.thingsboard.server.transport.lwm2m.client;

import org.eclipse.leshan.client.resource.SimpleInstanceEnabler;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.attributes.Attribute;
import org.eclipse.leshan.core.link.attributes.QuotedStringAttribute;
import org.eclipse.leshan.core.link.attributes.ResourceTypeAttribute;
import org.eclipse.leshan.core.link.attributes.UnquotedStringAttribute;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLink;
import org.eclipse.leshan.core.link.lwm2m.MixedLwM2mLink;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.argument.Argument;
import org.eclipse.leshan.core.request.argument.Arguments;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.response.SendResponse;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.RandomStringUtils;
import org.eclipse.leshan.core.util.datatype.ULong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * This aims to implements LWM2M Test Object(Id:3442) from LWM2M registry.
 */
public class LwM2mTestObject extends SimpleInstanceEnabler {
    private static final Logger LOG = LoggerFactory.getLogger(LwM2mTestObject.class);

    public static final String INITIAL_STRING_VALUE = "initial value";
    public static final Long INITIAL_INTEGER_VALUE = 1024l;
    public static final ULong INITIAL_UNSIGNED_INTEGER_VALUE = ULong.valueOf("9223372036854775808");
    public static final Double INITIAL_FLOAT_VALUE = 3.14159d;
    public static final Boolean INITIAL_BOOLEAN_VALUE = true;
    public static final byte[] INITIAL_OPAQUE_VALUE = Hex.decodeHex("0123456789ABCDEF".toCharArray());
    public static final Date INITIAL_TIME_VALUE = new Date(946684800000l);
    public static final ObjectLink INITIAL_OBJLINK_VALUE = new ObjectLink(3, 0);
    public static final Link[] INITIAL_CORELINK_VALUE = new Link[] { new LwM2mLink(null, new LwM2mPath(3442)) };

    private final Random random = new Random(System.currentTimeMillis());

    public LwM2mTestObject() {
        super();
        initialValues = new HashMap<>();

        initialValues.put(4, Collections.EMPTY_MAP);
        initialValues.put(6, Collections.EMPTY_MAP);

        // single
        initialValues.put(110, INITIAL_STRING_VALUE);
        initialValues.put(120, INITIAL_INTEGER_VALUE);
        initialValues.put(125, INITIAL_UNSIGNED_INTEGER_VALUE);
        initialValues.put(130, INITIAL_FLOAT_VALUE);
        initialValues.put(140, INITIAL_BOOLEAN_VALUE);
        initialValues.put(150, INITIAL_OPAQUE_VALUE);
        initialValues.put(160, INITIAL_TIME_VALUE);
        initialValues.put(170, INITIAL_OBJLINK_VALUE);
        initialValues.put(180, INITIAL_CORELINK_VALUE);

        // multi
        initialValues.put(1110, LwM2mResourceInstance.newStringInstance(0, INITIAL_STRING_VALUE));
        initialValues.put(1120, LwM2mResourceInstance.newIntegerInstance(0, INITIAL_INTEGER_VALUE));
        initialValues.put(1125, LwM2mResourceInstance.newUnsignedIntegerInstance(0, INITIAL_UNSIGNED_INTEGER_VALUE));
        initialValues.put(1130, LwM2mResourceInstance.newFloatInstance(0, INITIAL_FLOAT_VALUE));
        initialValues.put(1140, LwM2mResourceInstance.newBooleanInstance(0, INITIAL_BOOLEAN_VALUE));
        initialValues.put(1150, LwM2mResourceInstance.newBinaryInstance(0, INITIAL_OPAQUE_VALUE));
        initialValues.put(1160, LwM2mResourceInstance.newDateInstance(0, INITIAL_TIME_VALUE));
        initialValues.put(1170, LwM2mResourceInstance.newObjectLinkInstance(0, INITIAL_OBJLINK_VALUE));
        initialValues.put(1180, LwM2mResourceInstance.newCoreLinkInstance(0, INITIAL_CORELINK_VALUE));
    }

    private void clearValues() {
        Map<Integer, Object> clearedValues = new HashMap<>();

        clearedValues.put(4, Collections.EMPTY_MAP);
        clearedValues.put(6, Collections.EMPTY_MAP);

        // single
        clearedValues.put(110, "");
        clearedValues.put(120, 0l);
        clearedValues.put(125, ULong.valueOf(0l));
        clearedValues.put(130, 0.0d);
        clearedValues.put(140, false);
        clearedValues.put(150, new byte[0]);
        clearedValues.put(160, new Date(0l));
        clearedValues.put(170, new ObjectLink());
        clearedValues.put(180, new Link[0]);

        // multi
        clearedValues.put(1110, Collections.EMPTY_MAP);
        clearedValues.put(1120, Collections.EMPTY_MAP);
        clearedValues.put(1125, Collections.EMPTY_MAP);
        clearedValues.put(1130, Collections.EMPTY_MAP);
        clearedValues.put(1140, Collections.EMPTY_MAP);
        clearedValues.put(1150, Collections.EMPTY_MAP);
        clearedValues.put(1160, Collections.EMPTY_MAP);
        clearedValues.put(1170, Collections.EMPTY_MAP);
        clearedValues.put(1180, Collections.EMPTY_MAP);

        fireResourcesChange(applyValues(clearedValues));
    }

    private void resetValues() {
        fireResourcesChange(applyValues(initialValues));
    }

    private void randomValues() {

        Map<Integer, Object> randomValues = new HashMap<>();
        randomValues.put(4, generateResourceInstances(new StringGenerator()));

        // single
        randomValues.put(110, new StringGenerator().generate());
        randomValues.put(120, new LongGenerator().generate());
        randomValues.put(125, new ULongGenerator().generate());
        randomValues.put(130, new DoubleGenerator().generate());
        randomValues.put(140, new BooleanGenerator().generate());
        randomValues.put(150, new BytesGenerator().generate());
        randomValues.put(160, new DateGenerator().generate());
        randomValues.put(170, new ObjectLinkGenerator().generate());
        randomValues.put(180, new CoreLinkGenerator().generate());

        // multi
        randomValues.put(1110, generateResourceInstances(new StringGenerator()));
        randomValues.put(1120, generateResourceInstances(new LongGenerator()));
        randomValues.put(1125, generateResourceInstances(new ULongGenerator()));
        randomValues.put(1130, generateResourceInstances(new DoubleGenerator()));
        randomValues.put(1140, generateResourceInstances(new BooleanGenerator()));
        randomValues.put(1150, generateResourceInstances(new BytesGenerator()));
        randomValues.put(1160, generateResourceInstances(new DateGenerator()));
        randomValues.put(1170, generateResourceInstances(new ObjectLinkGenerator()));
        randomValues.put(1180, generateResourceInstances(new CoreLinkGenerator()));

        fireResourcesChange(applyValues(randomValues));
    }

    private void storeArguments(Arguments arguments) {
        // convert Arguments to Map
        Map<Integer, String> argValues = new LinkedHashMap<Integer, String>();
        for (Argument argument : arguments) {
            String value = argument.getValue() == null ? "" : argument.getValue();
            argValues.put(argument.getDigit(), value);
        }
        // put value in resource (4)
        Map<Integer, Object> newParams = new HashMap<>();
        newParams.put(4, argValues);
        fireResourcesChange(applyValues(newParams));
    }

    private ExecuteResponse sendData(LwM2mServer server, Arguments arguments) {
        // get path of resources to send
        List<String> paths = new ArrayList<>();
        LwM2mResource lwM2mResource = resources.get(6);
        if (lwM2mResource != null && lwM2mResource.isMultiInstances()) {
            for (LwM2mResourceInstance instance : lwM2mResource.getInstances().values()) {
                try {
                    paths.add(new LwM2mPath(instance.getValue().toString()).toString());
                } catch (IllegalArgumentException e) {
                    LOG.warn("Instance {} of resource 6 contains invalid path", instance.getId());
                }
            }
        }
        if (paths.isEmpty()) {
            LOG.info("Unable to Send Data to {} as resources 6 {} doesn't contain any valid LWM2M path", server,
                    lwM2mResource);
            return ExecuteResponse.internalServerError("resources 6 doesn't contain any valid LWM2M path");
        }

        ContentFormat format = ContentFormat.SENML_CBOR;
        Argument contenFormatArg = arguments.get(0);
        try {
            if (contenFormatArg != null)
                format = ContentFormat.fromCode(contenFormatArg.getValue());
        } catch (NumberFormatException e) {
            return ExecuteResponse.badRequest(
                    String.format("argument 1 %s is not a valid ContentFormat.", contenFormatArg.getValue()));
        }
        long timeout = 3000;// ms
        Argument timeoutArg = arguments.get(1);
        try {
            if (timeoutArg != null)
                timeout = Long.parseLong(timeoutArg.getValue());
        } catch (NumberFormatException e) {
            return ExecuteResponse
                    .badRequest(String.format("argument 0 %s is not a valid timeout.", timeoutArg.getValue()));
        }

        // send data;
        LOG.info("Try to send data {} using {} to {} (timeout {}ms)...", paths, format, server, timeout);
        getLwM2mClient().getSendService().sendData(server, format, paths, timeout,
                new ResponseCallback<SendResponse>() {
                    @Override
                    public void onResponse(SendResponse response) {
                        if (response.isSuccess()) {
                            LOG.info("... Send Succeed {}", response.getCode());
                        } else {
                            LOG.warn("... Send Response : {} {}", response.getCode(), response.getErrorMessage());
                        }

                    }
                }, new ErrorCallback() {
                    @Override
                    public void onError(Exception e) {
                        LOG.error("... Send failed.", e);
                    }
                });
        return ExecuteResponse.success();
    }

    private Map<Integer, ?> generateResourceInstances(ValueGenerator<?> generator) {
        HashMap<Integer, Object> instances = new HashMap<>();
        for (int i = 0; i < random.nextInt(10); i++) {
            instances.put(i, generator.generate());
        }
        return instances;
    }

    @Override
    public ExecuteResponse execute(LwM2mServer server, int resourceid, Arguments arguments) {
        switch (resourceid) {
        case 0:
            resetValues();
            return ExecuteResponse.success();
        case 1:
            randomValues();
            return ExecuteResponse.success();
        case 2:
            clearValues();
            return ExecuteResponse.success();
        case 3:
            storeArguments(arguments);
            return ExecuteResponse.success();
        case 5:
            return sendData(server, arguments);
        default:
            return super.execute(server, resourceid, arguments);
        }
    }

    /* ***************** Random Generator ****************** */

    static interface ValueGenerator<T> {
        T generate();
    }

    class StringGenerator implements ValueGenerator<String> {
        @Override
        public String generate() {
            return RandomStringUtils.randomAlphanumeric(random.nextInt(20));
        }
    }

    class LongGenerator implements ValueGenerator<Long> {
        @Override
        public Long generate() {
            return random.nextLong();
        }
    }

    class ULongGenerator implements ValueGenerator<ULong> {
        @Override
        public ULong generate() {
            return ULong.valueOf(random.nextLong());
        }
    }

    class DoubleGenerator implements ValueGenerator<Double> {
        @Override
        public Double generate() {
            return random.nextDouble();
        }
    }

    class BooleanGenerator implements ValueGenerator<Boolean> {
        @Override
        public Boolean generate() {
            return random.nextInt(2) == 1;
        }
    }

    class BytesGenerator implements ValueGenerator<byte[]> {
        @Override
        public byte[] generate() {
            byte[] bytes = new byte[random.nextInt(20)];
            random.nextBytes(bytes);
            return bytes;
        }
    }

    class DateGenerator implements ValueGenerator<Date> {
        @Override
        public Date generate() {
            // try to generate random date which is not so out of date.
            long rd = System.currentTimeMillis();

            // remove year randomly
            rd -= (random.nextInt(20) - 10) * 31557600000l;

            // add some variance in the year
            rd += random.nextInt(3155760) * 1000l;

            return new Date(rd);
        }
    }

    class ObjectLinkGenerator implements ValueGenerator<ObjectLink> {
        @Override
        public ObjectLink generate() {
            return new ObjectLink(random.nextInt(ObjectLink.MAXID - 1), random.nextInt(ObjectLink.MAXID - 1));
        }
    }

    class CoreLinkGenerator implements ValueGenerator<Link[]> {

        private LwM2mPath generatePath() {
            int dice4Value = random.nextInt(4);
            switch (dice4Value) {
            case 0:
                return new LwM2mPath(random.nextInt(ObjectLink.MAXID - 1));
            case 1:
                return new LwM2mPath(random.nextInt(ObjectLink.MAXID - 1), random.nextInt(ObjectLink.MAXID - 1));
            case 2:
                return new LwM2mPath(random.nextInt(ObjectLink.MAXID - 1), random.nextInt(ObjectLink.MAXID - 1),
                        random.nextInt(ObjectLink.MAXID - 1));
            case 3:
                return new LwM2mPath(random.nextInt(ObjectLink.MAXID - 1), random.nextInt(ObjectLink.MAXID - 1),
                        random.nextInt(ObjectLink.MAXID - 1), random.nextInt(ObjectLink.MAXID - 1));
            }
            return null; // should not happened
        }

        private Attribute[] generateAttributes() {
            int nbAttributes = random.nextInt(3);
            Map<String, Attribute> attributes = new HashMap<>(nbAttributes);
            for (int i = 0; i < nbAttributes; i++) {
                int dice2value = random.nextInt(2);
                Attribute attr = null;
                switch (dice2value) {
                case 0:
                    attr = new QuotedStringAttribute(RandomStringUtils.randomAlphabetic(random.nextInt(5) + 1),
                            RandomStringUtils.randomAlphanumeric(random.nextInt(5) + 1));
                    break;
                case 1:
                    attr = new UnquotedStringAttribute(RandomStringUtils.randomAlphabetic(random.nextInt(5) + 1),
                            RandomStringUtils.randomAlphanumeric(random.nextInt(5) + 1));
                    break;
                }
                attributes.put(attr.getName(), attr);
            }
            return attributes.values().toArray(new Attribute[attributes.size()]);
        }

        @Override
        public Link[] generate() {
            // define if root path is used or not
            String rootpath = random.nextInt(4) == 0 ? "/" + RandomStringUtils.randomAlphabetic(1)
                    + RandomStringUtils.randomAlphanumeric(random.nextInt(4)) : null;

            // define number of link
            int nbLink = random.nextInt(10);
            // create links
            Link[] links = new Link[nbLink];
            for (int i = 0; i < links.length; i++) {
                // when there is a rootpath first link has oma attribute
                if (rootpath != null && i == 0) {
                    links[i] = new MixedLwM2mLink(rootpath, LwM2mPath.ROOTPATH, new ResourceTypeAttribute("oma.lwm2m"));
                } else {
                    // generate random link with random path and attributes
                    links[i] = new MixedLwM2mLink(rootpath, generatePath(), generateAttributes());
                }
            }
            return links;
        }
    }
}
