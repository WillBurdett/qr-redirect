package com.example.qrredirect.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.qrredirect.config.AppProperties;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Renders QR codes with ZXing. The encoded content is always the short URL, never the target. */
@Service
public class QrImageService {

    /** Quiet zone (blank border) in modules. The QR spec asks for at least 4. */
    private static final int QUIET_ZONE = 4;

    private final AppProperties props;

    public QrImageService(AppProperties props) {
        this.props = props;
    }

    /** The permanent URL that gets printed as a QR code, e.g. https://qr.example.com/q/Ab3xK9z */
    public String shortUrl(String shortCode) {
        return props.baseUrl() + "/q/" + shortCode;
    }

    public byte[] png(String content, int sizePx, ErrorCorrectionLevel ec) {
        return png(content, sizePx, ec, null);
    }

    public byte[] png(String content, int sizePx, ErrorCorrectionLevel ec, String label) {
        BufferedImage qr = MatrixToImageWriter.toBufferedImage(encode(content, sizePx, sizePx, ec));
        BufferedImage image = (label == null || label.isBlank()) ? qr : withLabel(qr, label);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "PNG", out);
        } catch (IOException e) {
            throw new IllegalStateException("Could not write PNG", e);
        }
        return out.toByteArray();
    }

    private static BufferedImage withLabel(BufferedImage qr, String label) {
        int w = qr.getWidth();
        int labelH = Math.max(24, w / 8);

        BufferedImage out = new BufferedImage(w, qr.getHeight() + labelH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, out.getWidth(), out.getHeight());
            g.drawImage(qr, 0, 0, null);

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.BLACK);
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, Math.round(labelH * 0.6f)));

            FontMetrics fm = g.getFontMetrics();
            int x = (w - fm.stringWidth(label)) / 2;
            int y = qr.getHeight() + (labelH + fm.getAscent() - fm.getDescent()) / 2;
            g.drawString(label, x, y);
        } finally {
            g.dispose();
        }
        return out;
    }

    /**
     * Scalable vector output for print. One path, one rectangle per horizontal run of dark modules.
     * No fixed width/height: it scales to whatever size the printer or design tool wants.
     */
    public String svg(String content, ErrorCorrectionLevel ec) {
        return svg(content, ec, null);
    }

    public String svg(String content, ErrorCorrectionLevel ec, String label) {
        BitMatrix m = moduleMatrix(content, ec);

        int w = m.getWidth();
        int h = m.getHeight();
        boolean hasLabel = label != null && !label.isBlank();
        int labelH = hasLabel ? Math.max(3, w / 8) : 0;
        int totalH = h + labelH;
        int outW = 1024;
        int outH = Math.round(outW * (float) totalH / w);

        StringBuilder sb = new StringBuilder(w * h);
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(outW)
                .append("\" height=\"").append(outH)
                .append("\" viewBox=\"0 0 ").append(w).append(' ').append(totalH)
                .append("\" shape-rendering=\"crispEdges\" role=\"img\" aria-label=\"QR code\">");
        sb.append("<rect width=\"").append(w).append("\" height=\"").append(totalH).append("\" fill=\"#ffffff\"/>");

        sb.append("<path fill=\"#000000\" d=\"");
        for (int y = 0; y < h; y++) {
            int x = 0;
            while (x < w) {
                if (m.get(x, y)) {
                    int start = x;
                    while (x < w && m.get(x, y)) x++;
                    int len = x - start;
                    sb.append('M').append(start).append(' ').append(y)
                            .append('h').append(len).append("v1h-").append(len).append('z');
                } else {
                    x++;
                }
            }
        }
        sb.append("\"/>");

        if (hasLabel) {
            sb.append("<text x=\"").append(w / 2.0)
                    .append("\" y=\"").append(h + labelH * 0.7)
                    .append("\" font-size=\"").append(labelH * 0.6)
                    .append("\" font-family=\"monospace\" text-anchor=\"middle\" fill=\"#000000\" shape-rendering=\"auto\">")
                    .append(escapeXml(label))
                    .append("</text>");
        }
        sb.append("</svg>");
        return sb.toString();
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Smallest possible matrix: one cell per module, quiet zone included (0x0 asks ZXing for no scaling). */
    private BitMatrix moduleMatrix(String content, ErrorCorrectionLevel ec) {
        return encode(content, 0, 0, ec);
    }

    private BitMatrix encode(String content, int width, int height, ErrorCorrectionLevel ec) {
        Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.ERROR_CORRECTION, ec,
                EncodeHintType.MARGIN, QUIET_ZONE);
        try {
            return new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        } catch (WriterException e) {
            throw new IllegalStateException("Could not encode QR code", e);
        }
    }
}
