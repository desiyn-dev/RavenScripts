Color startColor, endColor, staticColor;
int colorMode;
float waveSpeed;

void updateColors() {
    colorMode = (int) modules.getSlider("Client", "Mode");
    waveSpeed = (float) modules.getSlider("Client", "Wave Speed");

    int color1Red = (int) modules.getSlider("Client", "Color 1 - Red");
    int color1Green = (int) modules.getSlider("Client", "Color 1 - Green");
    int color1Blue = (int) modules.getSlider("Client", "Color 1 - Blue");
    int color2Red = (int) modules.getSlider("Client", "Color 2 - Red");
    int color2Green = (int) modules.getSlider("Client", "Color 2 - Green");
    int color2Blue = (int) modules.getSlider("Client", "Color 2 - Blue");

    staticColor = new Color(color1Red, color1Green, color1Blue);

    if (colorMode == 2) {
        startColor = staticColor;
        endColor = new Color(color2Red, color2Green, color2Blue);
    }
}

int getRainbow(float seconds, float saturation, float brightness, long index) {
    float hue = ((client.time() + index) % (int)(seconds * 1000)) / (float)(seconds * 1000);
    return Color.HSBtoRGB(hue, saturation, brightness);
}

double getWaveRatio(float seconds, long index) {
    float time = ((client.time() + index) % (int)(seconds * 1000)) / (float)(seconds * 1000);
    double waveRatio = (time <= 0.5) ? (time * 2) : (2 - time * 2);
    return waveRatio;
}

Color blendColors(Color color1, Color color2, double ratio) {
    int r = clamp((int) (color1.getRed() * ratio + color2.getRed() * (1 - ratio)), 0, 255);
    int g = clamp((int) (color1.getGreen() * ratio + color2.getGreen() * (1 - ratio)), 0, 255);
    int b = clamp((int) (color1.getBlue() * ratio + color2.getBlue() * (1 - ratio)), 0, 255);
    return new Color(r, g, b);
}

int clamp(int val, int min, int max) {
    return Math.max(min, Math.min(max, val));
}

int getCurrentColor(long index) {
    switch(colorMode) {
        case 1: // Rainbow
            return getRainbow(waveSpeed, 1f, 1f, index);
        case 2: // Gradient
            double ratio = getWaveRatio(waveSpeed, index);
            return blendColors(startColor, endColor, ratio).getRGB();
        default: // Static
            return staticColor.getRGB();
    }
}

int getBackgroundColor() {
    int alpha = (int) modules.getSlider("Client", "Background Alpha");
    return (alpha << 24);
}
