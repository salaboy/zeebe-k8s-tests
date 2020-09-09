package io.zeebe.k8s.tests;

import io.zeebe.client.api.response.DeploymentEvent;
import io.zeebe.client.api.response.Workflow;
import io.zeebe.client.api.response.WorkflowInstanceEvent;
import io.zeebe.spring.client.ZeebeClientLifecycle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.Thread.sleep;

@RestController
@Slf4j
public class TestController {


    @Value("${version:0.0.0}")
    private String version;

    @Value("${ZEEQS_URL:http://zeeqs:9000}")
    private String ZEEQS_URL;

    @Autowired
    private TestWorker testWorker;

    @Autowired
    private ZeebeClientLifecycle client;

    private RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/")
    public String helloWorld() {
        return "hello world! - from PR";
    }

    @GetMapping("/info")
    public String infoWithVersion() {
        return "{ \"name\" : \"Zeebe K8s Tests\", \"version\" : \"" + version + "\", \"source\": \"https://github.com/salaboy/zeebe-k8s-tests/releases/tag/v" + version + "\" }";
    }


    @EventListener(classes = {ApplicationReadyEvent.class})
    public void appTestsStartedEvents() {
        log.info("Test Started Service Started!");
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Mono<String>> deployAndStartInstanceWithWorker() throws InterruptedException {

        log.info("+----------------------------------------------------------------------------------------------------+");
        log.info("TEST with Simple Worker Started at: " + new Date());
        List<String> processIds = new ArrayList<>();
        try {
            log.info("Deploying Workflow Definition at " + new Date());
            DeploymentEvent deploymentEvent = client.newDeployCommand().addResourceFromClasspath("simple-test-with-worker.bpmn").send().join();
            log.info("Deployed Workflow Definition at " + new Date());
            if (deploymentEvent != null) {
                log.info("Deployment Results: ");
                for (Workflow w : deploymentEvent.getWorkflows()) {
                    log.info("\t BPMN Process Id: " + w.getBpmnProcessId());
                    log.info("\t Resource Name: " + w.getResourceName());
                    log.info("\t Workflow Key: " + w.getWorkflowKey());
                    processIds.add(w.getBpmnProcessId());
                }
            }else{
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Mono.just("ERROR Deployment Null"));
            }
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Mono.just("ERROR Deploying: " + e.getMessage()));

        }
        long startedKey = 0L;
        try {
            log.info("Starting Workflow Instance at: " + new Date());
            WorkflowInstanceEvent workflowInstanceEvent = client.newCreateInstanceCommand().bpmnProcessId(processIds.get(0)).latestVersion().send().join();
            log.info("Started Workflow Instance at: " + new Date());
            if (workflowInstanceEvent != null) {
                log.info("New Instance Results: ");
                log.info("\t Workflow Instance Key: " + workflowInstanceEvent.getWorkflowInstanceKey());
                startedKey = workflowInstanceEvent.getWorkflowInstanceKey();
                log.info("\t BPMN Process Id: " + workflowInstanceEvent.getBpmnProcessId());
                log.info("\t Workflow Version: " + workflowInstanceEvent.getVersion());
            }else{
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Mono.just("ERROR Starting Instance Null"));
            }
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Mono.just("ERROR Starting Instance: " + e.getMessage()));

        }
        while (!testWorker.jobCompleted) {
            log.info("Waiting for Worker to complete the Job at: " + new Date());
            sleep(500);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("content-type", "application/graphql");

        // query is a grapql query wrapped into a String
        String query1 = "{\n" +
                "  workflowInstances(stateIn: [COMPLETED], limit: 100) {\n" +
                "    nodes {\n" +
                "      key\n" +
                "    }\n" +
                "  }\n" +
                "}";



        ResponseEntity<String> response = restTemplate.postForEntity(ZEEQS_URL, new HttpEntity<>(query1, headers), String.class);
        log.info("GraphQL response: " + response.getBody());
        boolean containsKey = response.getBody().contains(String.valueOf(startedKey));
        while(!containsKey){
            log.info("Waiting for Workflow Instance Key : " + startedKey + "appear in ZeeQS at: " +  new Date());
            sleep(500);
            response = restTemplate.postForEntity(ZEEQS_URL, new HttpEntity<>(query1, headers), String.class);
            log.info("GraphQL response: " + response.getBody());
            containsKey = response.getBody().contains(String.valueOf(startedKey));
        }


        log.info("TEST with Simple Worker Finished at: " + new Date());
        log.info("+----------------------------------------------------------------------------------------------------+");
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Mono.just("Test Completed Successfully"));

    }


}
