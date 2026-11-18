/**
 * FXML controllers, one per view.
 *
 * <p>Each controller gets a {@code UiContext} in its constructor, calls services through {@code UiTasks} and hands
 * failures to the {@code ErrorReporter} or marks them on its form. Its methods cite the behavior IDs of
 * {@code docs/ui-design.md}. Controllers follow the same rules as {@code com.mist.ui}: no access to {@code dao}, the
 * driver or Hibernate.</p>
 */
package com.mist.ui.controller;
