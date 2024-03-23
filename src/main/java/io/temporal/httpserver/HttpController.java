package io.temporal.httpserver;


import io.temporal._final.WorkerProcess;
import io.temporal._final.solution.workflow.AccountWorkflow;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.model.Account;
import io.temporal.model.StartWorkflowResponse;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HttpController {


    final String taskQueue = WorkerProcess.TASK_QUEUE;
    private final WorkflowClient workflowClient;

    public HttpController() {

        final WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        workflowClient = WorkflowClient.newInstance(service);

    }


    @PutMapping("/accounts")
    public StartWorkflowResponse createAccount(Account account) {

        final String workflowId = AccountWorkflow.workflowIdFromAccountId(account.accountId());

        final AccountWorkflow accountWorkflow = workflowClient.newWorkflowStub(AccountWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue(taskQueue)
                        .build());


        final WorkflowExecution workflow = WorkflowClient.start(accountWorkflow::open,
                account);

        return new StartWorkflowResponse(workflowId, workflow.getRunId());
    }


    @GetMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }


}