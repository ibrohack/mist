/**
 * Presentation layer: navigation ({@code Navigator}, {@code SceneRouter}, {@code View}), the context controllers
 * get ({@code UiContext}, {@code AppServices}, {@code Backend}), the TailwindFX facade, the message bundles and the
 * session holder. {@code docs/ui-design.md} describes every screen.
 *
 * <p>It may depend on {@code service}, {@code model} and {@code exception}. It never touches {@code dao}; data reaches
 * the UI only through services.</p>
 */
package com.mist.ui;
