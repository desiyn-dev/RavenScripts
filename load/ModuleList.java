void onLoad() {
    modules.registerSlider("X", "", 10.0, 5.0, client.getDisplaySize()[0], 5.0);
    modules.registerSlider("Y", "", 240.0, 5.0, client.getDisplaySize()[1], 5.0);
    modules.registerButton("Lowercase", true);
    
    for (String module : moduleNames) {
        modules.registerButton("Display " + module, true);
    }
    modules.registerDescription("by @desiyn");
}

// Colors
static final int GREEN = 0xFF00FF00;
static final int RED = 0xFFFF0000;
static final int WHITE = 0xFFFFFFFF;

void onRenderTick(float partialTicks) {
    float x = (float) modules.getSlider(scriptName, "X");
    float y = (float) modules.getSlider(scriptName, "Y");
    boolean lowercase = modules.getButton(scriptName, "Lowercase");
    float lineHeight = render.getFontHeight() + 1;

    for (String module : moduleNames) {
        if (!modules.getButton(scriptName, "Display " + module)) continue;

        // Format module name
        String displayName = lowercase ? module.toLowerCase() : module;
        displayName += ": ";
        
        // Format status
        boolean enabled = modules.isEnabled(module);
        String status = enabled ? "On" : "Off";
        status = lowercase ? status.toLowerCase() : status;
        
        // Calculate positions
        float statusX = x + render.getFontWidth(displayName);
        
        // Render
        render.text(displayName, x, y, 1f, WHITE, true);
        render.text(status, statusX, y, 1f, enabled ? GREEN : RED, true);
        
        y += lineHeight;
    }
}
