package bootcamp;

import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;


import java.security.PublicKey;
import java.util.List;

/* Our contract, governing how our state will evolve over time.
 * See src/main/java/examples/ArtContract.java for an example. */
public class IouContract implements Contract {
    public static String ID = "bootcamp.IouContract";

    //verify transaction
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        if(tx.getCommands().size() != 1){
            throw new IllegalArgumentException("Transaction must have one command");
        }
        Command command = tx.getCommand(0);
        if(command.getValue() instanceof Commands.Issue){
            //"Shape" Constraint - No. input states, no. output states, command
            if(tx.getInputStates().size() !=0)
                throw new IllegalArgumentException("Issue transaction must have no inputs");
            if(tx.getOutputStates().size() != 1)
                throw new IllegalArgumentException("Issue transaction must have 1 outputs");

            //Content constraint
            if(!(tx.getOutput(0) instanceof  IouState))
                throw new IllegalArgumentException("Ouptput must be a IouState");
            IouState iouState = (IouState) tx.getOutput(0);
            if(iouState.getAmount()<=0)
                throw new IllegalArgumentException("Amount must be bigger than 0");

            //Required singer constraint
            PublicKey issuekey = iouState.getIssuer().getOwningKey();
            List<PublicKey> requiresKeys = command.getSigners();
            if(!(requiresKeys.contains(issuekey))){
                throw new IllegalArgumentException("Transaction  singer must contain issuer");
            }
        }
        else {
            throw new IllegalArgumentException("Uncecognized command");
        }
    }


    public interface Commands extends CommandData {
        class Issue implements Commands { }
    }
}
