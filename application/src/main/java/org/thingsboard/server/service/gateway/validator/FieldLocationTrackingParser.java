/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.gateway.validator;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import lombok.Getter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class FieldLocationTrackingParser extends JsonParser {

    private final JsonParser delegate;
    @Getter
    private final Map<String, JsonLocation> fieldLocations = new HashMap<>();
    private JsonToken previousToken;
    private boolean inObjectArray = false;

    public FieldLocationTrackingParser(JsonParser delegate) {
        this.delegate = delegate;
    }

    @Override
    public JsonToken nextToken() throws IOException {
        JsonToken token = delegate.nextToken();
        if (token == JsonToken.FIELD_NAME
                || (previousToken == JsonToken.START_ARRAY && token == JsonToken.START_OBJECT)
                || (inObjectArray && token == JsonToken.START_OBJECT)) {
            addFieldLocation();
            inObjectArray = true;
        } else if (previousToken == JsonToken.END_OBJECT && token == JsonToken.END_ARRAY) {
            inObjectArray = false;
        }
        previousToken = token;
        return token;
    }

    private void addFieldLocation() {
        String fieldPath = delegate.getParsingContext().pathAsPointer().toString()
                .replace("/", ".");
        fieldLocations.put(fieldPath, delegate.currentLocation());
    }

    @Override
    public JsonToken nextValue() throws IOException {
        return delegate.nextValue();
    }

    @Override
    public JsonParser skipChildren() throws IOException {
        return delegate.skipChildren();
    }

    @Override
    public ObjectCodec getCodec() {
        return delegate.getCodec();
    }

    @Override
    public void setCodec(ObjectCodec oc) {
        delegate.setCodec(oc);
    }

    @Override
    public Object getInputSource() {
        return delegate.getInputSource();
    }

    @Override
    public Version version() {
        return delegate.version();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public JsonStreamContext getParsingContext() {
        return delegate.getParsingContext();
    }

    @Override
    public JsonToken getCurrentToken() {
        return delegate.getCurrentToken();
    }

    @Override
    public int getCurrentTokenId() {
        return delegate.currentTokenId();
    }

    @Override
    public boolean hasCurrentToken() {
        return delegate.hasCurrentToken();
    }

    @Override
    public boolean hasTokenId(int id) {
        return delegate.hasTokenId(id);
    }

    @Override
    public boolean hasToken(JsonToken t) {
        return delegate.hasToken(t);
    }

    @Override
    public void clearCurrentToken() {
        delegate.clearCurrentToken();
    }

    @Override
    public JsonToken getLastClearedToken() {
        return delegate.getLastClearedToken();
    }

    @Override
    public void overrideCurrentName(String name) {
        delegate.overrideCurrentName(name);
    }

    @Override
    public JsonLocation getCurrentLocation() {
        return delegate.currentLocation();
    }

    @Override
    public JsonLocation getTokenLocation() {
        return delegate.currentTokenLocation();
    }

    @Override
    public String getCurrentName() throws IOException {
        return delegate.currentName();
    }

    @Override
    public String getText() throws IOException {
        return delegate.getText();
    }

    @Override
    public char[] getTextCharacters() throws IOException {
        return delegate.getTextCharacters();
    }

    @Override
    public int getTextLength() throws IOException {
        return delegate.getTextLength();
    }

    @Override
    public int getTextOffset() throws IOException {
        return delegate.getTextOffset();
    }

    @Override
    public boolean hasTextCharacters() {
        return delegate.hasTextCharacters();
    }

    @Override
    public Number getNumberValue() throws IOException {
        return delegate.getNumberValue();
    }

    @Override
    public NumberType getNumberType() throws IOException {
        return delegate.getNumberType();
    }

    @Override
    public int getIntValue() throws IOException {
        return delegate.getIntValue();
    }

    @Override
    public long getLongValue() throws IOException {
        return delegate.getLongValue();
    }

    @Override
    public BigInteger getBigIntegerValue() throws IOException {
        return delegate.getBigIntegerValue();
    }

    @Override
    public float getFloatValue() throws IOException {
        return delegate.getFloatValue();
    }

    @Override
    public double getDoubleValue() throws IOException {
        return delegate.getDoubleValue();
    }

    @Override
    public BigDecimal getDecimalValue() throws IOException {
        return delegate.getDecimalValue();
    }

    @Override
    public byte[] getBinaryValue(Base64Variant bv) throws IOException {
        return delegate.getBinaryValue(bv);
    }

    @Override
    public String getValueAsString(String def) throws IOException {
        return delegate.getValueAsString(def);
    }
}
