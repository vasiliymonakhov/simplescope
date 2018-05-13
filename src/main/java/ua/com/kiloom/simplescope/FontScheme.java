package ua.com.kiloom.simplescope;

import java.awt.Font;

/**
 * Шрифтовая схема - набор шрифтов приложения
 * @author Vasily Monakhov
 */
class FontScheme {

    /**
     * Шрифт графика
     */
    private final Font scopeFont;

    /**
     * Шрифт интерфейса
     */
    private final Font guiFont;

    /**
     * Шрифт для обозначения значений
     */
    private final Font valFont;

    /**
     * Шрифт для рамок
     */
    private final Font borderFont;

    /**
     * Создаёт схему
     * @param guiFont шрифт интерфейса
     * @param valFont шрифт для значений
     * @param borderFont шрифт для рамок
     * @param scopeFont шрифт графика
     */
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
