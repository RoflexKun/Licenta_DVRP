package org.example;

import org.example.algorithms.GA.GeneticAlgorithm;
import org.example.algorithms.PSO.PSO;
import org.example.algorithms.VNS.VNS;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BenchmarkGenerator {
    private List<String> listOfTestData;
    private final int NUMBER_OF_ITERATIONS = 30;

    private static class ResultPair {
        double min;
        double avg;

        public ResultPair(double min, double avg){
            this.min = min;
            this.avg = avg;
        }
    }

    public BenchmarkGenerator(){
        this.listOfTestData = extractListOfTestData();
    }

    private List<String> extractListOfTestData(){
        List<String> listOfTestData = new ArrayList<>();
        File folder = new File("src/main/resources/TestData");

        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null){
            for(File file : listOfFiles){
                listOfTestData.add(file.getName());
            }
        }

        System.out.println(listOfTestData);
        return listOfTestData;
    }

    private ResultPair benchmarkVNS(String testData){
        List<Double> listOfScores = new ArrayList<>();

        DVRPManagement dvrpManager = new DVRPManagement(new DataParser(testData));

        for (int i = 0; i < NUMBER_OF_ITERATIONS; i++){

            double currentRunBestResult = dvrpManager.DVRP("VNS");
            System.out.println("Interation " + i + " best result:" + currentRunBestResult);
            listOfScores.add(currentRunBestResult);
        }
        double average = 0.00;
        for (Double scoreOfRun : listOfScores){
            average += scoreOfRun;
        }
        average = average / NUMBER_OF_ITERATIONS;

        return new ResultPair(Collections.min(listOfScores), average);
    }

    private ResultPair benchmarkGeneticAlgorithm(String testData){
        List<Double> listOfScores = new ArrayList<>();

        DVRPManagement dvrpManager = new DVRPManagement(new DataParser(testData));

        for (int i = 0; i < NUMBER_OF_ITERATIONS; i++){

            double currentRunBestResult = dvrpManager.DVRP("GA");
            System.out.println("Interation " + i + " best result:" + currentRunBestResult);
            listOfScores.add(currentRunBestResult);
        }
        double average = 0.00;
        for (Double scoreOfRun : listOfScores){
            average += scoreOfRun;
        }
        average = average / NUMBER_OF_ITERATIONS;

        return new ResultPair(Collections.min(listOfScores), average);
    }

    private ResultPair benchmarkPSO(String testData){
        List<Double> listOfScores = new ArrayList<>();

        DVRPManagement dvrpManager = new DVRPManagement(new DataParser(testData));

        for(int i = 0; i < NUMBER_OF_ITERATIONS; i++){
            double currentRunBestResult = dvrpManager.DVRP("PSO");
            System.out.println("Interation " + i + " best result:" + currentRunBestResult);
            listOfScores.add(currentRunBestResult);
        }
        double average = 0.00;
        for (Double scoreOfRun : listOfScores) {
            average += scoreOfRun;
        }
        average = average / NUMBER_OF_ITERATIONS;

        return new ResultPair(Collections.min(listOfScores), average);
    }

    public void runBenchmark(){

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String directoryPath = "src/main/resources/BenchmarkResults";
        String filePath = directoryPath + "/Benchmark_Results_" + timestamp + ".csv";

        try(PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("Instance,GA_Min,GA_Avg,VNS_Min,VNS_Avg,PSO_Min,PSO_Avg");

            for(String testData : this.listOfTestData){
                System.out.println("Running benchmark for: " + testData);
                ResultPair psoResults = benchmarkPSO(testData);
                System.out.println("PSO Results: " + psoResults.min + " " + psoResults.avg);

                //ResultPair geneticAlgorithmResults = benchmarkGeneticAlgorithm(testData);
                //System.out.println("GA Results: " + geneticAlgorithmResults.min + " " + geneticAlgorithmResults.avg);

                //ResultPair vnsResults = benchmarkVNS(testData);
                //System.out.println("VNS Results: " + vnsResults.min + " " + vnsResults.avg);

                //writer.printf(Locale.US, "%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f%n",
                //        testData,
                //        geneticAlgorithmResults.min,
                //        geneticAlgorithmResults.avg,
                //        vnsResults.min,
                //        vnsResults.avg,
                //        psoResults.min,
                //        psoResults.avg
                //);

                //writer.flush();

            }

        } catch(IOException e){
            System.err.println(Arrays.toString(e.getStackTrace()));
        }

    }


}
