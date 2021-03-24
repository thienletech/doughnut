package com.odde.doughnut.algorithms;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamSource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class ImageUtilsTest {

    @Test
    void shouldNotTouchSmallImage() throws IOException {
        InputStreamSource stream = buildImage(300, 300);
        BufferedImage output = resizeImage(stream, "img.png");
        assertThat(output.getWidth(), equalTo(300));
        assertThat(output.getHeight(), equalTo(300));
    }

    @Test
    void shouldResizeLargeImage() throws IOException {
        InputStreamSource stream = buildImage(3000, 200);
        BufferedImage output = resizeImage(stream, "img.png");
        assertThat(output.getWidth(), equalTo(600));
        assertThat(output.getHeight(), equalTo(40));
    }

    @Test
    void shouldResizeTooTallImage() throws IOException {
        InputStreamSource stream = buildImage(200, 3000);
        BufferedImage output = resizeImage(stream, "img.png");
        assertThat(output.getWidth(), equalTo(40));
        assertThat(output.getHeight(), equalTo(600));
    }

    private BufferedImage resizeImage(InputStreamSource stream, String originalFilename) throws IOException {
        byte[] bytes = new ImageUtils().toResizedImageByteArray(stream, originalFilename);
        BufferedImage output = ImageIO.read(new ByteArrayInputStream(bytes));
        return output;
    }

    private InputStreamSource buildImage(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.drawString("Welcome to img", 0, 0);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", stream);
        return new InputStreamSource() {
            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(stream.toByteArray());
            }
        };
    }

}