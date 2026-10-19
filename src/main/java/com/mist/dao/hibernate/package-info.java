/**
 * Data access implementations on Hibernate ORM with the MongoDB extension.
 *
 * <p>Each DAO logs every operation through {@code DbOperationLogger} and rethrows driver and Hibernate failures as
 * {@code DataAccessException}. Together with {@code config}, this is the only place that may use Hibernate or the driver.</p>
 */
package com.mist.dao.hibernate;
