/**
 * Data access contracts: one interface per MongoDB collection ({@code users}, {@code games}, {@code orders},
 * {@code reviews} and {@code media}).
 *
 * <p>The interfaces speak only in {@code model} types and {@code exception} types, so they never depend on
 * {@code service}, {@code ui}, the driver or Hibernate.</p>
 */
package com.mist.dao;
