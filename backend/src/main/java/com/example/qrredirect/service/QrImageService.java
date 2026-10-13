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
        BitMatrix matrix = encode(content, sizePx, sizePx, ec);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        } catch (IOException e) {
            throw new IllegalStateException("Could not write PNG", e);
        }
        return out.toByteArray();
    }

    /**
     * Scalable vector output for print. One path, one rectangle per horizontal run of dark modules.
     * No fixed width/height: it scales to whatever size the printer or design tool wants.
     */
    public String svg(String content, ErrorCorrectionLevel ec) {
        // 0x0 asks ZXing for the smallest matrix: 1 unit per module, quiet zone included.
        BitMatrix m = moduleMatrix(content, ec);

        int w = m.getWidth();
        int h = m.getHeight();

        StringBuilder sb = new StringBuilder(w * h);
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ")
          .append(w).append(' ').append(h)
          .append("\" shape-rendering=\"crispEdges\" role=\"img\" aria-label=\"QR code\">");
        sb.append("<rect width=\"").append(w).append("\" height=\"").append(h).append("\" fill=\"#ffffff\"/>");
        sb.append("<path fill=\"#000000\" d=\"");
        for (int y = 0; y < h; y++) {
            int x = 0;
            while (x < w) {
                if (m.get(x, y)) {
                    int start = x;
                    while (x < w && m.get(x, y)) {
                        x++;
                    }
                    int run = x - start;
                    sb.append('M').append(start).append(' ').append(y)
                      .append('h').append(run).append("v1h-").append(run).append('z');
                } else {
                    x++;
                }
            }
        }
        sb.append("\"/></svg>");
        return sb.toString();
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
