package io.zeebe.k8s.tests;

import io.zeebe.client.api.response.DeploymentEvent;
import io.zeebe.client.api.response.Workflow;
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
import java.util.List;

@RestController
@Slf4j
public class TestController {


    @Value("${version:0.0.0}")
    private String version;

    @Autowired
    private ZeebeClientLifecycle client;

    @GetMapping("/info")
    public String infoWithVersion() {
        return "{ \"name\" : \"Zeebe K8s Tests\", \"version\" : \"" + version + "\", \"source\": \"https://github.com/salaboy/zeebe-k8s-tests/releases/tag/v"+version+"\" }";
    }


    @EventListener(classes = {ApplicationReadyEvent.class})
    public void appTestsStartedEvents() {
        log.info("Test Started Service Started!");
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Mono<String>> newTest(@RequestBody String test) {

        try {
            DeploymentEvent deploymentEvent = client.newDeployCommand().addResourceFromClasspath("c4p-orchestration.bpmn").send().join();
            if(deploymentEvent != null) {
                List<String> processIds = new ArrayList<>();
                for (Workflow w : deploymentEvent.getWorkflows()) {

                    log.info("> processId: " + w.getBpmnProcessId());
                    log.info("> resourceName: " + w.getResourceName());
                    log.info("> workflowKey: " + w.getWorkflowKey());
                    processIds.add(w.getBpmnProcessId());
                }
                return ResponseEntity
                        .status(HttpStatus.OK)
                        .body(Mono.just("Deployed: " + Arrays.toString(processIds.toArray())));

            }else{
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Mono.just("Deployment failed -> workflowInstanceEvent null"));
            }
        }catch (Exception e){
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Mono.just("ERROR: " + e.getMessage()));

        }


    }




}
