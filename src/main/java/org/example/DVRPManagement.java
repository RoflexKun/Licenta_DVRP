package org.example;

import java.util.*;

public class DVRPManagement {
    private DataParser dataParser;
    private int timeCount;
    private Set<Integer> servedNodes;
    private Set<Integer> depotList;
    private Map<Integer, Integer> lastVehiclePosition;
    private Map<Integer, Integer> currentVehicleTime;

    private final int TIME_SLICE = 40;

    public DVRPManagement(DataParser dataParser){
        this.dataParser = dataParser;

        this.timeCount = this.dataParser.getDepotOpenTimeFrame().get(0)[1] / this.TIME_SLICE;
        this.depotList = new HashSet<>(this.dataParser.getMapDepotToNode().values());

    }

    private boolean isDepot(int node){
        return this.depotList.contains(node);
    }

    public void DVRP(){
        this.servedNodes = new HashSet<>();
        this.lastVehiclePosition = new HashMap<>();
        this.currentVehicleTime = new HashMap<>();

        for (int i = 0; i < this.dataParser.getNumberOfVehicles(); i++){
            lastVehiclePosition.put(i, 0); // Each vehicle starts from the depot
            currentVehicleTime.put(i, 0); // Each vehicle starts from time 0 (when the depot opens up)
        }

        for(int i = 0; i < this.TIME_SLICE; i ++){
            DataParser dataParserModified = new DataParser(this.dataParser);
            int currentTime = i * this.timeCount + this.timeCount;

            List<Integer> allNodes = dataParserModified.getListOfDepotsIndex();
            List<Integer> onlyNodesOpened = new ArrayList<>();
            Map<Integer, Integer> openingTime = dataParserModified.getVisitOpeningTime();

            for(Integer node : allNodes){
                if(isDepot(node)){
                    onlyNodesOpened.add(node);
                    continue;
                }

                if (openingTime.get(node) <= currentTime && !this.servedNodes.contains(node)) {
                    onlyNodesOpened.add(node);
                }
            }

            dataParserModified.setListOfDepotsIndex(onlyNodesOpened);

            // TODO: If the clients are served, add them to the servedNodes HashSet
            // TODO: Need to modify heuristic in order to transition from VRPTW to DVRP
            // TODO: Need to make some algorithm to modify the Maps that remember the last node / the current time
        }
    }
}
