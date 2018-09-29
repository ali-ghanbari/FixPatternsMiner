package org.nlpcn.es4sql.parse;

import java.util.ArrayList;
import java.util.List;

import org.durid.sql.ast.statement.*;
import org.nlpcn.es4sql.domain.*;
import org.nlpcn.es4sql.domain.Where.CONN;
import org.nlpcn.es4sql.exception.SqlParseException;
import org.durid.sql.ast.SQLExpr;
import org.durid.sql.ast.SQLOrderBy;
import org.durid.sql.ast.SQLOrderingSpecification;
import org.durid.sql.ast.expr.SQLBetweenExpr;
import org.durid.sql.ast.expr.SQLBinaryOpExpr;
import org.durid.sql.ast.expr.SQLCharExpr;
import org.durid.sql.ast.expr.SQLIdentifierExpr;
import org.durid.sql.ast.expr.SQLInListExpr;
import org.durid.sql.ast.expr.SQLMethodInvokeExpr;
import org.durid.sql.ast.expr.SQLNullExpr;
import org.durid.sql.ast.expr.SQLNumericLiteralExpr;
import org.durid.sql.ast.expr.SQLPropertyExpr;
import org.durid.sql.ast.expr.SQLQueryExpr;
import org.durid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import org.durid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock.Limit;

/**
 * es sql support
 * 
 * @author ansj
 * 
 */
public class SqlParser {

	public SqlParser() {
	};

	public Select parseSelect(SQLQueryExpr mySqlExpr) throws SqlParseException {

		MySqlSelectQueryBlock query = (MySqlSelectQueryBlock) mySqlExpr.getSubQuery().getQuery();

		Select select = new Select();

		findSelect(query, select);

		select.getFrom().addAll(findFrom(query.getFrom()));

		select.setWhere(findWhere(query.getWhere()));

		findLimit(query, select);

		findOrderBy(query, select);

		findGroupBy(query, select);

		return select;
	}


	public Delete parseDelete(SQLDeleteStatement deleteStatement) throws SqlParseException {
		Delete delete = new Delete();

		delete.getFrom().addAll(findFrom(deleteStatement.getTableSource()));

		delete.setWhere(findWhere(deleteStatement.getWhere()));

		return delete;
	}

	private Where findWhere(SQLExpr where) throws SqlParseException {
		if(where == null) {
			return null;
		}

		Where myWhere = Where.newInstance();
		parseWhere(where, myWhere);
		return myWhere;
	}


	private boolean isCond(SQLBinaryOpExpr expr) {
		return expr.getLeft() instanceof SQLIdentifierExpr || expr.getLeft() instanceof SQLPropertyExpr;
	}

	private void parseWhere(SQLExpr expr, Where where) throws SqlParseException {
		if (expr instanceof SQLBinaryOpExpr && !isCond((SQLBinaryOpExpr) expr)) {
			SQLBinaryOpExpr bExpr = (SQLBinaryOpExpr) expr;
			routeCond(bExpr, bExpr.left, where);
			routeCond(bExpr, bExpr.right, where);
		} else {
			explanCond("AND", expr, where);
		}
	}

	private void routeCond(SQLBinaryOpExpr bExpr, SQLExpr sub, Where where) throws SqlParseException {
		if (sub instanceof SQLBinaryOpExpr) {
			parseWhere(bExpr, (SQLBinaryOpExpr) sub, where);
		} else {
			explanCond(bExpr.operator.name, sub, where);
		}
	}

	private void parseWhere(SQLBinaryOpExpr expr, SQLBinaryOpExpr sub, Where where) throws SqlParseException {
		if (isCond(sub)) {
			explanCond(expr.operator.name, sub, where);
		} else {
			if (sub.operator.priority < expr.operator.priority) {
				Where subWhere = new Where(expr.getOperator().name);
				where.addWhere(subWhere);
				parseWhere(sub, subWhere);
			} else {
				parseWhere(sub, where);
			}
		}

	}

