package gr.forth.ics.isl.iati.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Utility application that collects XML files about IATI activities, and copies them in a target folder for further analysis.
 * 
 * @author Yannis Marketakis (marketak 'at' ics 'dot' forth 'dot' gr)
 */
public class IATIConsolidator {
    private static final Logger log= LogManager.getLogger(IATIConsolidator.class);
    public static final Path ROOT_FOLDER_WITH_RESOURCES=Path.of("D:/temp/FAO-IATI/iati-data/datasets");
    public static final Path TARGET_FOLDER=Path.of("D:/temp/FAO-IATI/split/");
    
    public static void main(String[] arsg) throws ParserException{
        validityChecks();
        try(Stream<Path> stream = Files.walk(ROOT_FOLDER_WITH_RESOURCES)){     
            List<Path> allXmlFiles=stream.filter(Files::isRegularFile)
                                         .filter(path -> path.toString().toLowerCase().endsWith(".xml"))
                                         .collect(Collectors.toList());
            log.debug("{} XML files to check",allXmlFiles.size());
            for(Path xmlFile : allXmlFiles){
                if(isIatiActivityFile(xmlFile.toFile())){
                    Files.copy(xmlFile, TARGET_FOLDER.resolve(xmlFile.getFileName()),StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }catch(IOException ex){
            throw new ParserException("An error occured while consolidating IATI XML files", ex);
        }
    }
    
    private static boolean isIatiActivityFile(File xmlFile){
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);

        try (InputStream input = new FileInputStream(xmlFile)) {
            XMLStreamReader reader=factory.createXMLStreamReader(input);
        try {
            int depth = 0;
            boolean rootIsIatiActivities = false;
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    depth++;
                    String name = reader.getLocalName();
                    if (depth == 1) {
                        rootIsIatiActivities =
                            name.equals("iati-activities");
                        if (!rootIsIatiActivities) {
                            return false;
                        }
                    }
                    if (depth == 2 &&
                        name.equals("iati-activity")) {
                        return true;
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    depth--;
                }
            }
            return false;
        } finally {
            reader.close();
        }}catch(IOException | XMLStreamException ex){
            log.error("An error occured while checking if an XML file ('"+xmlFile.getAbsolutePath()+"') is an IATI activity record file", ex);
            return false;
        }
    }
    
    private static void validityChecks() throws ParserException{
        if(!Files.exists(ROOT_FOLDER_WITH_RESOURCES)){
            throw new ParserException("The folder with PATH '"+ROOT_FOLDER_WITH_RESOURCES.toString()+"' does not exist");
        }
        if(!Files.isDirectory(ROOT_FOLDER_WITH_RESOURCES)){
            throw new ParserException("The folder with PATH '"+ROOT_FOLDER_WITH_RESOURCES.toString()+"' is not a directory");
        }
        if(!Files.exists(TARGET_FOLDER)){
            throw new ParserException("The folder with PATH '"+ROOT_FOLDER_WITH_RESOURCES.toString()+"' does not exist");
        }
        if(!Files.isDirectory(TARGET_FOLDER)){
            throw new ParserException("The folder with PATH '"+ROOT_FOLDER_WITH_RESOURCES.toString()+"' is not a directory");
        }
        
    }
}
