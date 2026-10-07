package gr.forth.ics.isl.iatianalysisworkflow;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Yannis Marketakis (marketak 'at' ics 'dot' forth 'dot' gr)
 */
public class Results {
    private static final Logger log = LogManager.getLogger(Results.class);

    public static void flushCSVResults(List<String> csvParts, File resultsFile){
        try(FileWriter fileWriter = new FileWriter(resultsFile,true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            PrintWriter printWriter = new PrintWriter(bufferedWriter)){
            printWriter.append(String.join(",", csvParts));
            printWriter.append("\n");
        }catch(IOException ex){
            log.error("An error occured while updating results file",ex);
            log.warn("The line to append was: "+csvParts);
        }
    }
}