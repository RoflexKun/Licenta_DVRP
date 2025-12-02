package org.example.heuristics;

import org.graph4j.Graph;

import java.util.*;

public class Neighborhoods {
    private List<List<Integer>> currentSolution;

    public Neighborhoods(List<List<Integer>> currentSolution){
        this.currentSolution = currentSolution;
    }

    private boolean isDepot(int node, Set<Integer> depotSet){
        return depotSet.contains(node);
    }

    public List<List<Integer>> copyCurrentSolution(List<List<Integer>> currentSolution){
        List<List<Integer>> newSolution = new ArrayList<>();
        for(int i = 0; i < currentSolution.size(); i++){
            List<Integer> row = new ArrayList<>();

            for(int j = 0; j < currentSolution.get(i).size(); j++){
                row.add(currentSolution.get(i).get(j));
            }
            newSolution.add(row);
        }
        return newSolution;
    }

    public List<List<Integer>> relocateCustomer(int route1, int positionRoute1, int route2, int positionRoute2){
        List<List<Integer>> newSolution = copyCurrentSolution(this.currentSolution);

        if(newSolution.get(route1).size() <= 2)
            return newSolution;

        Integer customerIndex = newSolution.get(route1).get(positionRoute1);
        newSolution.get(route1).remove(positionRoute1);

        if(newSolution.get(route2).size() <= 1)
            positionRoute2 = 1;

        newSolution.get(route2).add(positionRoute2, customerIndex);

        return newSolution;
    }

    public List<List<Integer>> swapCustomers(int route1, int positionRoute1, int route2, int positionRoute2){
        List<List<Integer>> newSolution = copyCurrentSolution(this.currentSolution);

        Integer customer1 = newSolution.get(route1).get(positionRoute1);
        Integer customer2 = newSolution.get(route2).get(positionRoute2);

        newSolution.get(route1).set(positionRoute1, customer2);
        newSolution.get(route2).set(positionRoute2, customer1);

        return newSolution;
    }

}
