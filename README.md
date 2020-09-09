#Zeebe K8s Helm Charts Integration Tests

This project is a simple test packaged as a Helm chart for testing Zeebe Helm Charts. In special the Full Chart which includes multiple components. 
ZeeQS can be used to assert the status of the workflows that had been initiated. 

This test checks the full chart with ZEEQS enabled. 

Simple test is executed: 
1) Deploy a workflow definition with a single worker
2) The test project contains the worker and connects to the Zeebe Gateway specified when installing this chart
3) It wait for the job to be picked up and completed
4) It checks that the workflow instance is completed in ZeeQS

```
helm install test-core zeebe-jx/zeebe-full --values zeebe-dev-with-zeeqs-profile.yaml  --set tasklist.enabled=false 
```

```
helm install test zeebe-jx/zeebe-k8s-tests --set zeebeGateway=test-core-zeebe-gateway:26500 --set env.ZEEQS_URL=http://test-core-zeeqs:9000/graphql
```
You can run the test by running the following command

```
curl -X POST localhost:8080 -H "Content-Type: application/json" -d "{}"
```

200 means it worked

500 means that something failed
