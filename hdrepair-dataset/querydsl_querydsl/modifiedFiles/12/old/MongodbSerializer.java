/*
 * Copyright 2011, Mysema Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mysema.query.mongodb;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.BSONObject;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mysema.query.types.*;

/**
 * Serializes the given Querydsl query to a DBObject query for MongoDB
 *
 * @author laimw
 *
 */
public class MongodbSerializer implements Visitor<Object, Void> {

    public static final MongodbSerializer DEFAULT = new MongodbSerializer();

    public Object handle(Expression<?> expression) {
        return expression.accept(this, null);
    }

    public DBObject toSort(List<OrderSpecifier<?>> orderBys) {
        BasicDBObject sort = new BasicDBObject();
        for (OrderSpecifier<?> orderBy : orderBys) {
            Object key = orderBy.getTarget().accept(this, null);
            sort.append(key.toString(), orderBy.getOrder() == Order.ASC ? 1 : -1);
        }
        return sort;
    }

    @Override
    public Object visit(Constant<?> expr, Void context) {
        if (Enum.class.isAssignableFrom(expr.getType())) {
            return ((Enum<?>)expr.getConstant()).name(); 
        } else {
            return expr.getConstant();            
        }
    }

    @Override
    public Object visit(TemplateExpression<?> expr, Void context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(FactoryExpression<?> expr, Void context) {
        throw new UnsupportedOperationException();
    }

    private String asDBKey(Operation<?> expr, int index) {
        return (String) asDBValue(expr, index);
    }

    private Object asDBValue(Operation<?> expr, int index) {
        return expr.getArg(index).accept(this, null);
    }

    private String regexValue(Operation<?> expr, int index) {
        return Pattern.quote(expr.getArg(index).accept(this, null).toString());
    }

    protected DBObject asDBObject(String key, Object value) {
        return new BasicDBObject(key, value);
    }

    @Override
    public Object visit(Operation<?> expr, Void context) {
        Operator<?> op = expr.getOperator();
        if (op == Ops.EQ_OBJECT || op == Ops.EQ_PRIMITIVE ) {
            return asDBObject(asDBKey(expr, 0), asDBValue(expr, 1));

        }else if (op == Ops.STRING_IS_EMPTY){
            return asDBObject(asDBKey(expr, 0), "");
        }

        else if (op == Ops.AND) {
            BasicDBObject left = (BasicDBObject) handle(expr.getArg(0));
            left.putAll((BSONObject) handle(expr.getArg(1)));
            return left;
        }

        else if (op == Ops.NOT) {
            //Handle the not's child
            BasicDBObject arg = (BasicDBObject) handle(expr.getArg(0));

            //Only support the first key, let's see if there
            //is cases where this will get broken
            String key = arg.keySet().iterator().next();

            Operator<?> subOp = ((Operation<?>) expr.getArg(0)).getOperator();
            if (subOp != Ops.EQ_OBJECT && subOp != Ops.EQ_PRIMITIVE && subOp != Ops.STRING_IS_EMPTY){
                return asDBObject(key, asDBObject("$not", arg.get(key)));
            } else {
                return asDBObject(key, asDBObject("$ne", arg.get(key)));
            }
        }

        else if (op == Ops.OR){
            BasicDBList list = new BasicDBList();
            list.add(handle(expr.getArg(0)));
            list.add(handle(expr.getArg(1)));
            return asDBObject("$or", list);
        }

        else if (op == Ops.NE_OBJECT || op == Ops.NE_PRIMITIVE) {
            return asDBObject(asDBKey(expr, 0), asDBObject("$ne", asDBValue(expr, 1)));
        }

        else if (op == Ops.STARTS_WITH) {
            return asDBObject(asDBKey(expr, 0), 
                    Pattern.compile("^" + regexValue(expr, 1)));
        }

        else if (op == Ops.STARTS_WITH_IC) {
            return asDBObject(asDBKey(expr, 0),
                    Pattern.compile("^" + regexValue(expr, 1), Pattern.CASE_INSENSITIVE));
        }

        else if (op == Ops.ENDS_WITH) {
            return asDBObject(asDBKey(expr, 0), Pattern.compile(regexValue(expr, 1) + "$"));
        }

        else if (op == Ops.ENDS_WITH_IC) {
            return asDBObject(asDBKey(expr, 0),
                    Pattern.compile(regexValue(expr, 1) + "$", Pattern.CASE_INSENSITIVE));
        }

        else if (op == Ops.EQ_IGNORE_CASE) {
            return asDBObject(asDBKey(expr, 0),
                    Pattern.compile("^" + regexValue(expr, 1) + "$", Pattern.CASE_INSENSITIVE));
        }

        else if (op == Ops.STRING_CONTAINS) {
            return asDBObject(asDBKey(expr, 0), Pattern.compile(".*" + regexValue(expr, 1) + ".*"));
        }

        else if (op == Ops.STRING_CONTAINS_IC) {
            return asDBObject(asDBKey(expr, 0),
                    Pattern.compile(".*" + regexValue(expr, 1) + ".*", Pattern.CASE_INSENSITIVE));
        }

        else if (op == Ops.MATCHES) {
            return asDBObject(asDBKey(expr, 0), Pattern.compile(asDBValue(expr, 1).toString()));
        }

        else if (op == Ops.MATCHES_IC) {
            return asDBObject(asDBKey(expr, 0), Pattern.compile(asDBValue(expr, 1).toString(), Pattern.CASE_INSENSITIVE));
        }
            
        else if (op == Ops.BETWEEN) {
            BasicDBObject value = new BasicDBObject("$gte", asDBValue(expr, 1));
            value.append("$lte", asDBValue(expr, 2));
            return asDBObject(asDBKey(expr, 0), value);
        }

        else if (op == Ops.IN) {
            int constIndex = 0;
            int exprIndex = 1;            
            if (expr.getArg(1) instanceof Constant<?>) {
                constIndex = 1;
                exprIndex = 0;
            }   
            if (Collection.class.isAssignableFrom(expr.getArg(constIndex).getType())) {
                Collection<?> values = (Collection<?>) ((Constant<?>) expr.getArg(constIndex)).getConstant();
                return asDBObject(asDBKey(expr, exprIndex), asDBObject("$in", values.toArray()));    
            } else {
                return asDBObject(asDBKey(expr, exprIndex), asDBValue(expr, constIndex));
            }
            
        }
       
        
        else if (op == Ops.LT || op == Ops.BEFORE) {
            return asDBObject(asDBKey(expr, 0), asDBObject("$lt", asDBValue(expr, 1)));
        }

        else if (op == Ops.GT || op == Ops.AFTER) {
            return asDBObject(asDBKey(expr, 0), asDBObject("$gt", asDBValue(expr, 1)));
        }

        else if (op == Ops.LOE || op == Ops.BOE) {
            return asDBObject(asDBKey(expr, 0), asDBObject("$lte", asDBValue(expr, 1)));
        }

        else if (op == Ops.GOE || op == Ops.AOE) {
            return asDBObject(asDBKey(expr, 0), asDBObject("$gte", asDBValue(expr, 1)));
        }
        
        else if (op == Ops.IS_NULL){
            return asDBObject(asDBKey(expr, 0), asDBObject("$exists", false));
        }
        
        else if (op == Ops.IS_NOT_NULL){
            return asDBObject(asDBKey(expr, 0), asDBObject("$exists", true));
        }
        
        else if (op == MongodbOps.NEAR) {
            return asDBObject(asDBKey(expr, 0), asDBObject("$near", asDBValue(expr, 1)));
        }

        
        throw new UnsupportedOperationException("Illegal operation " + expr);
    }

    @Override
    public Object visit(Path<?> expr, Void context) {
        PathMetadata<?> metadata = expr.getMetadata();
        if (metadata.getParent() != null){
            if (metadata.getPathType() == PathType.COLLECTION_ANY){
                return visit(metadata.getParent(), context);
            }else if (metadata.getParent().getMetadata().getPathType() != PathType.VARIABLE){
                String rv = getKeyForPath(expr, metadata);
                return visit(metadata.getParent(), context) + "." + rv;
            }
        } 
        return getKeyForPath(expr, metadata);
    }

    protected String getKeyForPath(Path<?> expr, PathMetadata<?> metadata) {
        if (expr.getType().equals(ObjectId.class)){
            return "_id";
        } else {
            return metadata.getExpression().toString();    
        }        
    }

    @Override
    public Object visit(SubQueryExpression<?> expr, Void context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(ParamExpression<?> expr, Void context) {
        throw new UnsupportedOperationException();
    }

}