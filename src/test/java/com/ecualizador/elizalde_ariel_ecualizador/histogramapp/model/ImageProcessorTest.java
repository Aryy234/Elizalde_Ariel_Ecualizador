package com.ecualizador.elizalde_ariel_ecualizador.histogramapp.model;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class ImageProcessorTest {

    private final ImageProcessor processor = new ImageProcessor();

    @Test
    void grayscaleCreatesANewImageWithEqualColorChannels() {
        WritableImage source = oneRow(Color.RED, Color.BLUE);

        Image result = processor.convertToGrayscale(source);
        Color pixel = result.getPixelReader().getColor(0, 0);

        assertNotSame(source, result);
        assertEquals(pixel.getRed(), pixel.getGreen(), 0.001);
        assertEquals(pixel.getGreen(), pixel.getBlue(), 0.001);
    }

    @Test
    void sliceMakesSelectedRangeWhiteAndOtherPixelsBlack() {
        WritableImage source = oneRow(Color.gray(0.25), Color.gray(0.75));

        Image result = processor.applyIntensitySlice(source, 0, 127, false);

        assertEquals(Color.WHITE, result.getPixelReader().getColor(0, 0));
        assertEquals(Color.BLACK, result.getPixelReader().getColor(1, 0));
    }

    @Test
    void invertedSliceSwapsBlackAndWhite() {
        WritableImage source = oneRow(Color.gray(0.25), Color.gray(0.75));

        Image result = processor.applyIntensitySlice(source, 0, 127, true);

        assertEquals(Color.BLACK, result.getPixelReader().getColor(0, 0));
        assertEquals(Color.WHITE, result.getPixelReader().getColor(1, 0));
    }

    @Test
    void equalizationSpreadsTwoUsedIntensitiesToBlackAndWhite() {
        WritableImage source = oneRow(Color.gray(0.2), Color.gray(0.4));

        Image result = processor.equalize(source);

        assertEquals(0.0, result.getPixelReader().getColor(0, 0).getRed(), 0.01);
        assertEquals(1.0, result.getPixelReader().getColor(1, 0).getRed(), 0.01);
    }

    @Test
    void equalizationPreservesHueAndSaturationOfColorPixels() {
        Color darkRed = Color.hsb(0, 0.8, 0.2);
        Color lightBlue = Color.hsb(220, 0.7, 0.6);
        WritableImage source = oneRow(darkRed, lightBlue);

        Image result = processor.equalize(source);
        Color resultBlue = result.getPixelReader().getColor(1, 0);

        assertEquals(lightBlue.getHue(), resultBlue.getHue(), 0.5);
        assertEquals(lightBlue.getSaturation(), resultBlue.getSaturation(), 0.01);
    }

    @Test
    void positiveBrightnessMakesAnImageLighterAndPreservesColorDifferences() {
        WritableImage source = oneRow(Color.color(0.1, 0.2, 0.3), Color.BLACK);

        Image result = processor.adjustBrightness(source, 30);
        Color pixel = result.getPixelReader().getColor(0, 0);

        assertEquals(0.4, pixel.getRed(), 0.01);
        assertEquals(0.5, pixel.getGreen(), 0.01);
        assertEquals(0.6, pixel.getBlue(), 0.01);
    }

    @Test
    void brightnessIsLimitedToTheValidColorRange() {
        WritableImage source = oneRow(Color.gray(0.9), Color.gray(0.1));

        Image lighter = processor.adjustBrightness(source, 50);
        Image darker = processor.adjustBrightness(source, -50);

        assertEquals(1.0, lighter.getPixelReader().getColor(0, 0).getRed(), 0.01);
        assertEquals(0.0, darker.getPixelReader().getColor(1, 0).getRed(), 0.01);
    }

    private WritableImage oneRow(Color first, Color second) {
        WritableImage image = new WritableImage(2, 1);
        image.getPixelWriter().setColor(0, 0, first);
        image.getPixelWriter().setColor(1, 0, second);
        return image;
    }
}
