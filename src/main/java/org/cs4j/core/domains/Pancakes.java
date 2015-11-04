package org.cs4j.core.domains;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.cs4j.core.SearchDomain;

/**
 * The pancake problem is a famous search problem where the objective is to sort a sequence of
 * objects (pancakes) through a minimal number of prefix reversals (flips).
 */
public class Pancakes implements SearchDomain {

    private COST_FUNCTION costFunction;
    // The possible cost functions
    public enum COST_FUNCTION {UNIT, HEAVY};
    private int numCakes = 0;
    // The initial given state
    private int init[];
    private Operator[] possibleOperators;

    /**
     * The constructor reads an instance of Pancakes problem from the specified input stream
     *
     * @param stream The input stream to read the problem from
     * @param costFunction The computeCost function to use
     */
    public Pancakes(InputStream stream, COST_FUNCTION costFunction) {
        this.costFunction = costFunction;

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            // Read the number of cakes
            String line = reader.readLine();
            this.numCakes = Integer.parseInt(line);
            this.init = new int[this.numCakes];
            this.possibleOperators = new Operator[this.numCakes];
            // Read the cakes
            line = reader.readLine();
            String[] cakes = line.split(" ");
            for (int i = 0; i < cakes.length; ++i) {
                this.init[i] = Integer.parseInt(cakes[i]);
            }
            // Finally, initialize the operators (according to the updated position of the pancake)
            for (int i = 0; i < this.numCakes; ++i) {
                this.possibleOperators[i] = new PancakeOperator(i + 1);
            }
        } catch(IOException e) {
            Utils.fatal("Error reading input file");
        }
    }

    /**
     * The constructor reads an instance of Pancakes problem from the specified input stream.
     * In this case, the UNIT cost function is used.
     *
     * @param stream the input stream to read
     */
    public Pancakes(InputStream stream) {
        this(stream, COST_FUNCTION.UNIT);
    }

    /**
     * Check whether there is a gap between the cakes n and n+1? (whether they are following)
     *
     * @param cakes The pancakes array to calculate the data from
     * @param n The pancake to check
     *
     * @return Whether there is a gap between the pancakes at positions n and n+1
     */
    private boolean _hasGap(int cakes[], int n) {
        // A special case for the last cake: simply check if its position is true
        if (n == (this.numCakes - 1)) {
            return cakes[this.numCakes - 1] != (this.numCakes - 1);
        }
        // Whether the difference is different than 1
        return Math.abs(cakes[n] - cakes[n+1]) != 1;
    }

    /**
     * The function calculates the number of 'gaps' between the cakes which forms the heuristic
     * function: Since, at least a single operation should be performed on each gap - in order to
     * remove it
     *
     * @param cakes The pancakes array
     * @param costFunction The cost function to apply
     *
     * @return The calculated gaps number, which allows to form the heuristic function
     */
    private int _countGaps(int cakes[], COST_FUNCTION costFunction) {
        int gapsCount = 0;
        for (int i = 0; i < this.numCakes; ++i) {
            if (this._hasGap(cakes, i)) {
                switch (costFunction) {
                    case UNIT:
                        ++gapsCount;
                        break;
                    case HEAVY:
                        int a = cakes[i];
                        int b = (i != this.numCakes - 1) ? cakes[i+1] : Integer.MAX_VALUE;
                        // Each gap costs the 1+the minimal cake
                        gapsCount += (1 + Math.min(a, b));
                        break;
                }
            }
        }
        return gapsCount;
    }

    @Override
    public PancakeState initialState() {
        PancakeState s = new PancakeState(this.numCakes);
        System.arraycopy(this.init, 0, s.cakes, 0, numCakes);
        s.h = this._countGaps(s.cakes, this.costFunction);
        s.d = this._countGaps(s.cakes, COST_FUNCTION.UNIT);
        return s;
    }

    @Override
    public boolean isGoal(State state) {
        return ((PancakeState)state).d == 0;
    }

    @Override
    // Each operator can be applied on any state
    public int getNumOperators(State state) {
        return this.numCakes - 1;
    }

    @Override
    public Operator getOperator(State state, int nth) {
        return this.possibleOperators[nth];
    }

    @Override
    public State applyOperator(State state, Operator op) {
        PancakeState pacakeState = (PancakeState)copy(state);
        int pancakeOperator = ((PancakeOperator)op).value;
        // Flip the top of the stack
        pacakeState.flipTopStackPortion(pancakeOperator);
        pacakeState.h = this._countGaps(pacakeState.cakes, this.costFunction);
        pacakeState.d = this._countGaps(pacakeState.cakes, COST_FUNCTION.UNIT);
        return pacakeState;
    }

    @Override
    public State copy(State state) {
        return new PancakeState((PancakeState)state);
    }

    /**
     * Calculates the cost of the given operator according to the given cost function
     *
     * @param op The operator whose cost should be calculated
     * @return The calculated cost of the operator
     */
    private double computeCost(int op) {
        double value = 1.0;
        switch (this.costFunction) {
            case HEAVY:
                value = 1 + op;
                break;
            case UNIT:
            default:
                break;
        }
        return value;
    }

    @Override
    public long pack(State s) {
        PancakeState ps = (PancakeState)s;
        long word = 0;
        // We need at least 60 bits (for 15 pancakes)
        for (int i = 0; i < this.numCakes; ++i) {
            word = (word << 4) | ps.cakes[i];
        }
        return word;
    }

    @Override
    public State unpack(long word) {
        PancakeState state = new PancakeState(this.numCakes);
        for (int i = numCakes - 1; i >= 0; --i) {
            int t = (int) word & 0xF;
            word >>= 4;
            state.cakes[i] = t;
        }
        state.h = this._countGaps(state.cakes, this.costFunction);
        state.d = this._countGaps(state.cakes, COST_FUNCTION.UNIT);
        return state;
    }

    /**
     * Pancake stat class
     */
    public final class PancakeState implements State {
        public int numCakes;
        public int[] cakes;
        public double h;
        public double d;
        private PancakeState parent = null;

        /**
         * A default constructor of the class
         */
        private PancakeState() { }

        /**
         * A standard constructor
         *
         * @param numCakes The number of cakes in the state
         */
        public PancakeState(int numCakes) {
            this.numCakes = numCakes;
            this.cakes = new int[numCakes];
        }

        /**
         * A copy constructor
         *
         * @param pancake Another pancake state to copy
         */
        public PancakeState(PancakeState pancake) {
            this.numCakes = pancake.numCakes;
            this.cakes = new int[numCakes];
            this.h = pancake.h;
            this.d = pancake.d;
            System.arraycopy(pancake.cakes, 0, cakes, 0, pancake.cakes.length);
        }

        /**
         * Pancake states are compared by comparing all the pancakes in the state
         *
         * @param object The object to compare to the current state
         *
         * @return Whether the states are equal
         */
        @Override
        public boolean equals(Object object) {
            try {
                PancakeState pancakeState = (PancakeState)object;
                return Arrays.equals(this.cakes, pancakeState.cakes);
            } catch (ClassCastException e) {
                return false;
            }
        }

        @Override
        public double getH() {
            return h;
        }

        @Override
        public double getD() {
            return d;
        }

        @Override
        public State getParent() {
            return this.parent;
        }

        /**
         * Lift a top portion of the stack and reverse it
         *
         * @param op The operator for the operation (represented by an integer number)
         */
        private void flipTopStackPortion(int op) {
            // Assert the operator is valid
            assert (op > 0);
            assert (op < this.numCakes);
            // Flip the first half (and the second will be flipped automatically)
            for (int n = 0; n <= op / 2; ++n) {
                int tmp = this.cakes[n];
                this.cakes[n] = this.cakes[op - n];
                this.cakes[op - n] = tmp;
            }
        }

        @Override
        public String dumpState() {
            StringBuilder sb = new StringBuilder();
            sb.append("********************************\n");
            // h
            sb.append("h: ");
            sb.append(this.h);
            sb.append("\n");
            // d
            sb.append("d: ");
            sb.append(this.d);
            sb.append("\n");

            for (int i = 0; i < this.cakes.length; ++i) {
                sb.append(cakes[i]);
                sb.append(" ");
            }
            sb.append("\n");
            sb.append("********************************\n\n");
            return sb.toString();
        }

    }

    /**
     * An operator class which represents a single operator of the Pancake domain
     */
    private final class PancakeOperator implements Operator {

        private int value;

        private PancakeOperator(int value) {
            this.value = value;
        }

        @Override
        public double getCost(State state) {
            PancakeState ps = (PancakeState)state;
            return computeCost(ps.cakes[value]);
        }

        @Override
        public Operator reverse(State state) {
            // In the Pancakes domain, reversing an operation can be performed easily by applying
            // the same operator on the state
            return this;
        }

    }

    public String dumpStatesCollection(State[] states) {
        return null;
    }
}