/*
 * Copyright 2011 Witoslaw Koczewsi <wi@koczewski.de>
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero
 * General Public License as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package ilarkesto.core.time;

import java.util.Date;

public class Tm {

	public static final long SECOND = 1000;
	public static final long MINUTE = SECOND * 60;
	public static final long HOUR = MINUTE * 60;
	public static final long DAY = HOUR * 24;
	public static final long WEEK = DAY * 7;

	public static final long MONTH = DAY * 30;
	public static final long YEAR = MONTH * 12;

	private static TmLocalizer tmLocalizer;
	private static TmLocalizerDe tmLocalizerDe;

	public static TmLocalizer getLocalizer(String language) {
		if (language.equals("de")) {
			if (tmLocalizerDe == null) tmLocalizerDe = new TmLocalizerDe();
			return tmLocalizerDe;
		}
		if (tmLocalizer == null) tmLocalizer = new TmLocalizer();
		return tmLocalizer;
	}

	@SuppressWarnings("deprecation")
	public static Date createDate(int year, int month, int day, int min, int sec) {
		return new Date(year - 1900, month - 1, day, min, sec);
	}

	public static Date createDate(int year, int month, int day) {
		return createDate(year, month, day, 0, 0);
	}

	public static Date addDays(Date date, int days) {
		return addDaysToDate(new Date(date.getTime()), days);
	}

	@SuppressWarnings("deprecation")
	public static Date addDaysToDate(Date date, int days) {
		date.setDate(date.getDate() + days);
		return date;
	}

	public static Date createDate(long millis) {
		return new Date(millis);
	}

	@SuppressWarnings("deprecation")
	public static int getMonth(Date date) {
		return date.getMonth() + 1;
	}

	@SuppressWarnings("deprecation")
	public static int getWeekday(Date date) {
		return date.getDay();
	}

	@SuppressWarnings("deprecation")
	public static int getDay(Date date) {
		return date.getDate();
	}

	@SuppressWarnings("deprecation")
	public static int getYear(Date date) {
		return date.getYear() + 1900;
	}

	@SuppressWarnings("deprecation")
	public static int getHour(Date date) {
		return date.getHours();
	}

	@SuppressWarnings("deprecation")
	public static int getMinute(Date date) {
		return date.getMinutes();
	}

	@SuppressWarnings("deprecation")
	public static int getSecond(Date date) {
		return date.getSeconds();
	}

	public static long getNowAsMillis() {
		return System.currentTimeMillis();
	}

	public static Date getNowAsDate() {
		return createDate(getNowAsMillis());
	}

}
