import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    private UTXOPool utxoPool;
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool= new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        double sumOfOut =0;
        double sumOfIn = 0;
        Set<UTXO> used = new HashSet<>();
        for(int i=0 ; i<tx.numInputs() ; ++i){
        	Transaction.Input input = tx.getInput(i);
        	UTXO utxo = new UTXO(input.prevTxHash,input.outputIndex);
        	if(!utxoPool.contains(utxo))return false;//check (1)
        	Transaction.Output output =utxoPool.getTxOutput(utxo);// the consume coin correspond prev output coin;
        	sumOfIn += output.value;//(5)
        	if(!Crypto.verifySignature(output.address, tx.getRawDataToSign(i),
        			input.signature))return false;//check(2) 
        	if(!used.add(utxo))return false;//check(3)
        }
        for(Transaction.Output output : tx.getOutputs()){
        	if(output.value <0)return false;//check(5)
        	sumOfOut+=output.value;
        }
        if(sumOfIn < sumOfOut)return false;//check(5);
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
    	HashSet<Transaction>txVis = new HashSet<>();
    	//fixed point algorithm,iter untill no new transaction is valid
    	while (true) {
    		boolean updated = false;
			for(Transaction tx: possibleTxs){
				if(txVis.contains(tx))continue;
				if(isValidTx(tx)){
					txVis.add(tx);
					updated = true;
					//add unspent coin
					for(int i=0 ; i<tx.numOutputs() ; ++i){
						UTXO utxo = new UTXO(tx.getHash(),i);
						utxoPool.addUTXO(utxo, tx.getOutput(i));
					}
					//delete spent coin
					for(int i=0 ; i<tx.numInputs() ; ++i){
						Transaction.Input input = tx.getInput(i);
						UTXO utxo = new UTXO(input.prevTxHash,input.outputIndex);
						utxoPool.removeUTXO(utxo);
					}
				}
			}
			if(!updated)break;
		};
		Transaction[] ret = new Transaction[txVis.size()];
		int idx =0;
		for(Transaction tx : txVis)
			ret[idx++] = tx;
		return ret;
    }
    
}
