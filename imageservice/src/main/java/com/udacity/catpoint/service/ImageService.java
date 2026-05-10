package com.udacity.catpoint.service;

import java.awt.image.BufferedImage;

/**
 * Interface for image recognition services that can identify cats in images.
 * Different implementations can use various approaches (fake, AWS Rekognition, etc.)
 */
public interface ImageService {

    /**
     * Analyzes an image to determine if it contains a cat.
     *
     * @param image the image to analyze
     * @param confidenceThreshold the confidence threshold for detection (0-100)
     * @return true if a cat is detected above the confidence threshold, false otherwise
     */
    boolean imageContainsCat(BufferedImage image, float confidenceThreshold);
}
