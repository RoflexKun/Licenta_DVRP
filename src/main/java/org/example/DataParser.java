package org.example;

import org.graph4j.Graph;
import org.graph4j.GraphBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataParser {
    private final Path filename;

    private int numberOfDepots; // number of depots where the vehicles can return to
    private int numberOfCapacities; // number of capacities/constraints of a vehicle
    private int numberOfVisits; // number of locations besides the depots
    private int numberOfLocations; // total number of locations: depots + customers
    private int numberOfVehicles;
    private int capacityLimit;
    private int timestep; // Unit of time

    private final List<Integer> listOfDepotsIndex = new ArrayList<>();
    private Map<Integer, Integer> listOfCustomerDemand = new HashMap<>();
    private final Map<Integer, CoordinatePair> nodeCoordinates = new HashMap<>();
    private Map<Integer, Integer> mapDepotToNode = new HashMap<>();
    private Map<Integer, Integer> mapVisitIdToNode = new HashMap<>();
    private Map<Integer, Integer> durationOfVisit = new HashMap<>();
    private Map<Integer, Integer[]> depotOpenTimeFrame = new HashMap<>();
    private Map<Integer, Integer> visitOpeningTime = new HashMap<>();


    DataParser(String filename) {
        Path projectRoot = Paths.get("").toAbsolutePath();
        this.filename = projectRoot.resolve("src/main/resources/TestData/" + filename);
        parseFileContent();

    }

    public Integer extractValueFromLine(String line){
        String[] splitLine = line.split(" ");
        return Integer.parseInt(splitLine[1]);
    }

    public Map<Integer, Integer> extractListOfValues(List<String> fileContent, int indexLine){
        Map<Integer, Integer> mapIndexToValue = new HashMap<>();
        while(true){
            String[] values = fileContent.get(indexLine).trim().split(" ");

            if(values[0].matches(".*[A-Z].*"))
                break;

            mapIndexToValue.put(Integer.parseInt(values[0]), Integer.parseInt(values[1]));
            indexLine++;
        }
        return mapIndexToValue;
    }

    public void parseFileContent() {
        try {
            List<String> fileContent = Files.readAllLines(this.filename);

            for(String line : fileContent){
                if(line.contains("NUM_DEPOTS")){
                    this.numberOfDepots = extractValueFromLine(line);
                } else if (line.contains("NUM_CAPACITIES")){
                    this.numberOfCapacities = extractValueFromLine(line);
                } else if(line.contains("NUM_VISITS")){
                    this.numberOfVisits = extractValueFromLine(line);
                } else if(line.contains("NUM_LOCATIONS")){
                    this.numberOfLocations = extractValueFromLine(line);
                } else if(line.contains("NUM_VEHICLES")){
                    this.numberOfVehicles = extractValueFromLine(line);
                } else if(line.contains("CAPACITIES")){
                    this.capacityLimit = extractValueFromLine(line);
                } else if(line.contains("DEPOTS")){
                    int valuesIndex = fileContent.indexOf(line) + 1;
                    while(true){
                        String depotIndex = fileContent.get(valuesIndex).trim();

                        if(depotIndex.matches(".*[A-Z].*"))
                            break;

                        this.listOfDepotsIndex.add(Integer.parseInt(depotIndex.trim()));
                        valuesIndex++;
                    }
                } else if(line.contains("DEMAND_SECTION")) {
                    this.listOfCustomerDemand = extractListOfValues(fileContent, fileContent.indexOf(line) + 1);
                } else if(line.contains("LOCATION_COORD_SECTION")) {
                    int valuesIndex = fileContent.indexOf(line) + 1;
                    while(true){
                        String[] coordinatesLine = fileContent.get(valuesIndex).trim().split(" ");
                        if(coordinatesLine[0].matches(".*[A-Z].*"))
                            break;

                        this.nodeCoordinates.put(Integer.parseInt(coordinatesLine[0]),
                                new CoordinatePair(Integer.parseInt(coordinatesLine[1]), Integer.parseInt(coordinatesLine[2])));
                        valuesIndex++;
                    }
                } else if(line.contains("DEPOT_LOCATION_SECTION")){
                    this.mapDepotToNode = extractListOfValues(fileContent, fileContent.indexOf(line) + 1);
                } else if(line.contains("VISIT_LOCATION_SECTION")){
                    this.mapVisitIdToNode = extractListOfValues(fileContent, fileContent.indexOf(line) + 1);
                } else if(line.contains("DURATION_SECTION")){
                    this.durationOfVisit = extractListOfValues(fileContent, fileContent.indexOf(line) + 1);
                } else if(line.contains("DEPOT_TIME_WINDOW_SECTION")) {
                    int valuesIndex = fileContent.indexOf(line) + 1;
                    while(true){
                        String[] coordinatesLine = fileContent.get(valuesIndex).trim().split(" ");
                        if(coordinatesLine[0].matches(".*[A-Z].*"))
                            break;

                        this.depotOpenTimeFrame.put(Integer.parseInt(coordinatesLine[0]),
                                new Integer[]{
                                        Integer.parseInt(coordinatesLine[1]),
                                        Integer.parseInt(coordinatesLine[2])
                        });
                        valuesIndex++;
                    }
                } else if(line.contains("TIMESTEP")){
                    String[] splitLine = line.split(" ");
                    this.timestep = Integer.parseInt(splitLine[2]);
                } else if(line.contains("TIME_AVAIL_SECTION")){
                    this.visitOpeningTime = extractListOfValues(fileContent, fileContent.indexOf(line) + 1);
                }

            }

        } catch (IOException ex) {
            System.err.println("Error reading file: " + ex.getMessage());
        }
    }

    public int getNumberOfDepots() {
        return numberOfDepots;
    }

    public int getCapacityLimit() {
        return capacityLimit;
    }

    public int getNumberOfCapacities() {
        return numberOfCapacities;
    }

    public int getNumberOfLocations() {
        return numberOfLocations;
    }

    public int getNumberOfVehicles() {
        return numberOfVehicles;
    }

    public int getNumberOfVisits() {
        return numberOfVisits;
    }

    public int getTimestep() {
        return timestep;
    }

    public Map<Integer, Integer> getListOfCustomerDemand() {
        return listOfCustomerDemand;
    }

    public List<Integer> getListOfDepotsIndex() {
        return listOfDepotsIndex;
    }

    public Map<Integer, CoordinatePair> getNodeCoordinates() {
        return nodeCoordinates;
    }

    public Map<Integer, Integer> getDurationOfVisit() {
        return durationOfVisit;
    }

    public Map<Integer, Integer> getMapDepotToNode() {
        return mapDepotToNode;
    }

    public Map<Integer, Integer> getMapVisitIdToNode() {
        return mapVisitIdToNode;
    }

    public Map<Integer, Integer> getVisitOpeningTime() {
        return visitOpeningTime;
    }

    public Map<Integer, Integer[]> getDepotOpenTimeFrame() {
        return depotOpenTimeFrame;
    }
}