	private void explanCond(String opear, SQLExpr expr, Where where) throws SqlParseException {
		if (expr instanceof SQLBinaryOpExpr) {
			SQLBinaryOpExpr soExpr = (SQLBinaryOpExpr) expr;
			Condition condition = new Condition(CONN.valueOf(opear), soExpr.getLeft().toString(), soExpr.getOperator().name, parseValue(soExpr.getRight()));
			where.addWhere(condition);
		} else if (expr instanceof SQLInListExpr) {
			SQLInListExpr siExpr = (SQLInListExpr) expr;
			Condition condition = new Condition(CONN.valueOf(opear), siExpr.getExpr().toString(), siExpr.isNot() ? "NOT IN" : "IN", parseValue(siExpr.getTargetList()));
			where.addWhere(condition);
		} else if (expr instanceof SQLBetweenExpr) {
			SQLBetweenExpr between = ((SQLBetweenExpr) expr);
			Condition condition = new Condition(CONN.valueOf(opear), between.getTestExpr().toString(), between.isNot() ? "NOT BETWEEN" : "BETWEEN", new Object[] { parseValue(between.beginExpr),
					parseValue(between.endExpr) });
			where.addWhere(condition);
		} else {
			throw new SqlParseException("err find condition " + expr.getClass());
		}
	}

	private Object[] parseValue(List<SQLExpr> targetList) throws SqlParseException {
		Object[] value = new Object[targetList.size()];
		for (int i = 0; i < targetList.size(); i++) {
			value[i] = parseValue(targetList.get(i));
		}
		return value;
	}

	private Object parseValue(SQLExpr expr) throws SqlParseException {
		if (expr instanceof SQLNumericLiteralExpr) {
			return ((SQLNumericLiteralExpr) expr).getNumber();
		} else if (expr instanceof SQLCharExpr) {
			return ((SQLCharExpr) expr).getText();
		} else if (expr instanceof SQLMethodInvokeExpr) {
			return expr;
		} else if (expr instanceof SQLNullExpr) {
			return null;
		} else if (expr instanceof SQLIdentifierExpr) {
			return expr;
		} else {
			throw new SqlParseException(
					String.format("Failed to parse SqlExpression of type %s. expression value: %s", expr.getClass(), expr)
			);
		}
	}

	private void findSelect(MySqlSelectQueryBlock query, Select select) throws SqlParseException {
		List<SQLSelectItem> selectList = query.getSelectList();
		for (SQLSelectItem sqlSelectItem : selectList) {
			select.addField(FieldMaker.makeField(sqlSelectItem.getExpr(), sqlSelectItem.getAlias()));
		}
	}

	private void findGroupBy(MySqlSelectQueryBlock query, Select select) throws SqlParseException {
		SQLSelectGroupByClause groupBy = query.getGroupBy();
		if (groupBy == null) {
			return;
		}
		List<SQLExpr> items = groupBy.getItems();
		for (SQLExpr sqlExpr : items) {
			select.addGroupBy(FieldMaker.makeField(sqlExpr, null));
		}
	}

	private void findOrderBy(MySqlSelectQueryBlock query, Select select) throws SqlParseException {
		SQLOrderBy orderBy = query.getOrderBy();

		if (orderBy == null) {
			return;
		}
		List<SQLSelectOrderByItem> items = orderBy.getItems();
		List<String> lists = new ArrayList<>();
		for (SQLSelectOrderByItem sqlSelectOrderByItem : items) {
			SQLExpr expr = sqlSelectOrderByItem.getExpr();
			lists.add(FieldMaker.makeField(expr, null).toString());
			if (sqlSelectOrderByItem.getType() == null) {
				sqlSelectOrderByItem.setType(SQLOrderingSpecification.ASC);
			}
			String type = sqlSelectOrderByItem.getType().toString();
			for (String name : lists) {
				select.addOrderBy(name, type);
			}
			lists.clear();
		}

	}

	private void findLimit(MySqlSelectQueryBlock query, Select select) {
		Limit limit = query.getLimit();

		if (limit == null) {
			return;
		}

		select.setRowCount(Integer.parseInt(limit.getRowCount().toString()));

		if (limit.getOffset() != null)
			select.setOffset(Integer.parseInt(limit.getOffset().toString()));
	}

	/**
	 * Parse the from clause
	 * @param from the from clause.
	 * @return list of From objects represents all the sources.
	 */
	private List<From> findFrom(SQLTableSource from) {
		String[] split = from.getTablename().toString().split(",");

		ArrayList<From> fromList = new ArrayList<>();
		for (String source : split) {
			fromList.add(new From(source.trim()));
		}

		return fromList;
	}

}
