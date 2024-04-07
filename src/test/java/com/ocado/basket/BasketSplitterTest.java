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

public class BasketSplitterTest {
    private static BasketSplitter basketSplitter;
    private static final String configName = "test_config.json";
    private static final String solutionsName = "solutions.json";
    private static final String[] basketNames = {
            "basket-1.json",
            "basket-2.json"
    };
    private static final List<List<String>> ListOfListOfItems = new ArrayList<>();
    private static Map<String,List<Integer>> solutions;

    @BeforeClass
    public static void init() throws IOException, NullPointerException {
        var classLoader = BasketSplitterTest.class.getClassLoader();

        var solutionPath = new File(classLoader.getResource(solutionsName).getFile()).getAbsolutePath();
        readSolutions(solutionPath);

        var absPath = new File(classLoader.getResource(configName).getFile()).getAbsolutePath();
        basketSplitter = new BasketSplitter(absPath);

        //read list of items from files
        for (var filename:basketNames)
            ListOfListOfItems.add(basketSplitter.readItems(new File(classLoader.getResource(filename).getFile()).getAbsolutePath()));
    }

    private static void readSolutions(String path) throws IOException {
        String jsonString = new String(Files.readAllBytes(Paths.get(path)));
        solutions = new ObjectMapper().readValue(jsonString, new TypeReference<>() {});
    }

    @Test
    public void split() throws Exception {
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