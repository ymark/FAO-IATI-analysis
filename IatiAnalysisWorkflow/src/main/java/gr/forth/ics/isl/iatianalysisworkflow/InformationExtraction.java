package gr.forth.ics.isl.iatianalysisworkflow;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Yannis Marketakis (marketak 'at' ics 'dot' forth 'dot' gr)
 */
public class InformationExtraction {
    private Multimap<String,String> ecosystemClassification=HashMultimap.create();
    private static final Logger log = LogManager.getLogger(InformationExtraction.class);
    
    public void extractInformationUsingVocabularyOnly(File datasetFileCsv) throws FileNotFoundException, UnsupportedEncodingException, IOException{
        log.info("Start extracting information using vocabularies only");
        CSVParser csvParser= CSVFormat.DEFAULT.parse(new InputStreamReader(new FileInputStream(datasetFileCsv), "UTF-8"));
        for(CSVRecord csvRecord : csvParser){
            this.checkEcosystemOccurencies(csvRecord.get(0),csvRecord.get(1)+" - "+csvRecord.get(2));
        }
    }
    
    private void checkEcosystemOccurencies(String recordId, String text){
        for(String ecosystemTerm : Vocabulary.ecosystemTerms){
            if(text.toLowerCase().contains(ecosystemTerm.toLowerCase())){
                log.debug("Found classification for record '{}' with term '{}'",recordId,ecosystemTerm);
                ecosystemClassification.put(recordId, ecosystemTerm);
                Results.flushCSVResults(Arrays.asList(recordId,ecosystemTerm), new File(Resources.ECOSYSTEMS_CLASSIFICATION_RESULTS));
            }
        }
    }

}