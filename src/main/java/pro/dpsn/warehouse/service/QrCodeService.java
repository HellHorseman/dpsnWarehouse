package pro.dpsn.warehouse.service;

import com.google.zxing.WriterException;

import java.io.IOException;

public interface QrCodeService {

    public byte[] generateQrCode(String text, int width, int height) throws WriterException, IOException;
}
