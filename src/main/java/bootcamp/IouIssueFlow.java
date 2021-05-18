package bootcamp;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.List;

import static java.util.Collections.singletonList;

@InitiatingFlow
@StartableByRPC
public class IouIssueFlow extends FlowLogic<SignedTransaction> {
    private final Party owner;
    private final int amount;

    public IouIssueFlow(Party owner, int amount) {
        this.owner = owner;
        this.amount = amount;
    }

    private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // We choose our transaction's notary (the notary prevents double-spends).
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        // We get a reference to our own identity.
        Party issuer = getOurIdentity();

        /* ============================================================================
         *         TODO 1 - Create our IouState to represent on-ledger tokens!
         * ===========================================================================*/
        // We create our new IouState.
        IouState iouState = new IouState(issuer,owner,amount);
        IouContract.Commands.Issue command = new IouContract.Commands.Issue();


        /* ============================================================================
         *      TODO 3 - Build our token issuance transaction to update the ledger!
         * ===========================================================================*/
        // We build our transaction.
        TransactionBuilder transactionBuilder = new TransactionBuilder();
        transactionBuilder.setNotary(notary);
        transactionBuilder.addOutputState(iouState,IouContract.ID);
        List<PublicKey> requireSingers = ImmutableList.of(iouState.getIssuer().getOwningKey(),owner.getOwningKey());
        transactionBuilder.addCommand(command,requireSingers);


        /* ============================================================================
         *          TODO 2 - Write our IouContract to control token issuance!
         * ===========================================================================*/
        // We check our transaction is valid based on its contracts.
        transactionBuilder.verify(getServiceHub());

        FlowSession session = initiateFlow(owner);

        // We sign the transaction with our private key, making it immutable.
        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

        // The counterparty signs the transaction
        SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(signedTransaction, singletonList(session)));

        // We get the transaction notarised and recorded automatically by the platform.
        return subFlow(new FinalityFlow(fullySignedTransaction, singletonList(session)));
    }
}
