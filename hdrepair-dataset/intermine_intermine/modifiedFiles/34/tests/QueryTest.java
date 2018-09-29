package org.flymine.sql.query;

import junit.framework.*;
import java.util.*;

public class QueryTest extends TestCase
{
    private Query q1, q2, q3;

    public QueryTest(String arg1) {
        super(arg1);
    }

    public void setUp()
    {
        q1 = new Query();
        Table t = new Table("mytable");
        Constant c = new Constant("1");
        Field f = new Field("a", t);
        SelectValue sv = new SelectValue(f, null);
        q1.addFrom(t);
        q1.addSelect(sv);
        q1.addWhere(new Constraint(f, Constraint.EQ, c));

        q2 = new Query();
        t = new Table("mytable");
        c = new Constant("1");
        f = new Field("a", t);
        sv = new SelectValue(f, null);
        q2.addFrom(t);
        q2.addSelect(sv);
        q2.addWhere(new Constraint(f, Constraint.EQ, c));

        q3 = new Query();
        t = new Table("anotherTable");
        c = new Constant("2");
        f = new Field("b", t);
        sv = new SelectValue(f, null);
        q3.addFrom(t);
        q3.addSelect(sv);
        q3.addWhere(new Constraint(f, Constraint.LT, c));
    }
    public void testGetSQLString() throws Exception {
        Query q = new Query();
        Table t = new Table("mytable");
        Constant c = new Constant("1");
        Field f = new Field("a", t);
        SelectValue sv = new SelectValue(f, null);
        q.addFrom(t);
        q.addSelect(sv);
        q.addWhere(new Constraint(f, Constraint.EQ, c));
        assertEquals("SELECT mytable.a FROM mytable WHERE mytable.a = 1", q.getSQLString());
    }

    public void testEquals() throws Exception {
        assertEquals(q1, q1);
        assertEquals(q1, q2);
        assertTrue("Expected q1 to not equal q3", !q1.equals(q3));
    }

    public void testHashCode() throws Exception {
        assertEquals(q1.hashCode(), q1.hashCode());
        assertEquals(q1.hashCode(), q2.hashCode());
        assertTrue("Expected q1 hashcode not to equal q3 hashcode", q1.hashCode() != q3.hashCode());
    }

