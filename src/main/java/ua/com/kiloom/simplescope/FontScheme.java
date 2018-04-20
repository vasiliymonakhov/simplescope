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

    private FontScheme(Font scopeFont, Font guiFont, Font valFont, Font borderFont) {
        this.scopeFont = scopeFont;
        this.guiFont = guiFont;
        this.valFont = valFont;
        this.borderFont = borderFont;
    }

    final static FontScheme STANDART = new FontScheme(new Font("Noto Sans", Font.BOLD, 13),
            new Font("Noto Sans", 0, 13),
            new Font("Noto Sans", 0, 18),
            new Font("Noto Sans", 0, 9));



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
