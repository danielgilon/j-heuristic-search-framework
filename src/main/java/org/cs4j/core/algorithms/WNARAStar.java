package org.cs4j.core.algorithms;

import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by user on 29/11/2015.
 *
 * The algorithm is equivalent to the standard WA* suboptimal search, but, this algorithms runs NR by default and
 * only if the predicted suboptimality of the solution is higher than the suboptimality bound, the algorithm runs AR
 * and stops, otherwise, the solution of NR is returned
 */
public class WNARAStar extends WRAStar {

    private static final Map<String, Class> WNARAStarPossibleParameters;

    // Declare the parameters that can be tuned before running the search
    static {
        WNARAStarPossibleParameters = new HashMap<>();
        WNARAStar.WNARAStarPossibleParameters.put("weight", Double.class);
    }

    /**
     * A default constructor of the class
     */
    public WNARAStar() {
        super(1.0);
    }

    @Override
    public String getName() {
        return "wnarastar";
    }

    @Override
    public Map<String, Class> getPossibleParameters() {
        return WNARAStar.WNARAStarPossibleParameters;
    }

    @Override
    public void setAdditionalParameter(String parameterName, String value) {
        switch (parameterName) {
            case "weight": {
                this.weight = Double.parseDouble(value);
                if (this.weight < 1.0d) {
                    System.out.println("[ERROR] The weight must be >= 1.0");
                    throw new IllegalArgumentException();
                } else if (this.weight == 1.0d) {
                    System.out.println("[WARNING] Weight of 1.0 is equivalent to A*");
                }
                break;
            }
            default: {
                throw new NotImplementedException();
            }
        }
    }

    @Override
    public SearchResult search(SearchDomain domain) {
        this.domain = domain;
        // TODO ...
        double maxPreviousCost = Double.MAX_VALUE;
        // Initialize all the data structures required for the search
        this._initDataStructures();
        // Let's instantiate the initial state
        SearchDomain.State currentState = domain.initialState();
        // Create a graph node from this state
        Node initNode = new Node(currentState);

        // And add it to the frontier
        this.open.add(initNode);
        this.cleanup.add(initNode);
        // The nodes are ordered in the closed list by their packed values
        this.closed.put(initNode.packed, initNode);

        double maxCost = Double.MAX_VALUE;
        System.out.println("[INFO] Performing first search with NR");
        SearchResultImpl nrResult = new SearchResultImpl();
        // Run iteration 0 : NR + recording of the minimum F value in the OPEN list
        this._search(domain, 0, maxPreviousCost, nrResult);
        // If now solution - run with AR
        if (nrResult.hasSolution()) {
            double bestF = this.cleanup.peek().rF;
            // Get current solution
            SearchResult.Solution currentSolution = nrResult.getSolutions().get(0);
            // Update the maximum cost (if the search continues with AR, all the nodes with g+h > maxCost will be pruned)
            maxCost = currentSolution.getCost();
            double suboptimalBoundSup = maxCost / bestF;
            System.out.println("[INFO] Running with NR ended: (BestF: " + bestF +
                    ", Approximated suboptimality bound: " + suboptimalBoundSup + ")");
            if (suboptimalBoundSup <= this.weight) {
                System.out.println("[INFO] Bound is sufficient (required: " + this.weight + ", got: " +
                        suboptimalBoundSup + ")");
                return nrResult;
            }
            System.out.println("[INFO] Insufficient bound (" + suboptimalBoundSup + " > " +
                    this.weight + "), expanded: " + nrResult.getExpanded() + " - running AR");
        } else {
            System.out.println("[WARNING] Running with NR doesn't reveal to solution - trying with AR");
        }
        // Otherwise, fallback to AR
        SearchAlgorithm alg = new WAStar();
        alg.setAdditionalParameter("weight", this.weight + "");
        alg.setAdditionalParameter("reopen", true + "");
        alg.setAdditionalParameter("max-cost", maxCost + "");
        SearchResult arResult = alg.search(domain);
        arResult.increase(nrResult);
        return arResult;
    }
}