    public void testConstructNullString() throws Exception {
        try {
            Query q1 = new Query((String) null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testConstructEmptyString() throws Exception {
        try {
            Query q1 = new Query("");
            fail("Expected: IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
        }
    }

    public void testConstructIllegalString() throws Exception {
        try {
            Query q1 = new Query("A load of rubbish");
            fail("Expected: IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
        }
    }

    public void testSelectNoAlias() throws Exception {
        Query q1 = new Query("select table1.field1 from table1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        assertEquals(q2, q1);
    }

    public void testSelectTableAlias() throws Exception {
        Query q1 = new Query("select t1.field1 from table1 as t1");
        Query q1_alt = new Query("select t1.field1 from table1 t1");
        Query q2 = new Query();
        Table t1 = new Table("table1", "t1");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        assertEquals(q2, q1);
        assertEquals(q2, q1_alt);
    }

    public void testSelectFieldAlias() throws Exception {
        Query q1 = new Query("select table1.field1 as alias1 from table1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, "alias1");
        q2.addSelect(sv1);
        q2.addFrom(t1);
        assertEquals(q2, q1);
    }

    public void testSelectFunctionAlias() throws Exception {
        Query q1 = new Query("select max(table1.field1) as alias1 from table1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Function func1 = new Function(Function.MAX);
        func1.add(f1);
        SelectValue sv1 = new SelectValue(func1, "alias1");
        q2.addSelect(sv1);
        q2.addFrom(t1);
        assertEquals(q2, q1);
    }

    public void testSelectPlusFunctionAlias() throws Exception {
        Query q1 = new Query("select table1.field1 + table1.field2 as alias1 from table1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Function func1 = new Function(Function.PLUS);
        func1.add(f1);
        func1.add(f2);
        SelectValue sv1 = new SelectValue(func1, "alias1");
        q2.addSelect(sv1);
        q2.addFrom(t1);
        assertEquals(q2, q1);
    }

    public void testSelectTwoSameTables() throws Exception {
        Query q1 = new Query("select t1.field1 from table1 t1, table1 t2");
        Query q2 = new Query();
        Table t1 = new Table("table1", "t1");
        Table t2 = new Table("table1", "t2");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addFrom(t2);
        assertEquals(q2, q1);
    }

    public void testSelectTwoDifferentTables() throws Exception {
        Query q1 = new Query("select table1.field1 from table1, table2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Table t2 = new Table("table2");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addFrom(t2);
        assertEquals(q2, q1);
    }

    public void testSelectTwoDifferentTablesWithAliases() throws Exception {
        Query q1 = new Query("select t1.field1 from table1 t1, table2 t2");
        Query q2 = new Query();
        Table t1 = new Table("table1", "t1");
        Table t2 = new Table("table2", "t2");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addFrom(t2);
        assertEquals(q2, q1);
    }

    public void testSelectFromSubQuery() throws Exception {
        Query q1 = new Query("select t1.field1 from (select table2.field2 from table2) as t1");
        Query q2 = new Query();
        SubQuery sq1 = new SubQuery(new Query("select table2.field2 from table2"), "t1");
        Field f1 = new Field("field1", sq1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(sq1);
        //throw new Exception(q1.getSQLString() + "     " + q2.getSQLString());
        assertEquals(q2, q1);
    }

    public void testWhereOneEqualConstraint() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where table1.field1 = 1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Constant c = new Constant("1");
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addWhere(new Constraint(f1, Constraint.EQ, c));
        assertEquals(q2, q1);
    }

    public void testWhereTwoEqualConstraints() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where table1.field1 = 1 and table1.field2 = 2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Constant c1 = new Constant("1");
        Constant c2 = new Constant("2");
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addWhere(new Constraint(f1, Constraint.EQ, c1));
        q2.addWhere(new Constraint(f2, Constraint.EQ, c2));
        assertEquals(q2, q1);
    }

    public void testTreeParserRulesForConstraint() throws Exception {
        // (aleft != aleft) becomes NOT (aleft = aright)
        Query lq1 = new Query("select t.a from t where t.a != t.b");
        Query lq2 = new Query("select t.a from t where not t.a = t.b");
        assertEquals(lq1, lq2);

        Query lq3 = new Query("select t.a from t where t.a = t.b");
        assertTrue("Expected lq1 to not equal lq3", !lq1.equals(lq3));

        // (bleft >= bright) becomes NOT (bleft < bright)
        lq1 = new Query("select t.a from t where t.a >= t.b");
        lq2 = new Query("select t.a from t where not t.a < t.b");
        assertEquals(lq1, lq2);

        // (cleft <= cright) becomes NOT (cright < cleft)
        lq1 = new Query("select t.a from t where t.a <= t.b");
        lq2 = new Query("select t.a from t where not t.a > t.b");
        assertEquals(lq1, lq2);

        // (dleft > dright) becomes (dright < dleft)
        lq1 = new Query("select t.a from t where t.a < t.b");
        lq2 = new Query("select t.a from t where t.b > t.a");
        assertEquals(lq1, lq2);
    }

    public void testTreeParserRulesForNotConstraint() throws Exception {
        // NOT (NOT a) becomes a
        Query lq1 = new Query("select t.a from t where not not t.a = t.b");
        Query lq2 = new Query("select t.a from t where t.a = t.b");
        assertEquals(lq1, lq2);

        // NOT (b OR c..OR..) becomes NOT b AND NOT (c..OR..)
        lq1 = new Query("select t.a from t where NOT (t.b = t.x OR t.c = t.y)");
        lq2 = new Query("select t.a from t where NOT t.b = t.x AND NOT t.c = t.y");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where NOT (t.b = t.x OR t.c = t.y OR t.c = t.z)");
        lq2 = new Query("select t.a from t where NOT t.b = t.x AND NOT (t.c = t.y OR t.c = t.z)");
        assertEquals(lq1, lq2);

        // NOT (e AND f..AND..) becomes NOT e OR NOT (f..AND..)
        lq1 = new Query("select t.a from t where NOT (t.b = t.x AND t.c = t.y)");
        lq2 = new Query("select t.a from t where NOT t.b = t.x OR NOT t.c = t.y");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where NOT (t.b = t.x AND t.c = t.y AND t.c = t.z)");
        lq2 = new Query("select t.a from t where NOT t.b = t.x OR NOT (t.c = t.y AND t.c = t.z)");
        ConstraintSet a = (ConstraintSet) lq1.getWhere().iterator().next();
        ConstraintSet b = (ConstraintSet) lq2.getWhere().iterator().next();
        Iterator aIter = a.cons.iterator();
        Iterator bIter = b.cons.iterator();
        NotConstraint aA = (NotConstraint) aIter.next();
        NotConstraint aB = (NotConstraint) aIter.next();
        NotConstraint aC = (NotConstraint) aIter.next();
        NotConstraint bA = (NotConstraint) bIter.next();
        NotConstraint bB = (NotConstraint) bIter.next();
        NotConstraint bC = (NotConstraint) bIter.next();
        assertEquals(AbstractConstraint.EQUAL, aA.compare(bA));
        assertEquals(AbstractConstraint.INDEPENDENT, aA.compare(bB));
        assertEquals(AbstractConstraint.INDEPENDENT, aA.compare(bC));
        assertEquals(AbstractConstraint.INDEPENDENT, aB.compare(bA));
        assertEquals(AbstractConstraint.EQUAL, aB.compare(bB));
        assertEquals(AbstractConstraint.INDEPENDENT, aB.compare(bC));
        assertEquals(AbstractConstraint.INDEPENDENT, aC.compare(bA));
        assertEquals(AbstractConstraint.INDEPENDENT, aC.compare(bB));
        assertEquals(AbstractConstraint.EQUAL, aC.compare(bC));
        assertEquals(AbstractConstraint.IMPLIES, a.internalCompare(b));
        assertEquals(AbstractConstraint.IMPLIES, b.internalCompare(a));
        assertEquals(AbstractConstraint.EQUAL, a.compare(b));
        assertEquals(lq1.getSQLString(), lq2.getSQLString());
        assertEquals(lq1, lq2);
    }

    public void testTreeParserRulesForOrConstraint1() throws Exception {
        // (a..OR..) OR b..OR.. becomes a..OR.. OR b..OR..
        Query lq1 = new Query("select t.a from t where (t.a = t.y OR t.a = t.z) OR t.b = t.y");
        Query lq2 = new Query("select t.a from t where t.a = t.y OR t.a = t.z OR t.b = t.y");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where (t.aa = t.y OR t.ab = t.z) OR t.ba = t.y OR t.bb = t.z");
        lq2 = new Query("select t.a from t where t.aa = t.y OR t.ab = t.z OR t.ba = t.y OR t.bb = t.z");
        assertEquals(lq1, lq2);
    }

    public void testTreeParserRulesForOrConstraint2() throws Exception {
        // d..OR.. OR (e..OR..) OR f..OR.. becomes d..OR.. OR e..OR.. OR f..OR..
        Query lq1 = new Query("select t.a from t where t.d = 1 OR (t.ea = 2 OR t.eb = 3) OR t.f = 4");
        Query lq2 = new Query("select t.a from t where t.d = 1 OR t.ea = 2 OR t.eb = 3 OR t.f = 4");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where t.da = 1 OR t.db = 5 OR (t.ea = 2 OR t.eb = 3) OR t.f = 4");
        lq2 = new Query("select t.a from t where t.da = 1 OR t.db = 5 OR t.ea = 2 OR t.eb = 3 OR t.f = 4");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where t.d = 1 OR (t.ea = 2 OR t.eb = 3) OR t.fa = 4 OR t.fb = 5");
        lq2 = new Query("select t.a from t where t.d = 1 OR t.ea = 2 OR t.eb = 3 OR t.fa = 4 OR t.fb = 5");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where t.da = 1 OR t.db = 5 OR (t.ea = 2 OR t.eb = 3) OR t.fa = 4 OR t.fb = 5");
        lq2 = new Query("select t.a from t where t.da = 1 OR t.db = 5 OR t.ea = 2 OR t.eb = 3 OR t.fa = 4 OR t.fb = 5");
        assertEquals(lq1, lq2);
    }

    public void testTreeParserRulesForOrConstraint3() throws Exception {
        // g..OR.. OR (h..OR..) becomes g..OR.. OR h..OR..
        Query lq1 = new Query("select t.a from t where t.g = 1 OR (t.ha = 2 OR t.hb = 3)");
        Query lq2 = new Query("select t.a from t where t.g = 1 OR t.ha = 2 OR t.hb = 3");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where t.ga = 1 OR t.gb = 4 OR (t.ha = 2 OR t.hb = 3)");
        lq2 = new Query("select t.a from t where t.ga = 1 OR t.gb = 4 OR t.ha = 2 OR t.hb = 3");
        assertEquals(lq1, lq2);
    }

    public void testTreeParserRulesForOrConstraint4() throws Exception {
        // (i AND j..AND..) OR k..OR.. becomes (i OR k..OR..) AND ((j..AND..) OR k..OR..)
        Query lq1 = new Query("select t.a from t where (t.i = 1 AND t.j = 2) OR t.k = 3");
        Query lq2 = new Query("select t.a from t where (t.i = 1 OR t.k = 3) AND (t.j = 2 OR t.k = 3)");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where (t.i = 1 AND t.ja = 2 AND t.jb = 4) OR t.k = 3");
        lq2 = new Query("select t.a from t where (t.i = 1 OR t.k = 3) AND ((t.ja = 2 AND t.jb = 4) OR t.k = 3)");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where (t.i = 1 AND t.j = 2) OR t.ka = 3 OR t.kb = 4");
        lq2 = new Query("select t.a from t where (t.i = 1 OR t.ka = 3 OR t.kb = 4) AND (t.j = 2 OR t.ka = 3 OR t.kb = 4)");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where (t.i = 1 AND t.ja = 2 AND t.jb = 4) OR t.ka = 3 OR t.kb = 5");
        lq2 = new Query("select t.a from t where (t.i = 1 OR t.ka = 3 OR t.kb = 5) AND ((t.ja = 2 AND t.jb = 4) OR t.ka = 3 OR t.kb = 5)");
        assertEquals(lq1, lq2);
    }

    public void testTreeParserRulesForOrConstraint5() throws Exception {
        // l..OR.. OR (m AND n..AND..) OR o..OR.. becomes
        //                      (l..OR.. OR m OR o..OR..) AND (l..OR.. OR (n..AND..) OR o..OR..)
        Query lq1 = new Query("select t.a from t where t.l = 1 OR (t.m = 2 AND t.n = 3) OR t.o = 4");
        Query lq2 = new Query("select t.a from t where (t.l = 1 OR t.m = 2 OR t.o = 4) AND (t.l = 1 OR t.n = 3 OR t.o = 4)");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where t.la = 1 OR t.lb = 5 OR (t.m = 2 AND t.n = 3) OR t.o = 4");
        lq2 = new Query("select t.a from t where (t.la = 1 OR t.lb = 5 OR t.m = 2 OR t.o = 4) AND (t.la = 1 OR t.lb = 5 OR t.n = 3 OR t.o = 4)");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where t.l = 1 OR (t.m = 2 AND t.na = 3 AND t.nb = 6) OR t.o = 4");
        lq2 = new Query("select t.a from t where (t.l = 1 OR t.m = 2 OR t.o = 4) AND (t.l = 1 OR (t.na = 3 AND t.nb = 6) OR t.o = 4)");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where t.la = 1 OR t.lb = 5 OR (t.m = 2 AND t.na = 3 AND t.nb = 6) OR t.o = 4");
        lq2 = new Query("select t.a from t where (t.la = 1 OR t.lb = 5 OR t.m = 2 OR t.o = 4) AND (t.la = 1 OR t.lb = 5 OR (t.na = 3 AND t.nb = 6) OR t.o = 4)");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where t.l = 1 OR (t.m = 2 AND t.n = 3) OR t.oa = 4 OR t.ob = 7");
        lq2 = new Query("select t.a from t where (t.l = 1 OR t.m = 2 OR t.oa = 4 OR t.ob = 7) AND (t.l = 1 OR t.n = 3 OR t.oa = 4 OR t.ob = 7)");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where t.la = 1 OR t.lb = 5 OR (t.m = 2 AND t.n = 3) OR t.oa = 4 OR t.ob = 7");
        lq2 = new Query("select t.a from t where (t.la = 1 OR t.lb = 5 OR t.m = 2 OR t.oa = 4 OR t.ob = 7) AND (t.la = 1 OR t.lb = 5 OR t.n = 3 OR t.oa = 4 OR t.ob = 7)");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where t.l = 1 OR (t.m = 2 AND t.na = 3 AND t.nb = 6) OR t.oa = 4 OR t.ob = 7");
        lq2 = new Query("select t.a from t where (t.l = 1 OR t.m = 2 OR t.oa = 4 OR t.ob = 7) AND (t.l = 1 OR (t.na = 3 AND t.nb = 6) OR t.oa = 4 OR t.ob = 7)");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where t.la = 1 OR t.lb = 5 OR (t.m = 2 AND t.na = 3 AND t.nb = 6) OR t.oa = 4 OR t.ob = 7");
        lq2 = new Query("select t.a from t where (t.la = 1 OR t.lb = 5 OR t.m = 2 OR t.oa = 4 OR t.ob = 7) AND (t.la = 1 OR t.lb = 5 OR (t.na = 3 AND t.nb = 6) OR t.oa = 4 OR t.ob = 7)");
        assertEquals(lq1, lq2);
    }

    public void testTreeParserRulesForOrConstraint6() throws Exception {
        // p..OR.. OR (q AND r..AND..) becomes (p..OR.. OR q) AND (p..OR.. OR (r..AND..))
        Query lq1 = new Query("select t.a from t where t.p = 1 OR (t.q = 2 AND t.r = 3)");
        Query lq2 = new Query("select t.a from t where (t.p = 1 OR t.q = 2) AND (t.p = 1 OR t.r = 3)");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where t.pa = 1 OR t.pb = 4 OR (t.q = 2 AND t.r = 3)");
        lq2 = new Query("select t.a from t where (t.pa = 1 OR t.pb = 4 OR t.q = 2) AND (t.pa = 1 OR t.pb = 4 OR t.r = 3)");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where t.p = 1 OR (t.q = 2 AND t.ra = 3 AND t.rb = 5)");
        lq2 = new Query("select t.a from t where (t.p = 1 OR t.q = 2) AND (t.p = 1 OR (t.ra = 3 AND t.rb = 5))");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where t.pa = 1 OR t.pb = 4 OR (t.q = 2 AND t.ra = 3 AND t.rb = 5)");
        lq2 = new Query("select t.a from t where (t.pa = 1 OR t.pb = 4 OR t.q = 2) AND (t.pa = 1 OR t.pb = 4 OR (t.ra = 3 AND t.rb = 5))");
        assertEquals(lq1, lq2);
    }

    public void testTreeParserRulesForAndConstraint1() throws Exception {
        // (a..AND..) AND b..AND.. becomes a..AND.. b..AND..
        Query lq1 = new Query("select t.a from t where (t.aa = 1 AND t.ab = 2) AND t.b = 3");
        Query lq2 = new Query("select t.a from t where t.aa = 1 AND t.ab = 2 AND t.b = 3");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where (t.aa = 1 AND t.ab = 2) AND t.ba = 3 AND t.bb = 4");
        lq2 = new Query("select t.a from t where t.aa = 1 AND t.ab = 2 AND t.ba = 3 AND t.bb = 4");
        assertEquals(lq1, lq2);
    }

    public void testTreeParserRulesForAndConstraint2() throws Exception {
        // d..AND.. AND (e..AND..) AND f..AND.. becomes d..AND.. AND e..AND.. AND f..AND..
        Query lq1 = new Query("select t.a from t where t.d = 1 AND (t.ea = 2 AND t.eb = 3) AND t.f = 4");
        Query lq2 = new Query("select t.a from t where t.d = 1 AND t.ea = 2 AND t.eb = 3 AND t.f = 4");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where t.da = 1 AND t.db = 5 AND (t.ea = 2 AND t.eb = 3) AND t.f = 4");
        lq2 = new Query("select t.a from t where t.da = 1 AND t.db = 5 AND t.ea = 2 AND t.eb = 3 AND t.f = 4");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where t.d = 1 AND (t.ea = 2 AND t.eb = 3) AND t.fa = 4 AND t.fb = 6");
        lq2 = new Query("select t.a from t where t.d = 1 AND t.ea = 2 AND t.eb = 3 AND t.fa = 4 AND t.fb = 6");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where t.da = 1 AND t.db = 5 AND (t.ea = 2 AND t.eb = 3) AND t.fa = 4 AND t.fb = 6");
        lq2 = new Query("select t.a from t where t.da = 1 AND t.db = 5 AND t.ea = 2 AND t.eb = 3 AND t.fa = 4 AND t.fb = 6");
        assertEquals(lq1, lq2);
    }

    public void testTreeParserRulesForAndConstraint5() throws Exception {
        // g..AND.. AND (h..AND..) becomes g..AND.. h..AND..
        Query lq1 = new Query("select t.a from t where t.g = 1 AND (t.ha = 2 AND t.hb = 3)");
        Query lq2 = new Query("select t.a from t where t.g = 1 AND t.ha = 2 AND t.hb = 3");
        assertEquals(lq1, lq2);

        lq1 = new Query("select t.a from t where t.ga = 1 AND t.gb = 4 AND (t.ha = 2 AND t.hb = 3)");
        lq2 = new Query("select t.a from t where t.ga = 1 AND t.gb = 4 AND t.ha = 2 AND t.hb = 3");
        assertEquals(lq1, lq2);
    }

    public void testWhereFieldLessThanField() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where table1.field1 < table1.field2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addWhere(new Constraint(f1, Constraint.LT, f2));
        assertEquals(q2, q1);
    }


    public void testWhereNottedFieldLessThanField() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where not table1.field1 < table1.field2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addWhere(new NotConstraint(new Constraint(f1, Constraint.LT, f2)));
        assertEquals(q2, q1);
    }


    public void testWhereFieldLessThanFunction() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where table1.field1 < avg(table1.field2)");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Function func1 = new Function(Function.AVG);
        func1.add(f2);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addWhere(new Constraint(f1, Constraint.LT, func1));
        assertEquals(q2, q1);
    }


    public void testWhereFieldLessThanPlusFunction() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where table1.field1 < table1.field2 + table1.field3");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Field f3 = new Field("field3", t1);
        Function func1 = new Function(Function.PLUS);
        func1.add(f2);
        func1.add(f3);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addWhere(new Constraint(f1, Constraint.LT, func1));
        assertEquals(q2, q1);
    }


    public void testWhereFieldInSubQuery() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where table1.field1 in (select table2.field2 from table2)");
        Query q2 = new Query();
        Query q3 = new Query();
        Table t1 = new Table("table1");
        Table t2 = new Table("table2");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t2);
        SelectValue sv1 = new SelectValue(f1, null);
        SelectValue sv2 = new SelectValue(f2, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q3.addSelect(sv2);
        q3.addFrom(t2);
        q2.addWhere(new SubQueryConstraint(f1, q3));
        assertEquals(q2, q1);
    }

     public void testWhereConstraintSet() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where (table1.field1 = table1.field2 or table1.field1 = table1.field3)");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Field f3 = new Field("field3", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        ConstraintSet cs1 = new ConstraintSet();
        cs1.add(new Constraint(f1, Constraint.EQ, f2));
        cs1.add(new Constraint(f1, Constraint.EQ, f3));
        q2.addWhere(cs1);
        assertEquals(q2, q1);
    }

     public void testWhereNottedConstraintSet() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where not (table1.field1 = table1.field2 or table1.field1 = table1.field3)");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Field f3 = new Field("field3", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addWhere(new NotConstraint(new Constraint(f1, Constraint.EQ, f2)));
        q2.addWhere(new NotConstraint(new Constraint(f1, Constraint.EQ, f3)));
        assertEquals(q2, q1);
    }

    public void testOrderBySingleField() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 order by table1.field1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addOrderBy(f1);
        assertEquals(q2, q1);
    }

    public void testOrderByTwoFields() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 order by table1.field1, table1.field2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addOrderBy(f1);
        q2.addOrderBy(f2);
        assertEquals(q2, q1);
    }

