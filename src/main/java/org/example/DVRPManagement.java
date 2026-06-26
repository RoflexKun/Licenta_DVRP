package org.example;

import org.example.algorithms.GA.GeneticAlgorithm;
import org.example.algorithms.PSO.PSO;
import org.example.algorithms.VNS.VNS;
import org.graph4j.Graph;

import java.util.*;

public class DVRPManagement {
    private DataParser dataParser;
    private int timeCount;
    private Set<Integer> servedNodes;
    private Set<Integer> depotList;
    private Map<Integer, Integer> lastVehiclePosition;
    private Map<Integer, Integer> currentVehicleTime;
    private Map<Integer, Integer> currentVehicleCapacity;
    private List<List<Integer>> plannedRoutes;
    private List<List<Integer>> fullRoutes;

    private final int TIME_SLICE = 40;

    public DVRPManagement(DataParser dataParser){
        this.dataParser = dataParser;

        this.timeCount = this.dataParser.getDepotOpenTimeFrame().get(0)[1] / this.TIME_SLICE;
        this.depotList = new HashSet<>(this.dataParser.getMapDepotToNode().values());

    }

    private boolean isDepot(int node){
        return this.depotList.contains(node);
    }

    private double commitRoutes(List<List<Integer>> bestRoutes, int nextTimeLimit, Graph graph, boolean isLastSlice){
        int timestep = this.dataParser.getTimestep();
        Map<Integer, Integer> openingTimes = this.dataParser.getVisitOpeningTime();

        double currentDistanceCovered = 0.0;

        for(int i = 0; i < bestRoutes.size(); i ++){
            List<Integer> currentRoute = bestRoutes.get(i);
            int vehicleTime = this.currentVehicleTime.get(i);
            int lastVisitedNode = this.lastVehiclePosition.get(i);
            int currentNode=0, lastNode=lastVisitedNode;
            int capacityLeft = this.currentVehicleCapacity.get(i);
            int cutoffIndex = currentRoute.size();

            for(int j = 1; j < currentRoute.size(); j++) {
                currentNode = currentRoute.get(j);
                lastNode = currentRoute.get(j-1);

                double distance = (lastNode == currentNode) ? 0.0 : graph.getEdgeWeight(lastNode, currentNode);
                int travelTime = (int) (distance / timestep);
                int arrivalTime = vehicleTime + travelTime;

                if(!isDepot(currentNode)){
                    while(arrivalTime < this.dataParser.getVisitOpeningTime().get(currentNode)){
                        arrivalTime += timestep;
                    }
                }

                if(!isLastSlice && arrivalTime > nextTimeLimit){
                    cutoffIndex = j;
                    break;
                }

                if(!isDepot(currentNode)){
                    this.servedNodes.add(currentNode);
                    capacityLeft -= Math.abs(this.dataParser.getListOfCustomerDemand().get(currentNode));
                } else {
                    capacityLeft = this.dataParser.getCapacityLimit();
                }

                List<Integer> history = this.fullRoutes.get(i);
                if (history.get(history.size() - 1) != currentNode) {
                    history.add(currentNode);
                }

                currentDistanceCovered += distance;
                vehicleTime = arrivalTime;
                lastNode = currentNode;

            }

            this.lastVehiclePosition.put(i, lastNode);
            this.currentVehicleTime.put(i, vehicleTime);
            this.currentVehicleCapacity.put(i, capacityLeft);

            List<Integer> futureRoute = new ArrayList<>();
            futureRoute.add(lastNode);

            for(int k = cutoffIndex; k < currentRoute.size(); k++) {
                int futureNode = currentRoute.get(k);

                if (!isDepot(futureNode) && this.servedNodes.contains(futureNode))
                    continue;

                if (futureRoute.get(futureRoute.size() - 1) != futureNode) {
                    futureRoute.add(futureNode);
                }
            }

            if (!isDepot(futureRoute.get(futureRoute.size() - 1))){
                futureRoute.add(0);
            }

            this.plannedRoutes.set(i, futureRoute);

        }

        return currentDistanceCovered;
    }

