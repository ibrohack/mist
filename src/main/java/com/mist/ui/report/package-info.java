/**
 * The PDF reports MIST prints from what the UI shows: the purchase receipt of an order (behaviors CART-6 and ORD-3)
 * and a developer's sales report (STUDIO-7) of {@code docs/ui-design.md}.
 *
 * <p>{@code Reports} fills the JasperReports templates {@code receipt.jrxml} and {@code sales.jrxml} with labels from
 * the UI bundles and values formatted in the current language, and exports them to a PDF file the user chose. A
 * report that cannot be built or written arrives as a {@code ReportException}, which {@code ErrorReporter} shows with
 * its own message.</p>
 */
package com.mist.ui.report;
