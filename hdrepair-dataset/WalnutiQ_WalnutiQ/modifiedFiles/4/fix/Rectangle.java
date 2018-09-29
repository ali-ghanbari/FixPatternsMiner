package model.util;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * IMPORTANT TO UNDERSTAND: the most top left corner of the plane this
 * rectangle can be on is (0, 0). Additionally, the positive X-axis is the
 * horizontal axis pointing right and the positive Y-axis is the vertical axis
 * pointing south.
 *
 * @author Quinn Liu (quinnliu@vt.edu)
 * @version July 12, 2014
 */
public class Rectangle {
    private Point topLeftCorner;
    private Point bottomRightCorner;

    public Rectangle(Point topLeftCorner, Point bottomRightCorner) {
        int TLx = (int) topLeftCorner.getX();
        int TLy = (int) topLeftCorner.getY();
        int BRx = (int) bottomRightCorner.getX();
        int BRy = (int) bottomRightCorner.getY();

        if (TLx < 0 || TLy < 0 || BRx < 0 || BRy < 0) {
            throw new IllegalArgumentException("In class Rectangle constructor method the input point" +
                    " must be >= 0");
        } else if (TLx >= BRx || TLy >= BRy) {
            throw new IllegalArgumentException("In class Rectangle constructor method the parameter " +
                    "topLeftCorner is not to the top left of the parameter bottomRightCorner");
        }

        this.topLeftCorner = topLeftCorner;
        this.bottomRightCorner = bottomRightCorner;
    }

    public int getWidth() {
        return (int) (this.bottomRightCorner.getX() - this.topLeftCorner.getX());
    }

    public int getHeight() {
        return (int) (this.bottomRightCorner.getY() - this.topLeftCorner.getY());
    }

    public Point getTopLeftCorner() {
        return this.topLeftCorner;
    }

    public void setTopLeftCorner(Point topLeftCorner) {
        this.setNewTopLeftCornerIfValid(topLeftCorner);
    }

    public Point getBottomRightCorner() {
        return this.bottomRightCorner;
    }

    public void setBottomRightCorner(Point bottomRightCorner) {
        this.setNewBottomRightCornerIfValid(bottomRightCorner);
    }

    void setNewTopLeftCornerIfValid(Point newTopLeftCorner) {
        int TLx = (int) newTopLeftCorner.getX();
        int TLy = (int) newTopLeftCorner.getY();
        int BRx = (int) this.bottomRightCorner.getX();
        int BRy = (int) this.bottomRightCorner.getY();
        if (TLx < 0 || TLy < 0) {
            throw new IllegalArgumentException("In class Rectangle isNewTopLeftCornerValid method the input point" +
                    " must be >= 0");
        } else if (TLx >= BRx || TLy >= BRy) {
            throw new IllegalArgumentException("In class Rectangle isNewTopLeftCornerValid method the input point" +
                    " is not to the top left of the current bottom right point");
        } else {
            this.topLeftCorner.setLocation(newTopLeftCorner);
        }
    }

    void setNewBottomRightCornerIfValid(Point newBottomRightCorner) {
        int TLx = (int) this.topLeftCorner.getX();
        int TLy = (int) this.topLeftCorner.getY();
        int BRx = (int) newBottomRightCorner.getX();
        int BRy = (int) newBottomRightCorner.getY();

        if (BRx <= 0 || BRy <= 0) {
            throw new IllegalArgumentException("In class Rectangle isNewBottomRightCornerValid method the input point" +
                    " must be >= 0");
        }
        if (TLx >= BRx || TLy >= BRy) {
            throw new IllegalArgumentException("In class Rectangle isNewBottomRightCornerValid method the input point" +
                    " is not to the bottom right of the current top left point");
        } else {
            this.bottomRightCorner.setLocation(newBottomRightCorner);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Rectangle rectangle = (Rectangle) o;

        if (bottomRightCorner != null ? !bottomRightCorner.equals(rectangle.bottomRightCorner) : rectangle.bottomRightCorner != null)
            return false;
        if (topLeftCorner != null ? !topLeftCorner.equals(rectangle.topLeftCorner) : rectangle.topLeftCorner != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = topLeftCorner != null ? topLeftCorner.hashCode() : 0;
        result = 31 * result + (bottomRightCorner != null ? bottomRightCorner.hashCode() : 0);
        return result;
    }
}
