package ch.obermuhlner.math.big.matrix.internal.sparse;

import ch.obermuhlner.math.big.BigDecimalMath;
import ch.obermuhlner.math.big.matrix.BigMatrix;
import ch.obermuhlner.math.big.matrix.ImmutableBigMatrix;
import ch.obermuhlner.math.big.matrix.internal.AbstractBigMatrix;
import ch.obermuhlner.math.big.matrix.internal.MatrixUtils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static java.math.BigDecimal.valueOf;

public abstract class AbstractSparseBigMatrix extends AbstractBigMatrix {
    protected final int rows;
    protected final int columns;
    protected final Map<Integer, BigDecimal> data = new HashMap<>();
    protected BigDecimal defaultValue = BigDecimal.ZERO;

    public AbstractSparseBigMatrix(int rows, int columns) {
        MatrixUtils.checkRows(rows);
        MatrixUtils.checkColumns(columns);

        this.rows = rows;
        this.columns = columns;
    }

    public AbstractSparseBigMatrix(int rows, int columns, BigDecimal... values) {
        this(rows, columns);

        for (int i = 0; i < values.length; i++) {
            internalSet(i, values[i]);
        }
    }

    public AbstractSparseBigMatrix(int rows, int columns, BiFunction<Integer, Integer, BigDecimal> valueFunction) {
        this(rows, columns);

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                internalSet(row, column, valueFunction.apply(row, column));
            }
        }
    }

    @Override
    public int rows() {
        return rows;
    }

    @Override
    public int columns() {
        return columns;
    }

    @Override
    public BigDecimal get(int row, int column) {
        MatrixUtils.checkRow(this, row);
        MatrixUtils.checkColumn(this, column);

        int index = row*columns + column;
        return internalGet(index);
    }

    public BigDecimal getDefaultValue() {
        return defaultValue;
    }

    BigDecimal internalGet(int index) {
        return data.getOrDefault(index, defaultValue);
    }

    public int sparseFilledSize() {
        return data.size();
    }

    public int sparseEmptySize() {
        return size() - sparseFilledSize();
    }

    public double sparseEmptyRatio() {
        if (size() == 0) {
            return 0.0;
        }

        return (double)sparseEmptySize() / size();
    }

    @Override
    public ImmutableBigMatrix add(BigMatrix other, MathContext mathContext) {
        if (other instanceof AbstractSparseBigMatrix) {
            return addSparse((AbstractSparseBigMatrix) other, mathContext);
        }
        return super.add(other, mathContext);
    }

    private ImmutableBigMatrix addSparse(AbstractSparseBigMatrix other, MathContext mathContext) {
        MatrixUtils.checkSameSize(this, other);

        SparseImmutableBigMatrix m = new SparseImmutableBigMatrix(rows, columns);
        m.defaultValue = MatrixUtils.subtract(defaultValue, other.defaultValue, mathContext);

        Set<Integer> mergedIndexes = new HashSet<>(data.keySet());
        mergedIndexes.addAll(other.data.keySet());

        for (int index : mergedIndexes) {
            m.internalSet(index, MatrixUtils.add(internalGet(index), other.internalGet(index), mathContext));
        }

        return m.asImmutableMatrix();
    }

    @Override
    public ImmutableBigMatrix subtract(BigMatrix other, MathContext mathContext) {
        if (other instanceof AbstractSparseBigMatrix) {
            return subtractSparse((AbstractSparseBigMatrix) other, mathContext);
        }
        return super.subtract(other, mathContext);
    }

    private ImmutableBigMatrix subtractSparse(AbstractSparseBigMatrix other, MathContext mathContext) {
        MatrixUtils.checkSameSize(this, other);

        SparseImmutableBigMatrix m = new SparseImmutableBigMatrix(rows, columns);
        m.defaultValue = MatrixUtils.subtract(defaultValue, other.defaultValue, mathContext);

        Set<Integer> mergedIndexes = new HashSet<>(data.keySet());
        mergedIndexes.addAll(other.data.keySet());

        for (int index : mergedIndexes) {
            m.internalSet(index, MatrixUtils.subtract(internalGet(index), other.internalGet(index), mathContext));
        }

        return m.asImmutableMatrix();
    }

    @Override
    public ImmutableBigMatrix multiply(BigDecimal value, MathContext mathContext) {
        return multiplySparse(value, mathContext);
    }

    private ImmutableBigMatrix multiplySparse(BigDecimal value, MathContext mathContext) {
        SparseImmutableBigMatrix m = new SparseImmutableBigMatrix(rows, columns);
        m.defaultValue = MatrixUtils.multiply(defaultValue, value, mathContext);

        for (Map.Entry<Integer, BigDecimal> entry : data.entrySet()) {
            int index = entry.getKey();
            BigDecimal sparseValue = entry.getValue();
            m.internalSet(index, MatrixUtils.multiply(sparseValue, value, mathContext));
        }

        return m.asImmutableMatrix();
    }

    @Override
    public BigDecimal sum(MathContext mathContext) {
        BigDecimal result = MatrixUtils.multiply(valueOf(size() - sparseFilledSize()), defaultValue, mathContext);

        for (BigDecimal value : data.values()) {
            result = MatrixUtils.add(result, value, mathContext);
        }

        return result;
    }

    @Override
    public BigDecimal product(MathContext mathContext) {
        if (mathContext == null) {
            return super.product(null);
        }

        BigDecimal result = BigDecimalMath.pow(defaultValue,size() - sparseFilledSize(), mathContext);

        for (BigDecimal value : data.values()) {
            result = MatrixUtils.multiply(result, value, mathContext);
        }

        return result;
    }

    protected void internalSet(int row, int column, BigDecimal value) {
        MatrixUtils.checkRow(this, row);
        MatrixUtils.checkColumn(this, column);

        int index = row*columns + column;
        internalSet(index, value);
    }

    protected void internalSet(int index, BigDecimal value) {
        if (value.compareTo(defaultValue) == 0) {
            data.remove(index);
        } else {
            data.put(index, value);
        }
    }
}
