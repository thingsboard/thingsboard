// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer.chart.layout;

public class TbContentJustify {

    public static final TbContentJustify NONE
            = new TbContentJustify("HorizontalAlignment.NONE");

    public static final TbContentJustify SPACE_AROUND
            = new TbContentJustify("HorizontalAlignment.SPACE_AROUND");

    private String name;

    private TbContentJustify(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TbContentJustify)) {
            return false;
        }
        final TbContentJustify that = (TbContentJustify) obj;
        if (!this.name.equals(that.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
