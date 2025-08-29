package org.example;

import org.graph4j.Graph;
import org.graph4j.GraphBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtractData {
    private final Path filename;
    private final Graph map;
    private final Map<Integer, CoordinatePair> nodeCoordinates;

    ExtractData(String filename) {
        Path projectRoot = Paths.get("").toAbsolutePath();
        this.filename = projectRoot.resolve("src/main/resources/TestData/" + filename);
        this.map = GraphBuilder.empty().buildGraph();
        this.nodeCoordinates = new HashMap<>();
    }

    public void buildGraph() {
        try {
            List<String> fileContent = Files.readAllLines(this.filename);
            Boolean checkCoordinates = false;
            Integer numberOfNodes = 0;

            for(String line : fileContent) {
                if(line.equals("LOCATION_COORD_SECTION")) {
                    checkCoordinates = true;
                } else if(line.equals("DEPOT_LOCATION_SECTION")){
                    checkCoordinates = false;
                } else if(checkCoordinates){
                    String[] values = line.trim().split("\\s+");
                    this.nodeCoordinates.put(Integer.parseInt(values[0]),
                            new CoordinatePair(Integer.parseInt(values[1]), Integer.parseInt(values[2])));
                    numberOfNodes++;
                    System.out.println(line + "\nNumber of nodes: " + numberOfNodes);
                }
            }

            for(int i=0; i < numberOfNodes; i++){
                this.map.addLabeledVertex(i);
            }

            // Generating a complete Graph
            for(Integer i = 0; i < numberOfNodes - 1; i++){
                for(Integer j = i + 1; j < numberOfNodes; j++){
                    this.map.addEdge(i, j);
                    this.map.setEdgeWeight(i, j, this.nodeCoordinates.get(i).distanceToNode(this.nodeCoordinates.get(j)));
                }
            }

            for(Integer i = 0; i < numberOfNodes - 1; i++) {
                for (Integer j = i + 1; j < numberOfNodes; j++) {
                    System.out.println("i = " + i + " j = " + j + " dist = " + this.map.getEdgeWeight(i, j));
                }
            }

        } catch (IOException ex) {
            System.err.println("Error reading file: " + ex.getMessage());
        }
    }
}
