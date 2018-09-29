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

import ilarkesto.testng.ATest;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TmTest extends ATest {

	private java.util.Date birthday;
	private java.util.Date armageddon;

	@BeforeMethod
	public void init() {
		birthday = Tm.createDate(1979, 8, 3);
		armageddon = Tm.createDate(2012, 12, 21);
	}

	@Test
	public void getWeekday() {
		assertEquals(Tm.getWeekday(birthday), Calendar.FRIDAY);
		assertEquals(Tm.getWeekday(armageddon), Calendar.FRIDAY);
	}

	@Test
	public void preconditions() {
		Date date = Tm.createDate(1979, 8, 3);
		assertEquals(date, birthday);
		assertEquals(Tm.getYear(date), 1979);
		assertEquals(Tm.getMonth(date), 8);
		assertEquals(Tm.getDay(date), 3);
	}

	@Test
	public void addDays() {
		assertEquals(Tm.addDays(birthday, 1), Tm.createDate(1979, 8, 4));
	}

	@Test
	public void getDay() {
		assertEquals(Tm.getDay(birthday), 3);
		assertEquals(Tm.getDay(armageddon), 21);
	}

	@Test
	public void getMonth() {
		assertEquals(Tm.getMonth(birthday), 8);
		assertEquals(Tm.getMonth(armageddon), 12);
	}

	@Test
	public void getYear() {
		assertEquals(Tm.getYear(birthday), 1979);
		assertEquals(Tm.getYear(armageddon), 2012);
	}

	@Test
	public void createDateFuture() {
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(Tm.createDate(2066, 10, 23));
		assertEquals(cal.get(GregorianCalendar.YEAR), 2066);
		assertEquals(cal.get(GregorianCalendar.MONTH), 9);
		assertEquals(cal.get(GregorianCalendar.DAY_OF_MONTH), 23);
		assertEquals(cal.get(GregorianCalendar.HOUR_OF_DAY), 0);
		assertEquals(cal.get(GregorianCalendar.MINUTE), 0);
	}

	@Test
	public void createDatePast() {
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(Tm.createDate(1979, 8, 3));
		assertEquals(cal.get(GregorianCalendar.YEAR), 1979);
		assertEquals(cal.get(GregorianCalendar.MONTH), 7);
		assertEquals(cal.get(GregorianCalendar.DAY_OF_MONTH), 3);
		assertEquals(cal.get(GregorianCalendar.HOUR_OF_DAY), 0);
		assertEquals(cal.get(GregorianCalendar.MINUTE), 0);
	}

}
