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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static java.lang.Thread.*;

@RestController
@Slf4j
public class TestController {


    @Value("${version:0.0.0}")
    private String version;

    @Autowired
    private TestWorker testWorker;

    @Autowired
    private ZeebeClientLifecycle client;

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

        try {
            log.info("Starting Workflow Instance at: " + new Date());
            WorkflowInstanceEvent workflowInstanceEvent = client.newCreateInstanceCommand().bpmnProcessId(processIds.get(0)).latestVersion().send().join();
            log.info("Started Workflow Instance at: " + new Date());
            if (workflowInstanceEvent != null) {
                log.info("New Instance Results: ");
                log.info("\t Workflow Instance Key: " + workflowInstanceEvent.getWorkflowInstanceKey());
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

        log.info("TEST with Simple Worker Finished at: " + new Date());
        log.info("+----------------------------------------------------------------------------------------------------+");
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Mono.just("Test Completed Successfully"));

    }


}
