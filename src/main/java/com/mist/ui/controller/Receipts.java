package com.mist.ui.controller;

import com.mist.model.Order;
import com.mist.model.User;
import com.mist.ui.I18n;
import com.mist.ui.UiContext;
import com.mist.ui.report.Reports;
import com.mist.ui.support.Dialogs;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Saves an order's receipt as a PDF where the user chooses; shared by the cart (CART-6) and the orders (ORD-3).
 */
final class Receipts {

    /** Not instantiable: a holder of the receipt action. */
    private Receipts() {
    }

    /**
     * Asks where to save the receipt of an order and writes it in the background.
     *
     * @param context what the action works with
     * @param order   the order
     * @param buyer   the player who placed it
     */
    static void save(UiContext context, Order order, User buyer) {
        String day = LocalDate.ofInstant(order.getCreatedAt(), ZoneId.systemDefault()).toString();
        // R3: a cancelled chooser does nothing.
        context.files().savePdf(context.navigator().window(), I18n.format("report.receipt.file", day))
                .ifPresent(target -> context.tasks().run(null, () -> {
                    Reports.receipt(order, buyer, target);
                    return target;
                }, saved -> Dialogs.info(context.navigator().window(), I18n.format("report.saved", saved)),
                        // R2: a ReportException is reported as a file error.
                        error -> context.errors().report(context.navigator().window(), error)));
    }
}
