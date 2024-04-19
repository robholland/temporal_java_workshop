package io.temporal._0.demo;

import io.temporal._0.demo.workflow.MoneyTransferWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.model.TransferRequest;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;

import static io.temporal.activity.AccountServiceImplWithRuntimeException.VALUE_TO_THROW_EXCEPTION;

public class StarterRetry {

    static final String MY_BUSINESS_ID = StarterRetry.class.getPackageName() + ":money-transfer";

    public static void main(String[] args) {

        // Get a Workflow service stub.
        final WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(WorkflowServiceStubsOptions
                .newBuilder()
                .setTarget(io.temporal.Constants.targetGRPC)
                .build());

        final WorkflowClient client = WorkflowClient.newInstance(service, WorkflowClientOptions
                .newBuilder()
                .setNamespace(io.temporal.Constants.namespace)
                .build());

        final WorkflowOptions options =
                WorkflowOptions.newBuilder()
                        .setWorkflowId(MY_BUSINESS_ID)
                        .setTaskQueue(WorkerProcess.TASK_QUEUE)
                        .build();

        // Create the workflow client stub.
        // It is used to start our workflow execution.
        final MoneyTransferWorkflow workflow =
                client.newWorkflowStub(MoneyTransferWorkflow.class, options);

        // For demo (if amount == VALUE_TO_THROW_EXCEPTION),
        // AccountServiceImplWithRuntimeException.deposit will throw a RuntimeException several times
        // before success
        workflow.transfer(new TransferRequest(
                "fromAccount",
                "toAccount",
                VALUE_TO_THROW_EXCEPTION));
    }
}
