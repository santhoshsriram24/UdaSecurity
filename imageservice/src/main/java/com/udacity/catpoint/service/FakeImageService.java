package com.udacity.catpoint.service;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Fake image service that randomly guesses if an image displays a cat.
 * Useful for testing without requiring actual image recognition capabilities.
 */
public class FakeImageService implements ImageService {
    private final Random r = new Random();

    @Override
    public boolean imageContainsCat(BufferedImage image, float confidenceThreshhold) {
        return r.nextBoolean();
    }
}
