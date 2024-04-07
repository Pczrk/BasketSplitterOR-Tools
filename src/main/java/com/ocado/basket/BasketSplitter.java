package com.ocado.basket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Class responsible for solving basket splitting problem.
 */
public class BasketSplitter {
    /**
     * Map to store possible delivery types for each item.
     */
    private HashMap<String, HashSet<String>> itemPossibleDeliveries;
    /**
     * List to store unique delivery types.
     */
    private final List<String> _deliveries = new ArrayList<>();

    /**
     * Constructor to initialize the BasketSplitter with configuration file path and fill {@link #_deliveries} list.
     * @param absolutePathToConfigFile Absolute path to the configuration file.
     * @throws IOException If an I/O error occurs while reading configuration file.
     */
    public BasketSplitter(String absolutePathToConfigFile) throws IOException {
        Loader.loadNativeLibraries();
        readConfig(absolutePathToConfigFile);
        fillHelperField();
    }

    /**
     * Splits list of items to minimum amount of delivery times,
     * maximizing number of items in delivery type with most items.
     * @param items List of items to split.
     * @return Map with delivery types as keys and list of items as values.
     * @throws SolutionNotFoundException If no solution was found
     */
    public Map<String, List<String>> split(List<String> items) throws SolutionNotFoundException {
        int numberOfDeliveryTypes = firstSolution(items);
        var deliveryTypesForGivenItems = getSubsetOfDeliveryTypes(items);

        Solution solution = null;
        for (int j=0;j< _deliveries.size();j++){
            if (deliveryTypesForGivenItems.contains(_deliveries.get(j))) {
                var s = findSolution(numberOfDeliveryTypes, j, items);
                solution = solution == null || s.maxItems > solution.maxItems ? s : solution;
            }
        }

        if (solution == null)
            throw new SolutionNotFoundException("No solution was found");

        return solution.deliveryToListOfItems;
    }

    /**
     * Reads the configuration file and fill {@link #itemPossibleDeliveries} hashmap.
     * @param path Absolute path to the configuration file.
     * @throws IOException If an I/O error occurs while reading configuration file.
     */
    private void readConfig(String path) throws IOException {
        String jsonString = new String(Files.readAllBytes(Paths.get(path)));
        itemPossibleDeliveries = new ObjectMapper().readValue(jsonString, new TypeReference<>() {});
    }

    /**
     * Reads the file with items.
     * @param path Absolute path to the file with items.
     * @return List of items.
     * @throws IOException If an I/O error occurs while reading items file.
     */
    public List<String> readItems(String path) throws IOException {
        String jsonString = new String(Files.readAllBytes(Paths.get(path)));
        return new ObjectMapper().readValue(jsonString, new TypeReference<>() {});
    }

    /**
     * Helper method that fills {@link #_deliveries list}
     */
    private void fillHelperField(){
        for (var deliveries:itemPossibleDeliveries.values())
            for (var delivery:deliveries)
                if (!_deliveries.contains(delivery))
                    _deliveries.add(delivery);
    }