    public void testOrderByFunction() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 order by table1.field1 + table1.field2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Function func1 = new Function(Function.PLUS);
        func1.add(f1);
        func1.add(f2);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addOrderBy(func1);
        assertEquals(q2, q1);
    }

    public void testGroupBySingleField() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        assertEquals(q2, q1);
    }

    public void testGroupByTwoFields() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1, table1.field2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        q2.addGroupBy(f2);
        assertEquals(q2, q1);
    }

    public void testGroupByFunction() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1 + table1.field2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Function func1 = new Function(Function.PLUS);
        func1.add(f1);
        func1.add(f2);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(func1);
        assertEquals(q2, q1);
    }

    public void testHavingOneLessThanConstraint() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1 having table1.field1 < 1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Constant c = new Constant("1");
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        q2.addHaving(new Constraint(f1, Constraint.LT, c));
        assertEquals(q2, q1);
    }

    public void testHavingTwoEqualConstraints() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1 having table1.field1 = 1 and table1.field2 = 2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Constant c1 = new Constant("1");
        Constant c2 = new Constant("2");
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        q2.addHaving(new Constraint(f1, Constraint.EQ, c1));
        q2.addHaving(new Constraint(f2, Constraint.EQ, c2));
        assertEquals(q2, q1);
    }

    public void testHavingFieldLessThanField() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1 having table1.field1 < table1.field2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        q2.addHaving(new Constraint(f1, Constraint.LT, f2));
        assertEquals(q2, q1);
    }


    public void testHavingNottedFieldLessThanField() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1 having not table1.field1 < table1.field2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        q2.addHaving(new NotConstraint(new Constraint(f1, Constraint.LT, f2)));
        assertEquals(q2, q1);
    }


    public void testHavingFieldLessThanFunction() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1 having table1.field1 < avg(table1.field2)");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Function func1 = new Function(Function.AVG);
        func1.add(f2);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        q2.addHaving(new Constraint(f1, Constraint.LT, func1));
        assertEquals(q2, q1);
    }


    public void testHavingFieldLessThanPlusFunction() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1 having table1.field1 < table1.field2 + table1.field3");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Field f3 = new Field("field3", t1);
        Function func1 = new Function(Function.PLUS);
        func1.add(f2);
        func1.add(f3);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        q2.addHaving(new Constraint(f1, Constraint.LT, func1));
        assertEquals(q2, q1);
    }


    public void testHavingFieldInSubQuery() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1 having table1.field1 in (select table2.field2 from table2)");
        Query q2 = new Query();
        Query q3 = new Query();
        Table t1 = new Table("table1");
        Table t2 = new Table("table2");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t2);
        SelectValue sv1 = new SelectValue(f1, null);
        SelectValue sv2 = new SelectValue(f2, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        q3.addSelect(sv2);
        q3.addFrom(t2);
        q2.addHaving(new SubQueryConstraint(f1, q3));
        assertEquals(q2, q1);
    }

     public void testHavingConstraintSet() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1 having (table1.field1 = table1.field2 or table1.field1 = table1.field3)");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Field f3 = new Field("field3", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        ConstraintSet cs1 = new ConstraintSet();
        cs1.add(new Constraint(f1, Constraint.EQ, f2));
        cs1.add(new Constraint(f1, Constraint.EQ, f3));
        q2.addHaving(cs1);
        assertEquals(q2, q1);
    }

     public void testHavingNottedConstraintSet() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 group by table1.field1 having not (table1.field1 = table1.field2 or table1.field1 = table1.field3)");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Field f3 = new Field("field3", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addGroupBy(f1);
        q2.addHaving(new NotConstraint(new Constraint(f1, Constraint.EQ, f2)));
        q2.addHaving(new NotConstraint(new Constraint(f1, Constraint.EQ, f3)));
        assertEquals(q2, q1);
    }

    public void testSelectDistinct() throws Exception {
        Query q1 = new Query("select distinct table1.field1 from table1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.setDistinct(true);
        assertEquals(q2, q1);
    }

    public void testExplain() throws Exception {
        Query q1 = new Query("explain select table1.field1 from table1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.setExplain(true);
        assertEquals(q2, q1);
    }

    public void testQuiteComplexQuery() throws Exception {
        Query q1 = new Query("select t1.field1 as first, table2.field1, count(*) as c, max(t1.field2) as mx from table1 t1, table2, (select table3.field2 as f1 from table3) as t3 where t1.field3 = table2.field1 and t3.f1 = table2.field3 and (t1.field4 = table2.field2 or t1.field4 = table2.field3) group by t1.field1, table2.field1 having (t1.field1 = table2.field2 or t1.field1 = table2.field3) order by t1.field1, table2.field1 limit 100 offset 10");
        Query q2 = new Query();
        Table t1 = new Table("table1", "t1");
        Table t2 = new Table("table2");
        Table t3 = new Table("table3");
        Field t1f1 = new Field("field1", t1);
        Field t1f2 = new Field("field2", t1);
        Field t1f3 = new Field("field3", t1);
        Field t1f4 = new Field("field4", t1);
        Field t2f1 = new Field("field1", t2);
        Field t2f2 = new Field("field2", t2);
        Field t2f3 = new Field("field3", t2);
        Field t3f2 = new Field("field2", t3);

        Query q3 = new Query();

        q3.addSelect(new SelectValue(t3f2, "f1"));
        q3.addFrom(t3);

        SubQuery sq1 = new SubQuery(q3, "t3");
        Field t3f1 = new Field("f1", sq1);

        SelectValue sv1 = new SelectValue(t1f1, "first");
        SelectValue sv2 = new SelectValue(t2f1, null);
        SelectValue sv3 = new SelectValue(new Function(Function.COUNT), "c");
        Function func1 = new Function(Function.MAX);
        func1.add(t1f2);
        SelectValue sv4 = new SelectValue(func1, "mx");

        ConstraintSet cs1 = new ConstraintSet();
        cs1.add(new Constraint(t1f4, Constraint.EQ, t2f2));
        cs1.add(new Constraint(t1f4, Constraint.EQ, t2f3));

        ConstraintSet cs2 = new ConstraintSet();
        cs2.add(new Constraint(t1f1, Constraint.EQ, t2f2));
        cs2.add(new Constraint(t1f1, Constraint.EQ, t2f3));


        q2.addSelect(sv1);
        q2.addSelect(sv2);
        q2.addSelect(sv3);
        q2.addSelect(sv4);
        q2.addFrom(t1);
        q2.addFrom(t2);
        q2.addFrom(sq1);

        q2.addWhere(new Constraint(t1f3, Constraint.EQ, t2f1));
        q2.addWhere(new Constraint(t2f3, Constraint.EQ, t3f1));
        q2.addWhere(cs1);

        q2.addGroupBy(t1f1);
        q2.addGroupBy(t2f1);

        q2.addHaving(cs2);

        q2.addOrderBy(t1f1);
        q2.addOrderBy(t2f1);

        q2.setLimitOffset(100,10);

        assertEquals(q2, q1);
    }

    public void testLimitOffset() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 limit 100 offset 10");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.setLimitOffset(100,10);
        assertEquals(q2, q1);
    }

    public void testSubQueryScope() throws Exception {
        Query q1 = new Query("select t1.f1 as v1, t2.f1 as v2 from t1, (select t3.f1 as v3 from t3 where t3.f1 = t1.f2) as t2");
        Query q2 = new Query();
        Table t1 = new Table("t1");
        Field t1f1 = new Field("f1", t1);
        Table t3 = new Table("t3");
        Field t3f1 = new Field("f1", t3);
        Field t1f2 = new Field("f2", t1);
        Query q3 = new Query();
        SelectValue sv1 = new SelectValue(t1f1, "v1");
        SelectValue sv3 = new SelectValue(t3f1, "v3");
        q3.addSelect(sv3);
        q3.addFrom(t3);
        Constraint c1 = new Constraint(t3f1, Constraint.EQ, t1f2);
        q3.addWhere(c1);
        SubQuery t2 = new SubQuery(q3, "t2");
        Field t2f1 = new Field("f1", t2);
        SelectValue sv2 = new SelectValue(t2f1, "v2");

        q2.addSelect(sv1);
        q2.addSelect(sv2);
        q2.addFrom(t1);
        q2.addFrom(t2);
        assertEquals(q2, q1);
    }

    public void testSubQueryWhereScope() throws Exception {
        Query q1 = new Query("select t1.f1 as v1 from t1 where t1.f1 in (select t3.f1 as v3 from t3 where t3.f1 = t1.f2)");
        Table t3 = new Table("t3");
        Field t3f1 = new Field("f1", t3);
        Query q3 = new Query();
        SelectValue sv3 = new SelectValue(t3f1, "v3");
        q3.addSelect(sv3);
        q3.addFrom(t3);
        Table t1 = new Table("t1");
        Field t1f2 = new Field("f2", t1);
        Constraint c1 = new Constraint(t3f1, Constraint.EQ, t1f2);
        q3.addWhere(c1);

        Query q2 = new Query();
        Field t1f1 = new Field("f1", t1);
        SelectValue sv1 = new SelectValue(t1f1, "v1");
        SubQueryConstraint c2 = new SubQueryConstraint(t1f1, q3);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addWhere(c2);
        assertEquals(q2, q1);
    }
}