    public double DVRP(String heuristic){
        this.servedNodes = new HashSet<>();
        this.lastVehiclePosition = new HashMap<>();
        this.currentVehicleTime = new HashMap<>();
        this.currentVehicleCapacity = new HashMap<>();
        this.plannedRoutes = new ArrayList<>();
        this.fullRoutes = new ArrayList<>();
        int maxCapacity = this.dataParser.getCapacityLimit();

        double totalDistanceVNS = 0.0;
        double totalDistanceGA = 0.0;
        double totalDistancePSO = 0.0;

        for (int i = 0; i < this.dataParser.getNumberOfVehicles(); i++){
            lastVehiclePosition.put(i, 0); // Each vehicle starts from the depot
            currentVehicleTime.put(i, 0); // Each vehicle starts from time 0 (when the depot opens up)
            currentVehicleCapacity.put(i, maxCapacity);

            List<Integer> initialRoute = new ArrayList<>();
            initialRoute.add(0);
            initialRoute.add(0);
            plannedRoutes.add(initialRoute);

            List<Integer> historyRoute = new ArrayList<>();
            historyRoute.add(0);
            this.fullRoutes.add(historyRoute);
        }

        for(int i = 0; i < this.TIME_SLICE; i ++){
            DataParser dataParserModified = new DataParser(this.dataParser);
            int currentTime = i * this.timeCount;

            Set<Integer> activeClients = new HashSet<>();
            Map<Integer, Integer> openingTime = dataParserModified.getVisitOpeningTime();
            Collection<Integer> activePositions = this.lastVehiclePosition.values();



            int depotClosingTime = this.dataParser.getDepotOpenTimeFrame().get(0)[1];
            double t_co = 0.5;
            int cutoffTimeLimit = (int) (depotClosingTime * t_co);

            for(Integer node : new ArrayList<>(openingTime.keySet())){
                if(isDepot(node))
                    continue;

                boolean isKnownFromStart = (openingTime.get(node) > cutoffTimeLimit);
                if (i == TIME_SLICE - 1) {
                    if (!this.servedNodes.contains(node)) {
                        activeClients.add(node);
                    }
                } else if(activePositions.contains(node) ||
                        (openingTime.get(node) <= currentTime && !this.servedNodes.contains(node)) ||
                        (isKnownFromStart && !this.servedNodes.contains(node))) {
                    activeClients.add(node);
                }
            }

            switch (heuristic){
                case "VNS":
                    MapBuilder mapBuilder = new MapBuilder(dataParserModified);
                    VNS vnsDVRP = new VNS(mapBuilder, this.lastVehiclePosition, this.currentVehicleTime, activeClients, this.currentVehicleCapacity, this.plannedRoutes);

                    List<List<Integer>> bestRoutes = vnsDVRP.runVNS();
                    //System.out.println("Slice " + i + " best Routes: " + bestRoutes.toString());

                    int nextTimeLimit = currentTime + this.timeCount;

                    totalDistanceVNS += commitRoutes(bestRoutes, nextTimeLimit, mapBuilder.getMapFromData(), i == (this.TIME_SLICE - 1) );
                    System.out.println("Slice " + i + " ISTORIC COMPLET: " + this.fullRoutes.toString());
                    break;
                case "GA":
                    MapBuilder mapBuilderGA = new MapBuilder(dataParserModified);
                    GeneticAlgorithm gaDVRP = new GeneticAlgorithm(mapBuilderGA, this.lastVehiclePosition, this.currentVehicleTime, activeClients, this.currentVehicleCapacity, this.plannedRoutes);

                    List<List<Integer>> bestRoutesGA = gaDVRP.runGA();
                    int nextTimeLimitGA = currentTime + this.timeCount;

                    totalDistanceGA += commitRoutes(bestRoutesGA, nextTimeLimitGA, mapBuilderGA.getMapFromData(), i == (this.TIME_SLICE - 1));
                    System.out.println("Slice " + i + " ISTORIC COMPLET GA: " + this.fullRoutes.toString());
                    break;
                case "PSO":
                    MapBuilder mapBuilderPSO = new MapBuilder(dataParserModified);
                    PSO psoDVRP = new PSO(mapBuilderPSO, this.lastVehiclePosition, this.currentVehicleTime, activeClients, this.currentVehicleCapacity, this.plannedRoutes);

                    List<List<Integer>> bestRoutesPSO = psoDVRP.runPSO();
                    int nextTimeLimitPSO = currentTime + this.timeCount;

                    totalDistancePSO += commitRoutes(bestRoutesPSO, nextTimeLimitPSO, mapBuilderPSO.getMapFromData(), i == (this.TIME_SLICE - 1));
                    System.out.println("Slice " + i + " ISTORIC COMPLET PSO: " + this.fullRoutes.toString());
                    break;
                default:
                    System.err.println("Heuristic non existent");
            }

        }



        switch(heuristic){
            case "VNS":
                return totalDistanceVNS;
            case "GA":
                return totalDistanceGA;
            case "PSO":
                return totalDistancePSO;
            default:
                System.err.println("Heuristic non existent");
                return -1;
        }
    }
}
