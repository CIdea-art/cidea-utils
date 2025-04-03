package cn.cidea.core.utils.math;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 分数，使用方式参照{@link BigDecimal}
 * 为了保证{@link BigDecimal}在连续乘除中，精度不丢失或尽量少丢
 * 例：1 / 3 * 3 -> (1 * 3) / 3
 * @author: CIdea
 */
public class Fraction implements Serializable {

    private final BigDecimal up;
    private final BigDecimal down;

    public final static Fraction ONE = new Fraction(BigDecimal.ONE);

    public Fraction(BigDecimal up) {
        this(up, BigDecimal.ONE);
    }

    public Fraction(BigDecimal up, BigDecimal down) {
        this.up = up;
        this.down = down;
    }

    public Fraction multiply(Fraction weight) {
        return new Fraction(weight.up.multiply(up), weight.down.multiply(down));
    }

    public Fraction divide(Fraction weight) {
        return new Fraction(up.multiply(weight.down), down.multiply(weight.up));
    }

    public Fraction reverse(){
        return new Fraction(down, up);
    }

    public BigDecimal val() {
        return up.divide(down);
    }

    public BigDecimal val(int scale, RoundingMode roundingMode) {
        if(BigDecimal.ONE.equals(down)){
            return up;
        }
        if(up.equals(down)){
            return BigDecimal.ONE;
        }
        return up.divide(down, scale, roundingMode);
    }

    @Override
    public String toString() {
        if(BigDecimal.ONE.equals(down)){
            return up.toString();
        }
        return up + "/" + down;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Fraction)){
            return false;
        }
        return equals((Fraction) obj);
    }
    public boolean equals(Fraction other) {
        return up.multiply(other.down).compareTo(down.multiply(other.up)) == 0;
    }

    public boolean gtOne(){
        return up.compareTo(down) > 0;
    }

    public boolean eqOne(){
        return up.compareTo(down) == 0;
    }

    public boolean gt(Fraction other) {
        return up.multiply(other.down).compareTo(down.multiply(other.up)) > 0;
    }

    public boolean lt(Fraction other) {
        return up.multiply(other.down).compareTo(down.multiply(other.up)) < 0;
    }
}
