package gr.forth.ics.isl.iatianalysisworkflow;

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Yannis Marketakis (marketak 'at' ics 'dot' forth 'dot' gr)
 */
public class IatiAnalysisWorkflow {
    private static final Logger log= LogManager.getLogger(IatiAnalysisWorkflow.class);

    public static void main(String[] args) throws IOException {
        log.info("Starting the FAO IATI analysis workflow");
        Vocabulary vocab=new Vocabulary();
        
        // extract info 
   
        // produce results
    }
}
