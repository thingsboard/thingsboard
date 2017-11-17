package org.thingsboard.server.common.data.kv;

import lombok.ToString;

import java.util.Objects;
import java.util.Optional;

@ToString
public class BasicDsKvEntry implements DsKvEntry{
    private final Double ds;
    private final KvEntry kv;

    public BasicDsKvEntry(Double Ds, KvEntry kv) {
        this.ds = Ds;
        this.kv = kv;
    }

    @Override
    public String getKey() {
        return kv.getKey();
    }

    @Override
    public DataType getDataType() {
        return kv.getDataType();
    }

    @Override
    public Optional<String> getStrValue() {
        return kv.getStrValue();
    }

    @Override
    public Optional<Long> getLongValue() {
        return kv.getLongValue();
    }

    @Override
    public Optional<Boolean> getBooleanValue() {
        return kv.getBooleanValue();
    }

    @Override
    public Optional<Double> getDoubleValue() {
        return kv.getDoubleValue();
    }

    @Override
    public Object getValue() {
        return kv.getValue();
    }

    @Override
    public Double getDs() {
        return ds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BasicDsKvEntry)) return false;
        BasicDsKvEntry that = (BasicDsKvEntry) o;
        return getDs() == that.getDs() &&
                Objects.equals(kv, that.kv);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDs(), kv);
    }

    @Override
    public String getValueAsString() {
        return kv.getValueAsString();
    }
}
