package com.mist.ui.report;

import com.mist.model.Order;
import com.mist.model.OrderItem;
import com.mist.model.PaymentCard;
import com.mist.model.User;
import com.mist.ui.I18n;
import com.mist.ui.support.Formats;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds MIST's PDF reports with JasperReports from the data the UI shows.
 *
 * <p>Each report is a JRXML template in {@code com/mist/ui/report/}, compiled the first time it is used. Labels come
 * from the UI bundles ({@code $R{...}}), and amounts and dates are formatted here with {@link Formats}, so a report
 * is in the language the user sees. Runs on a worker thread.</p>
 */
public final class Reports {

    /** The UI's own log. */
    private static final Logger LOG = LoggerFactory.getLogger("com.mist.ui");
    /** The compiled templates, by name. */
    private static final Map<String, JasperReport> COMPILED = new ConcurrentHashMap<>();

    /** Not instantiable: a holder of report builders. */
    private Reports() {
    }

    /**
     * Writes the receipt of an order as a PDF: the buyer, the date, the card, one line per game with its refund
     * state, and the total.
     *
     * @param order  the order
     * @param buyer  the player who placed it
     * @param target the PDF file to write
     * @throws ReportException if the report cannot be built or the file cannot be written
     */
    public static void receipt(Order order, User buyer, Path target) throws ReportException {
        Objects.requireNonNull(order, "order");
        Objects.requireNonNull(buyer, "buyer");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("orderId", order.getId().toHexString());
        parameters.put("orderDate", Formats.dateTime(order.getCreatedAt()));
        parameters.put("buyerName", buyer.fullName());
        parameters.put("buyerEmail", buyer.getEmail());
        PaymentCard card = order.getPaidWith();
        parameters.put("card", card == null ? "—" : Formats.label(card.brand()) + " " + card.masked());
        parameters.put("status", Formats.label(order.getStatus()));
        parameters.put("total", Formats.money(order.getTotal()));
        BigDecimal refunded = order.refundedAmount();
        parameters.put("refunded", refunded.signum() == 0 ? null : Formats.money(refunded));
        List<Map<String, ?>> rows = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            rows.add(Map.of(
                    "name", item.name(),
                    "state", item.refundedAt() == null ? I18n.text("report.paid")
                            : I18n.format("report.refunded", Formats.date(item.refundedAt())),
                    "price", Formats.money(item.price())));
        }
        write("receipt", parameters, rows, target);
    }

    /**
     * Fills a template and exports it as a PDF.
     *
     * @param template   the template's name, without {@code .jrxml}
     * @param parameters the report's parameters
     * @param rows       one map per detail row
     * @param target     the PDF file to write
     * @throws ReportException if the report cannot be built or the file cannot be written
     */
    static void write(String template, Map<String, Object> parameters, List<Map<String, ?>> rows, Path target)
            throws ReportException {
        try (InputStream logo = Reports.class.getResourceAsStream("/com/mist/ui/images/mist-icon-256.png")) {
            Map<String, Object> all = new HashMap<>(parameters);
            all.put(JRParameter.REPORT_RESOURCE_BUNDLE, I18n.bundle());
            all.put(JRParameter.REPORT_LOCALE, I18n.locale());
            all.put("logo", logo);
            JasperPrint print = JasperFillManager.fillReport(compiled(template), all,
                    new JRMapCollectionDataSource(rows));
            JasperExportManager.exportReportToPdfFile(print, target.toString());
            LOG.info("Wrote the {} report to {}", template, target);
        } catch (JRException | JRRuntimeException | java.io.IOException e) {
            // JasperReports reports a file it cannot open as a runtime exception.
            throw new ReportException("Cannot write the " + template + " report to " + target, e);
        }
    }

    /**
     * Returns a compiled template, compiling it the first time.
     *
     * @param template the template's name
     * @return the compiled report
     * @throws JRException if the template cannot be read or compiled
     */
    private static JasperReport compiled(String template) throws JRException {
        JasperReport report = COMPILED.get(template);
        if (report == null) {
            try (InputStream source = Reports.class.getResourceAsStream(template + ".jrxml")) {
                if (source == null) {
                    throw new JRException("Missing report template " + template + ".jrxml");
                }
                report = JasperCompileManager.compileReport(source);
            } catch (java.io.IOException e) {
                throw new JRException("Cannot read the report template " + template, e);
            }
            COMPILED.put(template, report);
        }
        return report;
    }
}
