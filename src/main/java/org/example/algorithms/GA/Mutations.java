package org.example.algorithms.GA;

import java.util.Collections;
import java.util.List;

public class Mutations {
    private static final double PM = 0.3;

    public List<List<Integer>> mutation(List<List<Integer>> population){

        for(List<Integer> individual : population){
            double chanceToMutate = Math.random();
            if(chanceToMutate < PM){
                swapMutation(individual);
            }
        }

        return population;
    }

    private void swapMutation(List<Integer> individual){
        int customer1 = (int) (Math.random() * individual.size());
        int customer2 = (int) (Math.random() * individual.size());

        while(customer1 == customer2){
            customer1 = (int) (Math.random() * individual.size());
            customer2 = (int) (Math.random() * individual.size());
        }

        Collections.swap(individual, customer1, customer2);
    }
}
