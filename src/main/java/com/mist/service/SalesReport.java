package com.mist.service;

import com.mist.model.Game;

import java.math.BigDecimal;
import java.util.List;

/**
 * A developer's sales figures, for the developer's dashboard. Each game's own figures are in its {@code stats}.
 *
 * @param totalRevenue income from every sale minus returns
 * @param unitsSold    copies sold across all games, minus returns
 * @param games        the developer's games, drafts included, newest first
 */
public record SalesReport(BigDecimal totalRevenue, int unitsSold, List<Game> games) {
}
