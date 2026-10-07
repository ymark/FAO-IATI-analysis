package gr.forth.ics.isl.iatianalysisworkflow;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import java.util.Collection;
import java.util.HashSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Yannis Marketakis (marketak 'at' ics 'dot' forth 'dot' gr)
 */
public class Vocabulary {
    public static Collection<String> ecosystemTerms=new HashSet<>();
    private static final Logger log= LogManager.getLogger(Vocabulary.class);
    
    public Vocabulary() throws IOException{
        int size=loadFromCSV(Resources.ECOSYSTEMS_PATH,2,ecosystemTerms);
        log.info("loaded {} ecosystem terms",ecosystemTerms.size());
    }
    
    private static int loadFromCSV(String relativePath,int index, Collection<String> collection) throws IOException {
        CSVParser csvParser= CSVFormat.DEFAULT.parse(new InputStreamReader(new FileInputStream(relativePath), "UTF-8"));
        int size=0;
        for(CSVRecord csvRecord : csvParser){
            collection.add(csvRecord.get(index));
            size+=1;
        }
        return size;
    }
}
