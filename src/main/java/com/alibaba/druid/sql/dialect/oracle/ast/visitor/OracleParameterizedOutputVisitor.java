package com.alibaba.druid.sql.dialect.oracle.ast.visitor;

import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLInListExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.expr.SQLLiteralExpr;
import com.alibaba.druid.sql.ast.expr.SQLNCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLNumberExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;

public class OracleParameterizedOutputVisitor extends OracleOutputVisitor {

    public OracleParameterizedOutputVisitor(Appendable appender){
        super(appender);
    }

    public void println() {
        print(' ');
    }

    public boolean visit(SQLInListExpr x) {
        x.getExpr().accept(this);

        if (x.isNot()) {
            print(" NOT IN (##)");
        } else {
            print(" IN (##)");
        }

        return false;
    }
    
    public boolean visit(SQLBinaryOpExpr x) {
        if (x.getLeft() instanceof SQLLiteralExpr && x.getRight() instanceof SQLLiteralExpr) {
            print(x.getLeft().toString());
            print(' ');
            print(x.getOperator().name);
            print(' ');
            print(x.getRight().toString());
            return false;
        }

        x = merge(x);

        return super.visit(x);
    }

    public SQLBinaryOpExpr merge(SQLBinaryOpExpr x) {
        if (x.getRight() instanceof SQLLiteralExpr) {
            x = new SQLBinaryOpExpr(x.getLeft(), x.getOperator(), new SQLVariantRefExpr("?"));
        }

        if (x.getLeft() instanceof SQLLiteralExpr) {
            x = new SQLBinaryOpExpr(new SQLVariantRefExpr("?"), x.getOperator(), x.getRight());
        }

        if (x.getRight() instanceof SQLBinaryOpExpr) {
            x = new SQLBinaryOpExpr(x.getLeft(), x.getOperator(), merge((SQLBinaryOpExpr) x.getRight()));
        }

        if (x.getLeft() instanceof SQLBinaryOpExpr) {
            x = new SQLBinaryOpExpr((SQLBinaryOpExpr) x.getLeft(), x.getOperator(), x.getRight());
        }

        // ID = ? OR ID = ? => ID = ?
        if (x.getOperator() == SQLBinaryOperator.BooleanOr) {
            if ((x.getLeft() instanceof SQLBinaryOpExpr) && (x.getRight() instanceof SQLBinaryOpExpr)) {
                SQLBinaryOpExpr left = (SQLBinaryOpExpr) x.getLeft();
                SQLBinaryOpExpr right = (SQLBinaryOpExpr) x.getRight();

                if (left.getOperator() == SQLBinaryOperator.Equality && right.getOperator() == SQLBinaryOperator.Equality) {
                    if ((left.getLeft() instanceof SQLIdentifierExpr) && (right.getLeft() instanceof SQLIdentifierExpr)) {
                        String leftColumn = left.getLeft().toString();
                        String rightColumn = right.getLeft().toString();

                        if (leftColumn.equals(rightColumn)) {
                            if ((left.getRight() instanceof SQLVariantRefExpr) && (right.getRight() instanceof SQLVariantRefExpr)) {
                                return left; // merge
                            }
                        }
                    }
                }
            }
        }

        return x;
    }
    
    public boolean visit(SQLIntegerExpr x) {
        print('?');
        return false;
    }

    public boolean visit(SQLNumberExpr x) {
        print('?');
        return false;
    }

    public boolean visit(SQLCharExpr x) {
        print('?');
        return false;
    }

    public boolean visit(SQLNCharExpr x) {
        print('?');
        return false;
    }

}