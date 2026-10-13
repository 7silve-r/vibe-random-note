package com.silver.storage.support;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.springframework.mock.web.MockMultipartFile;

public final class TestFiles {
    private TestFiles() {}

    public static MockMultipartFile png() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
        return new MockMultipartFile("file", "../../fake.html", "text/html", output.toByteArray());
    }
}
