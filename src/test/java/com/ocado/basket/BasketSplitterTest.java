package com.ocado.basket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Junit4 class to test BasketSplitter class
 */
public class BasketSplitterTest {
    private static BasketSplitter basketSplitter;
    private static final String configName = "test_config.json";
    private static final String solutionsName = "solutions.json";
    private static final String[] basketNames = {
            "basket-1.json",
            "basket-2.json"
    };
    /**
     * List with list of items from specific basket.
     */
    private static final List<List<String>> ListOfListOfItems = new ArrayList<>();
    /**
     * Map that maps names of the basket with its correct solution.
     * First element of list is number of delivery types, second is number of items in type that has most items.
     */
    private static Map<String,List<Integer>> solutions;

    /**
     * Initialization of necessary attributes
     * @throws IOException If there was problem with I/O operation
     * @throws NullPointerException If file doesn't exist
     */
    @BeforeClass
    public static void init() throws IOException, NullPointerException {
        var classLoader = BasketSplitterTest.class.getClassLoader();

        var solutionPath = new File(classLoader.getResource(solutionsName).getFile()).getAbsolutePath();
        readSolutions(solutionPath);

        var absPath = new File(classLoader.getResource(configName).getFile()).getAbsolutePath();
        basketSplitter = new BasketSplitter(absPath);

        for (var filename:basketNames)
            ListOfListOfItems.add(basketSplitter.readItems(new File(classLoader.getResource(filename).getFile()).getAbsolutePath()));
    }

    /**
     * Helper method that reads and maps {@link #solutions}.
     * @param path absolute path to file.
     * @throws IOException If an I/O error occurs while reading configuration file.
     */
    private static void readSolutions(String path) throws IOException {
        String jsonString = new String(Files.readAllBytes(Paths.get(path)));
        solutions = new ObjectMapper().readValue(jsonString, new TypeReference<>() {});
    }

    /**
     * Tests split() function of class BasketSplitter
     * @throws BasketSplitter.SolutionNotFoundException If solution wasn't found.
     */
    @Test
    public void split() throws BasketSplitter.SolutionNotFoundException {
        for (int i=0;i<ListOfListOfItems.size();i++){
            int optimalNumberOfDeliveries = solutions.get(basketNames[i]).get(0);
            int optimalMaxNumberOfItemsInMaxDeliveryType = solutions.get(basketNames[i]).get(1);
            var result = basketSplitter.split(ListOfListOfItems.get(i));

            assertEquals(optimalNumberOfDeliveries,result.size());

            int maxsize = 0;
            for (var entry:result.values()){
                maxsize = Math.max(maxsize,entry.size());
            }
            assertEquals(optimalMaxNumberOfItemsInMaxDeliveryType,maxsize);
        }
    }

}