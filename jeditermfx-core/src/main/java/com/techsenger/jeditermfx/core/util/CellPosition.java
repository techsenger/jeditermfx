package com.techsenger.jeditermfx.core.util;

import java.util.Objects;

public class CellPosition {

    private final int x;

    private final int y;

    /**
     * @param x one-based column
     * @param y one-based row
     */
    public CellPosition(int x, int y) {
        this.x = x;
        this.y = y;
        if (!(x >= 1)) {
            throw new IllegalArgumentException("Positive column is expected, got " + x);
        }
        if (!(y >= 1)) {
            throw new IllegalArgumentException("Positive row is expected, got " + y);
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!Objects.equals(this.getClass(), other != null ? other.getClass() : null)) {
            return false;
        } else {
            CellPosition otherPos = (CellPosition) other;
            return this.x == otherPos.x && this.y == otherPos.y;
        }
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public String toString() {
        return "column=" + x + ", row=" + y;
    }

}
