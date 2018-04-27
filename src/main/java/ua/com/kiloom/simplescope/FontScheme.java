package ua.com.kiloom.simplescope;

import java.awt.Font;

/**
 *
 * @author Vasily Monakhov
 */
class FontScheme {

    private final Font scopeFont;

    private final Font guiFont;

    private final Font valFont;

    private final Font borderFont;

    FontScheme(Font guiFont, Font valFont, Font borderFont, Font scopeFont) {
        this.scopeFont = scopeFont;
        this.guiFont = guiFont;
        this.valFont = valFont;
        this.borderFont = borderFont;
    }

    /**
     * @return the scopeFont
     */
    Font getScopeFont() {
        return scopeFont;
    }

    /**
     * @return the guiFont
     */
    Font getGuiFont() {
        return guiFont;
    }

    /**
     * @return the valFont
     */
    Font getValFont() {
        return valFont;
    }

    /**
     * @return the borderFont
     */
    Font getBorderFont() {
        return borderFont;
    }

}
