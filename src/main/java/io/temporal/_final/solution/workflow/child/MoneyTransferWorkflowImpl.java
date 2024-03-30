package io.temporal._final.solution.workflow.child;

import io.temporal.activity.ActivityOptions;
import io.temporal.activity.NotificationService;
import io.temporal.common.SearchAttributeKey;
import io.temporal.model.TransferRequest;
import io.temporal.model.TransferResponse;
import io.temporal.model.TransferState;
import io.temporal.service.AccountService;
import io.temporal.service.DepositRequest;
import io.temporal.service.WithdrawRequest;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;


public class MoneyTransferWorkflowImpl implements MoneyTransferWorkflow {

    private final AccountService accountService =
            Workflow.newActivityStub(
                    AccountService.class,
                    ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(3)).build());

    private final NotificationService notificationService =
            Workflow.newActivityStub(
                    NotificationService.class,
                    ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(3)).build());
    private final Logger log = Workflow.getLogger(MoneyTransferWorkflowImpl.class.getSimpleName());
    private final SearchAttributeKey<String> transferRequestState = SearchAttributeKey.forKeyword("TransferRequestState");
    private TransferState transferState;
    private boolean approveReceived = false;
    private TransferRequest transferRequest;

    @Override
    public TransferResponse transfer(final TransferRequest transferRequest) {

        log.info("Init for transfer: " + transferRequest);

        this.transferRequest = transferRequest;

        this.transferState = TransferState.ApprovalNotRequired;

        if (transferRequest.amount() > 100) {

            transferState = TransferState.ApprovalRequired;

            //Setting this SA will allow query workflows by `TransferRequestState="ApprovalRequired"`
            //http://localhost:8233/namespaces/default/workflows?query=WorkflowType%3D%22MoneyTransferWorkflow%22+and+ExecutionStatus%3D%22Running%22+and+TransferRequestState%3D%22ApprovalRequired%22
            Workflow.upsertTypedSearchAttributes(
                    transferRequestState.valueSet(transferState.name())
            );

            log.info("Request need approval: " + transferRequest);

            // Wait until the signal is received or timeout
            final Duration timeout = Duration.ofSeconds(30); // Can be days, years...
            boolean authorizationReceivedWithinTimeOut = Workflow.await(timeout, () -> approveReceived);
            if (!authorizationReceivedWithinTimeOut) {
                transferState = TransferState.ApprovalTimedOut;
                log.info("Authorization not received within " + timeout);
                return new TransferResponse(transferRequest, transferState);
            }


            log.info("TransferApproved: " + transferState);

            if (TransferState.ApprovalDenied.equals(transferState)) {
                // notify customer...
                notificationService.transferDenied(transferRequest);
                log.info("Notify customer, transferApproved: " + transferRequest);
                return new TransferResponse(transferRequest, transferState);

            }
        }

        accountService.withdraw(
                new WithdrawRequest(
                        transferRequest.fromAccountId(),
                        transferRequest.amount()));

        accountService.deposit(
                new DepositRequest(
                        transferRequest.toAccountId(),
                        transferRequest.amount()));

        notificationService.transferCompleted(transferRequest);

        log.info("Completed for transfer: " + transferRequest);
        return new TransferResponse(transferRequest, transferState);
    }

    @Override
    public void approveTransfer(TransferState transferState) {
        this.transferState = transferState;
        this.approveReceived = true;
    }

    @Override
    public TransferRequest getTransferRequest() {
        return this.transferRequest;
    }



}
