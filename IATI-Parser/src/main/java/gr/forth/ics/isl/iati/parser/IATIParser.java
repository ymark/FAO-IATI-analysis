package gr.forth.ics.isl.iati.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.commons.lang3.tuple.Triple;

/**
 * @author Yannis Marketakis (marketak 'at' ics 'dot' forth 'dot' gr)
 */
public class IATIParser {
    private static void parseFromSplittedFiles(File xmlFolder,File iatiInfoCsv) throws IOException, XMLStreamException{
        for(File singleFile : xmlFolder.listFiles()){
            Set<Triple<String,String,String>> iatiData=parseXmlFile(singleFile);
            appendIatiInfoCsv(iatiData,iatiInfoCsv);
//            break;
        }
    }
            
    private static Set<Triple<String,String,String>> parseXmlFile(File xmlFile) throws IOException, XMLStreamException{
        Set<Triple<String,String,String>> retSet=new HashSet<>();
        XMLInputFactory factory = XMLInputFactory.newInstance();
        try (InputStream in = Files.newInputStream(xmlFile.toPath())) {
            XMLStreamReader reader = factory.createXMLStreamReader(in);
            String identifier = null;
            String title = null;
            String description = null;

            boolean insideActivity = false;
            boolean insideTitle = false;
            boolean insideDescription = false;

            while (reader.hasNext()) {

                int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {

                    String element = reader.getLocalName();

                    switch (element) {

                        case "iati-activity":
                            insideActivity = true;
                            identifier = null;
                            title = null;
                            description = null;
                            break;

                        case "iati-identifier":
                            if (insideActivity) {
                                identifier = reader.getElementText().trim();
                            }
                            break;

                        case "title":
                            if (insideActivity) {
                                insideTitle = true;
                            }
                            break;

                        case "description":
                            if (insideActivity) {
                                insideDescription = true;
                            }
                            break;

                        case "narrative":

                            if (insideActivity) {

                                String text = reader.getElementText().trim();

                                if (insideTitle && title == null) {
                                    title = text;
                                }

                                if (insideDescription && description == null) {
                                    description = text;
                                }
                            }

                            break;
                    }

                } else if (event == XMLStreamConstants.END_ELEMENT) {

                    String element = reader.getLocalName();

                    switch (element) {

                        case "title":
                            insideTitle = false;
                            break;

                        case "description":
                            insideDescription = false;
                            break;

                        case "iati-activity":

//                            System.out.println("Identifier: " + identifier);
//                            System.out.println("Title: " + title);
//                            System.out.println("Description: " + description);
//                            System.out.println("--------------------------------");
                            retSet.add(Triple.of(identifier, title, description));

                            insideActivity = false;
                            break;
                    }
                }
            }
            reader.close();
        }        
        return retSet;
    }
    
    private static void appendIatiInfoCsv(Set<Triple<String,String,String>> iatiData, File iatiInfoCsv){
        boolean fileExists = Files.exists(iatiInfoCsv.toPath());
        try (BufferedWriter writer = Files.newBufferedWriter(iatiInfoCsv.toPath(),StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
             // Write header only when creating the file
            if (!fileExists) {
                writer.write("identifier,title,description");
                writer.newLine();
            }
            for(Triple<String,String,String> triple : iatiData){
                writer.write(escapeCsv(triple.getLeft()));
                writer.write(",");
                writer.write(escapeCsv(triple.getMiddle()));
                writer.write(",");
                writer.write(escapeCsv(triple.getRight()));
                writer.newLine();
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }
    
    private static String escapeCsv(String value) {

        if (value == null) {
            return "";
        }

        // Double quotation marks inside CSV values
        String escaped = value.replace("\"", "\"\"");

        // Quote the value if it contains comma, quote, CR or LF
        if (escaped.contains(",")
                || escaped.contains("\"")
                || escaped.contains("\n")
                || escaped.contains("\r")) {

            return "\"" + escaped + "\"";
        }

        return escaped;
    }
    
    public static void main(String[] args) throws IOException, XMLStreamException {
        parseFromSplittedFiles(new File("D:/temp/FAO-IATI/split"),new File("D:/temp/FAO-IATI/iati-info.csv"));
    }
}
