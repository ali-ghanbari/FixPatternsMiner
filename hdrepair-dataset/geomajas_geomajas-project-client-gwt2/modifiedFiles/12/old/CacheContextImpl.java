/*
 * This is part of Geomajas, a GIS framework, http://www.geomajas.org/.
 *
 * Copyright 2008-2011 Geosparc nv, http://www.geosparc.com/, Belgium.
 *
 * The program is available in open source according to the GNU Affero
 * General Public License. All contributions in this program are covered
 * by the Geomajas Contributors License Agreement. For full licensing
 * details, see LICENSE.txt in the project root.
 */

package org.geomajas.plugin.caching.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Implementation for {@link CacheContext}.
 *
 * @author Joachim Van der Auwera
 */
public class CacheContextImpl implements CacheContext {

	private static final long serialVersionUID = 100L;

	private transient final Logger log = LoggerFactory.getLogger(CacheContextImpl.class);

	private Map<String, Object> map = new HashMap<String, Object>();

	public void put(String key, Object object) {
		map.put(key, object);
	}

	public Object get(String key) {
		return map.get(key);
	}

	public <TYPE> TYPE get(String key, Class<TYPE> type) {
		Object res = get(key);
		if (type.isInstance(res)) {
			return (TYPE) res;
		}
		return null;
	}

	Set<Map.Entry<String, Object>> entries() {
		return map.entrySet();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CacheContextImpl)) {
			return false;
		}

		CacheContextImpl that = (CacheContextImpl) o;

		if (map.size() != that.map.size()) {
			return false;
		} else {
			for (Map.Entry<String, Object> one : map.entrySet()) {
				if (!objectEquals(one.getValue(), that.map.get(one.getKey()))) {
					log.debug("Map key {} does not match.", one.getKey());
					return false;
				}
			}
		}
		return true;
	}

	private boolean objectEquals(Object left, Object right) {
		if (null == left) {
			return null == right;
		} else {
			return left.equals(right);
		}
	}

	@Override
	public int hashCode() {
		return map.hashCode();
	}
}
