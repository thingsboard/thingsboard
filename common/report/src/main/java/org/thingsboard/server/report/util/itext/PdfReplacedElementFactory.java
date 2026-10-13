// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util.itext;

import org.w3c.dom.Element;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.pdf.BookmarkElement;
import org.xhtmlrenderer.pdf.CheckboxFormField;
import org.xhtmlrenderer.pdf.EmptyReplacedElement;
import org.xhtmlrenderer.pdf.ITextImageElement;
import org.xhtmlrenderer.pdf.TextFormField;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;

public class PdfReplacedElementFactory implements ReplacedElementFactory {

    public PdfReplacedElementFactory() {
    }

    @Override
    public ReplacedElement createReplacedElement(LayoutContext c, BlockBox box,
                                                 UserAgentCallback uac, int cssWidth, int cssHeight) {
        Element e = box.getElement();
        if (e == null) {
            return null;
        }

        String nodeName = e.getNodeName();
        switch (nodeName) {
            case "img":
                String srcAttr = e.getAttribute("src");
                if (srcAttr.isEmpty()) {
                    srcAttr = "/assets/report/components/image-placeholder.svg";
                }
                if (srcAttr.equals("noImage")) {
                    return null;
                }
                FSImage fsImage = uac.getImageResource(srcAttr).getImage();
                if (fsImage != null) {
                    if (cssWidth != -1 || cssHeight != -1) {
                        fsImage = fsImage.scale(cssWidth, cssHeight);
                    }
                    return new ITextImageElement(fsImage);
                }
                break;
            case "input":
                String type = e.getAttribute("type");
                switch (type) {
                    case "hidden":
                        return new EmptyReplacedElement(1, 1);
                    case "checkbox":
                        return new CheckboxFormField(c, box, cssWidth, cssHeight);
                    default:
                        return new TextFormField(c, box, cssWidth, cssHeight);
                }
            case "bookmark":
                if (e.hasAttribute("name")) {
                    String name = e.getAttribute("name");
                    c.addBoxId(name, box);
                    return new BookmarkElement(name);
                }
                return new BookmarkElement(null);
        }

        return null;
    }

    @Override
    public void reset() {
    }

    @Override
    public void remove(Element e) {

    }

    public void remove(String fieldName) {
    }

    @Override
    public void setFormSubmissionListener(FormSubmissionListener listener) {
        // nothing to do, form submission is handled by pdf readers
    }
}
