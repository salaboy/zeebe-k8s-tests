package io.zeebe.k8s.tests;

import io.zeebe.client.api.response.ActivatedJob;
import io.zeebe.client.api.worker.JobClient;
import io.zeebe.spring.client.annotation.ZeebeWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
public class TestWorker {

    public boolean jobCompleted = false;

    @ZeebeWorker(name = "test", type = "test-log-worker", timeout = 60 * 60 * 24 * 1000)
    public void testJobComplete(final JobClient client, final ActivatedJob job) {
        log.info("Job Acquired ->  Key:" + job.getKey() + " at: " + new Date());
        log.info("\t Job Workflow Key: " + +job.getWorkflowKey());
        log.info("\t Job Workflow BPMN Process Id: " + job.getBpmnProcessId());
        log.info("\t Job Workflow Element Id: " + job.getElementId());
        log.info("\t Job Workflow Variables: " + job.getVariables());

        client.newCompleteCommand(job.getKey()).send().join();

        log.info("Job Completed ->  Key:" + job.getKey() + " at: " + new Date());
        jobCompleted = true;
        return;

    }
}
