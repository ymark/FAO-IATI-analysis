package gr.forth.ics.isl.iatianalysisworkflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Yannis Marketakis (marketak 'at' ics 'dot' forth 'dot' gr)
 */
public class PostAnalysisUtils {
    private static final Logger log= LogManager.getLogger(PostAnalysisUtils.class);
    
    private static void pruneUnknownClassfiication(Path sourceClassification, Path prunedClassification) throws FileNotFoundException, UnsupportedEncodingException, IOException{
        log.info("Prune IATI records classified as 'UNKNOWN'");
        CSVParser csvParser= CSVFormat.DEFAULT.parse(new InputStreamReader(new FileInputStream(sourceClassification.toFile()), "UTF-8"));
        for(CSVRecord csvRecord : csvParser){
            if(csvRecord.get(1).equalsIgnoreCase("UNKNOWN")){
                continue;
            }else{
                Results.flushCSVResults(Arrays.asList("\""+csvRecord.get(0)+"\"",csvRecord.get(1)), prunedClassification.toFile());
            }
        }
    }
    
    private static void collectIatiActivityRecordsProperlyClassfied(Path classificationResults, Path sourceIatiRecords, Path prunnedIatiRecords)throws FileNotFoundException, UnsupportedEncodingException, IOException{
        log.info("Collect IATI records already classfied");
        Map<String,String> iatiRecordIds=new HashMap<>();
        CSVParser csvParser= CSVFormat.DEFAULT.parse(new InputStreamReader(new FileInputStream(classificationResults.toFile()), "UTF-8"));
        for(CSVRecord csvRecord : csvParser){
            iatiRecordIds.put(csvRecord.get(0),csvRecord.get(1));
        }
        csvParser.close();
        csvParser= CSVFormat.DEFAULT.parse(new InputStreamReader(new FileInputStream(sourceIatiRecords.toFile()), "UTF-8"));
        for(CSVRecord csvRecord : csvParser){
            if(csvRecord.get(0).equalsIgnoreCase("identifier")){
                continue;
            }
            if(iatiRecordIds.keySet().contains(csvRecord.get(0))){
                Results.flushCSVResults(Arrays.asList("\""+csvRecord.get(0)+"\"",iatiRecordIds.get(csvRecord.get(0)),"\""+csvRecord.get(1)+"\""), prunnedIatiRecords.toFile());
            }
        }
    }
    
    public static void main(String[] args) throws FileNotFoundException, IOException{
//        pruneUnknownClassfiication(Path.of("D:/temp/FAO-IATI/sample-to-work-on/IATI_ecosystem_classification_LLM.csv"), 
//                                   Path.of("D:/temp/FAO-IATI/sample-to-work-on/IATI_ecosystem_classification_LLM-prunned.csv"));
        

//        collectIatiActivityRecordsProperlyClassfied(Path.of("D:/temp/FAO-IATI/sample-to-work-on/IATI_ecosystem_classification_LLM-prunned.csv"),
//                                                    Path.of("D:/temp/FAO-IATI/sample-to-work-on/iati-info.csv"),
//                                                    Path.of("D:/temp/FAO-IATI/sample-to-work-on/iati-info-prunned.csv"));
        
    }
}
