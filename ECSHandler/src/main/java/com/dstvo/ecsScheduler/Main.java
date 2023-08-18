package com.dstvo.ecsScheduler;

import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.UpdateServiceRequest;
import com.amazonaws.services.ecs.model.UpdateServiceResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class Main implements RequestHandler<Map<String, Object>, String> {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static final String REGION = "eu-west-1";
    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";

    @Override
    public String handleRequest(Map<String, Object> input, Context context) {
        Map<String, Object> detail = (Map<String, Object>) input.get("detail");
        String action = (String) detail.get("action");
        List<Map<String, Object>> clusterData = readClusterDataFromConfig();
        logger.info("Cluster Data: " + clusterData);

        switch (action) {
            case ACTION_START:
                updateECSClusters(clusterData, true);
                break;
            case ACTION_STOP:
                updateECSClusters(clusterData, false);
                break;
            default:
                throw new IllegalArgumentException("Invalid action: " + action);
        }

        return "Action performed successfully";
    }

    private List<Map<String, Object>> readClusterDataFromConfig() {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.yaml")) {
            if (inputStream == null) {
                throw new RuntimeException("Unable to load configuration file.");
            }
            return yaml.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Error reading cluster data from configuration.", e);
        }
    }

    private void updateECSClusters(List<Map<String, Object>> clusterData, boolean start) {
        AmazonECS client = AmazonECSClientBuilder.standard().withRegion(REGION).build();
        for (Map<String, Object> entry : clusterData) {
            String clusterName = (String) entry.get("Cluster Name");
            String serviceName = (String) entry.get("Service Name");
            int desiredCount = start ? (int) entry.get("Scale") : 0;

            UpdateServiceRequest request = new UpdateServiceRequest()
                    .withCluster(clusterName)
                    .withService(serviceName)
                    .withDesiredCount(desiredCount);

            UpdateServiceResult response = client.updateService(request);
            String action = start ? "Started" : "Stopped";
            logger.info("ECS Clusters " + action + " for cluster: " + clusterName);
            // You can process the response or do other actions here
        }
    }
}