    /**
     * Helper method that returns set of all delivery types for given items
     * @param items List of items.
     * @return Subset of delivery types.
     */
    private Set<String> getSubsetOfDeliveryTypes(List<String> items){
        var result = new HashSet<String>();
        for (var item:items){
            result.addAll(itemPossibleDeliveries.get(item));
        }
        return result;
    }
    /**
     * Creates solver, variables and basic constraints that are used to calculate solution.
     * @param items List of items to split.
     * @return Object of class SolverWithVariables that contains solver and its variables.
     */
    private SolverWithVariables baseSolverWithVariables(List<String> items){
        int n = items.size(), m = _deliveries.size();
        MPSolver solver = new MPSolver("base_problem",MPSolver.OptimizationProblemType.BOP_INTEGER_PROGRAMMING);

        var itemVariables = new MPVariable[n][m];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                itemVariables[i][j] = solver.makeBoolVar(String.format("item_%d_%d", i, j));

        var deliveryVariables = new MPVariable[m];
        for (int j = 0; j < m; j++)
            deliveryVariables[j] = solver.makeBoolVar(String.format("delivery_%d", j));

        for (int i = 0; i < n; i++) {
            var constraint = solver.makeConstraint(1, 1, String.format("sum_item_%d_deliveries", i));
            for (int j = 0; j < m; j++) {
                constraint.setCoefficient(itemVariables[i][j], 1);
            }
        }

        for (int j = 0; j < m; j++) {
            var constraint = solver.makeConstraint(Double.NEGATIVE_INFINITY, 0, String.format("delivery_%d_present", j));
            constraint.setCoefficient(deliveryVariables[j],-1*(n+1));
            for (int i = 0; i < n; i++)
                constraint.setCoefficient(itemVariables[i][j], 1);
        }

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (!itemPossibleDeliveries.get(items.get(i)).contains(_deliveries.get(j))) {
                    var constraint = solver.makeConstraint(0, 0, String.format("item_%d_not_delivered_by_%d", i, j));
                    constraint.setCoefficient(itemVariables[i][j], 1);
                }
            }
        }

        return new SolverWithVariables(itemVariables,deliveryVariables,solver);
    }

    /**
     * Finds value of minimal delivery types.
     * Due to model and integer programming specifics it might not find optimal solution,
     * but always finds an optimal amount of delivery types.
     * @param items List of items to split.
     * @return Optimal amount of delivery types.
     * @throws SolutionNotFoundException If solution wasn't found.
     */
    private int firstSolution(List<String> items) throws SolutionNotFoundException {
        int m = _deliveries.size();
        var solverWithVariables = baseSolverWithVariables(items);
        var solver = solverWithVariables.solver;
        var deliveryVariables = solverWithVariables.deliveryVariables;

        MPObjective objective = solver.objective();
        for (int j = 0; j < m; j++)
            objective.setCoefficient(deliveryVariables[j],1.0);
        objective.setMinimization();

        var resultStatus = solver.solve();
        if (resultStatus != MPSolver.ResultStatus.OPTIMAL)
            throw new SolutionNotFoundException("Solution was not found");

        return (int)solver.objective().value();
    }

    /**
     * Finds solution for optimal number of delivery types, index of delivery type that will be maximized and list of items.
     * @param deliveryNumber Optimal number of delivery types.
     * @param deliveryIndex Index of delivery type that will be maximized.
     * @param items List of problem items.
     * @return Object of Solution class
     */
    private Solution findSolution(Integer deliveryNumber, Integer deliveryIndex, List<String> items){
        int n = items.size(), m = _deliveries.size();
        SolverWithVariables solverWithVariables = baseSolverWithVariables(items);
        var solver = solverWithVariables.solver;
        var itemVariables= solverWithVariables.itemVariables;
        var deliveryVariables = solverWithVariables.deliveryVariables;

        var constraint = solver.makeConstraint(deliveryNumber,deliveryNumber,"delivery_number_limit");
        for (int j=0;j<m;j++)
            constraint.setCoefficient(deliveryVariables[j],1);

        MPObjective objective = solver.objective();
        for (int i = 0; i < n; i++)
            objective.setCoefficient(itemVariables[i][deliveryIndex],1);
        objective.setMaximization();

        solver.solve();

        var deliveryToListOfItems = new HashMap<String, List<String>>();
        for (int j=0;j<m;j++){
            var list = new ArrayList<String>();
            for (int i=0;i<n;i++){
                if (itemVariables[i][j].solutionValue() == 1.0)
                    list.add(items.get(i));
            }
            if (!list.isEmpty())
                deliveryToListOfItems.put(_deliveries.get(j),list);
        }

        return new Solution((int) objective.value(), deliveryToListOfItems);
    }

    /**
     * Class to store solver and its variables.
     */
    private static class SolverWithVariables{
        MPVariable[][] itemVariables;
        MPVariable[] deliveryVariables;
        MPSolver solver;
        SolverWithVariables(MPVariable[][] itemVariables,MPVariable[] deliveryVariables,MPSolver solver){
            this.itemVariables = itemVariables;
            this.deliveryVariables = deliveryVariables;
            this.solver = solver;
        }
    }

    /**
     * Class to store solution with number of items in delivery type with max items.
     */
    public static class Solution {
        /**
         * Number of items in delivery type with max items.
         */
        int maxItems;
        /**
         * Map with delivery types as keys and list of items as values.
         */
        Map<String, List<String>> deliveryToListOfItems;

        /**
         * Constructor with initial values
         * @param maxItems {@link #maxItems}
         * @param deliveryToListOfItems {@link #deliveryToListOfItems}
         */
        Solution(int maxItems, Map<String,List<String>> deliveryToListOfItems){
            this.maxItems = maxItems;
            this.deliveryToListOfItems = deliveryToListOfItems;
        }

    }

    /**
     * Class of an exception that is thrown when solution cannot be found.
     */
    public static class SolutionNotFoundException extends Exception{
        public SolutionNotFoundException(String message) {
            super(message);
        }
    }
}