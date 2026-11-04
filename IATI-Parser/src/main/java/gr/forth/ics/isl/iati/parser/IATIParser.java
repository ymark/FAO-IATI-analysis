package gr.forth.ics.isl.iati.parser;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.commons.lang3.tuple.Triple;

/** Parses the contents of IATI Activity records (from multiple XML files) and exports their contents in a CSV file.
 * 
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
        System.out.println("checking file "+xmlFile.getAbsolutePath());
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
        String escaped = value.replace("\"", "\"\"").replaceAll("\n", " ").replaceAll("\\\n", " ").replaceAll("\r", " ").replaceAll("\\\r", " ");
        // Quote the value if it contains comma, quote, CR or LF
        if (escaped.contains(",")
                || escaped.contains("\"")
                || escaped.contains("\n")
                || escaped.contains("\r")) {

            return "\"" + escaped + "\"";
        }
        return escaped;
    }
    
    private static Set<String> getRecordIds(File iatiRecordIds) throws IOException{
        Set<String> retSet = new HashSet<>(Files.readAllLines(iatiRecordIds.toPath()));
        return retSet;
    }
    
    public static void extractSDGFromIatiRecords(File xmlFolder, File iatiRecordIds, File iatiRecordsAndSDG) throws IOException, XMLStreamException{
        Set<String> recordIds=getRecordIds(iatiRecordIds);
        for(File singleFile : xmlFolder.listFiles()){
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
            
            Set<String> sectorVocabs = Set.of("7", "8", "9");
            Set<String> tagVocabs = Set.of("1", "2", "3");

            try (InputStream is = new BufferedInputStream(new FileInputStream(singleFile))) {
                XMLStreamReader reader = factory.createXMLStreamReader(is);

                try {
                    String id = null;
                    boolean matched = false;
                    int depth = 0;

                    while (reader.hasNext()) {
                        int event = reader.next();

                        if (event == XMLStreamConstants.START_ELEMENT) {
                            String name = reader.getLocalName();

                            if (name.equals("iati-activity")) {
                                id = null;
                                matched = false;
                                depth = 1;
                            }
                            else if (depth > 0) {
                                depth++;

                                if (name.equals("iati-identifier") && depth == 2) {
                                    id = reader.getElementText().trim();
                                    matched = recordIds.contains(id);
                                    depth--;
                                }
                                else if (matched && depth == 2 &&
                                        (name.equals("sector") || name.equals("tag"))) {

                                    String vocab = reader.getAttributeValue(null, "vocabulary");
                                    String code = reader.getAttributeValue(null, "code");

                                    Set<String> allowed = name.equals("sector")
                                            ? sectorVocabs : tagVocabs;

                                    if (vocab != null && code != null && allowed.contains(vocab)) {
                                        Results.flushCSVResults(Arrays.asList("\""+id+"\"",name,vocab,code),
                                                                iatiRecordsAndSDG);
                                    }
                                }
                            }
                        }
                        else if (event == XMLStreamConstants.END_ELEMENT && depth > 0) {
                            depth--;

                            if (depth == 0) {
                                id = null;
                                matched = false;
                            }
                        }
                    }
                } finally {
                    reader.close();
                }
            }
        }
    }
    
    public static void extractCountriesFromIatiRecords(File xmlFolder, File iatiRecordIds, File iatiRecordsAndCountries) throws IOException, XMLStreamException{
        Set<String> recordIds=getRecordIds(iatiRecordIds);
        for(File singleFile : xmlFolder.listFiles()){
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
            
            try (InputStream is = new BufferedInputStream(new FileInputStream(singleFile))) {
                XMLStreamReader reader = factory.createXMLStreamReader(is);

                try {
                    String id = null;
                    boolean matched = false;
                    int depth = 0;

                    while (reader.hasNext()) {
                        int event = reader.next();

                        if (event == XMLStreamConstants.START_ELEMENT) {
                            String name = reader.getLocalName();

                            if (name.equals("iati-activity")) {
                                id = null;
                                matched = false;
                                depth = 1;
                            }
                            else if (depth > 0) {
                                depth++;

                                if (name.equals("iati-identifier") && depth == 2) {
                                    id = reader.getElementText().trim();
                                    matched = recordIds.contains(id);
                                    depth--;
                                }
                                else if (matched && depth == 2 && name.equals("recipient-country")) {
                                    String code = reader.getAttributeValue(null, "code");
                                    String narrative = "";

                                    // Read the contents of recipient-country
                                    while (reader.hasNext()) {
                                        int innerEvent = reader.next();

                                        if (innerEvent == XMLStreamConstants.START_ELEMENT
                                                && reader.getLocalName().equals("narrative")) {
                                            narrative = reader.getElementText().trim();
                                        }
                                        else if (innerEvent == XMLStreamConstants.END_ELEMENT
                                                && reader.getLocalName().equals("recipient-country")) {
                                            break;
                                        }
                                    }

                                    depth--; // recipient-country has been consumed
                               

                                    Results.flushCSVResults(Arrays.asList("\""+id+"\"",code
//                                            narrative
                                    ),
                                                                iatiRecordsAndCountries);
                                }
                            }
                        }
                        else if (event == XMLStreamConstants.END_ELEMENT && depth > 0) {
                            depth--;
                        }
                    }
                } finally {
                    reader.close();
                }
            }
        }
    }
    
    public static void extractReportingOrgFromIatiRecords(File xmlFolder, File iatiRecordIds, File iatiRecordsAndRepOrg) throws IOException, XMLStreamException{
        Set<String> recordIds=getRecordIds(iatiRecordIds);
        for(File singleFile : xmlFolder.listFiles()){
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
            
            try (InputStream is = new BufferedInputStream(new FileInputStream(singleFile))) {
                XMLStreamReader reader = factory.createXMLStreamReader(is);

                try {
                    String id = null;
                    boolean matched = false;
                    int depth = 0;

                    while (reader.hasNext()) {
                        int event = reader.next();

                        if (event == XMLStreamConstants.START_ELEMENT) {
                            String name = reader.getLocalName();

                            if (name.equals("iati-activity")) {
                                id = null;
                                matched = false;
                                depth = 1;
                            }
                            else if (depth > 0) {
                                depth++;

                                if (name.equals("iati-identifier") && depth == 2) {
                                    id = reader.getElementText().trim();
                                    matched = recordIds.contains(id);
                                    depth--;
                                }
                                else if (matched && depth == 2 && name.equals("reporting-org")) {
                                    String code = reader.getAttributeValue(null, "type");
                                    String narrative = "";

                                    // Read the contents of recipient-country
                                    while (reader.hasNext()) {
                                        int innerEvent = reader.next();

                                        if (innerEvent == XMLStreamConstants.START_ELEMENT
                                                && reader.getLocalName().equals("narrative")) {
                                            narrative = reader.getElementText().trim();
                                        }
                                        else if (innerEvent == XMLStreamConstants.END_ELEMENT
                                                && reader.getLocalName().equals("reporting-org")) {
                                            break;
                                        }
                                    }

                                    depth--; // recipient-country has been consumed
                               

                                    Results.flushCSVResults(Arrays.asList("\""+id+"\"",code,narrative),
                                                                iatiRecordsAndRepOrg);
                                }
                            }
                        }
                        else if (event == XMLStreamConstants.END_ELEMENT && depth > 0) {
                            depth--;
                        }
                    }
                } finally {
                    reader.close();
                }
            }
        }
    }
    
    public static void extractActivityStatusFromIatiRecords(File xmlFolder, File iatiRecordIds, File iatiRecordsAndActivityStatus) throws IOException, XMLStreamException{
        Set<String> recordIds=getRecordIds(iatiRecordIds);
        for(File singleFile : xmlFolder.listFiles()){
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
            
            try (InputStream is = new BufferedInputStream(new FileInputStream(singleFile))) {
                XMLStreamReader reader = factory.createXMLStreamReader(is);

                try {
                    String id = null;
                    boolean matched = false;
                    int depth = 0;

                    while (reader.hasNext()) {
                        int event = reader.next();

                        if (event == XMLStreamConstants.START_ELEMENT) {
                            String name = reader.getLocalName();

                            if (name.equals("iati-activity")) {
                                id = null;
                                matched = false;
                                depth = 1;
                            }
                            else if (depth > 0) {
                                depth++;

                                if (name.equals("iati-identifier") && depth == 2) {
                                    id = reader.getElementText().trim();
                                    matched = recordIds.contains(id);
                                    depth--;
                                }
                                else if (matched && depth == 2 && name.equals("activity-status")) {
                                    String code = reader.getAttributeValue(null, "code");
                                    

                                    
                                    depth--; // recipient-country has been consumed
                               

                                    Results.flushCSVResults(Arrays.asList("\""+id+"\"",code),
                                                                iatiRecordsAndActivityStatus);
                                }
                            }
                        }
                        else if (event == XMLStreamConstants.END_ELEMENT && depth > 0) {
                            depth--;
                        }
                    }
                } finally {
                    reader.close();
                }
            }
        }
    }
    
    public static void extractActivityScopeFromIatiRecords(File xmlFolder, File iatiRecordIds, File iatiRecordsAndActivityScope) throws IOException, XMLStreamException{
        Set<String> recordIds=getRecordIds(iatiRecordIds);
        for(File singleFile : xmlFolder.listFiles()){
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
            
            try (InputStream is = new BufferedInputStream(new FileInputStream(singleFile))) {
                XMLStreamReader reader = factory.createXMLStreamReader(is);

                try {
                    String id = null;
                    boolean matched = false;
                    int depth = 0;

                    while (reader.hasNext()) {
                        int event = reader.next();

                        if (event == XMLStreamConstants.START_ELEMENT) {
                            String name = reader.getLocalName();

                            if (name.equals("iati-activity")) {
                                id = null;
                                matched = false;
                                depth = 1;
                            }
                            else if (depth > 0) {
                                depth++;

                                if (name.equals("iati-identifier") && depth == 2) {
                                    id = reader.getElementText().trim();
                                    matched = recordIds.contains(id);
                                    depth--;
                                }
                                else if (matched && depth == 2 && name.equals("activity-scope")) {
                                    String code = reader.getAttributeValue(null, "code");
                                    

                                    
                                    depth--; // recipient-country has been consumed
                               

                                    Results.flushCSVResults(Arrays.asList("\""+id+"\"",code),
                                                                iatiRecordsAndActivityScope);
                                }
                            }
                        }
                        else if (event == XMLStreamConstants.END_ELEMENT && depth > 0) {
                            depth--;
                        }
                    }
                } finally {
                    reader.close();
                }
            }
        }
    }
    
    public static void extractAidTypeFromIatiRecords(File xmlFolder, File iatiRecordIds, File iatiRecordsAndAidType) throws IOException, XMLStreamException{
        Set<String> recordIds=getRecordIds(iatiRecordIds);
        for(File singleFile : xmlFolder.listFiles()){
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
            
            try (InputStream is = new BufferedInputStream(new FileInputStream(singleFile))) {
                XMLStreamReader reader = factory.createXMLStreamReader(is);

                try {
                    String id = null;
                    boolean matched = false;
                    int depth = 0;

                    while (reader.hasNext()) {
                        int event = reader.next();

                        if (event == XMLStreamConstants.START_ELEMENT) {
                            String name = reader.getLocalName();

                            if (name.equals("iati-activity")) {
                                id = null;
                                matched = false;
                                depth = 1;
                            }
                            else if (depth > 0) {
                                depth++;

                                if (name.equals("iati-identifier") && depth == 2) {
                                    id = reader.getElementText().trim();
                                    matched = recordIds.contains(id);
                                    depth--;
                                }
                                else if (matched && depth == 2 &&
                                        (name.equals("default-aid-type"))) {

                                    String vocab = reader.getAttributeValue(null, "vocabulary");
                                    String code = reader.getAttributeValue(null, "code");

                                    
                                    if (vocab != null && code != null && vocab.equals("1")) {
                                        Results.flushCSVResults(Arrays.asList("\""+id+"\"",code),
                                                                iatiRecordsAndAidType);
                                    }
                                }
                            }
                        }
                        else if (event == XMLStreamConstants.END_ELEMENT && depth > 0) {
                            depth--;

                            if (depth == 0) {
                                id = null;
                                matched = false;
                            }
                        }
                    }
                } finally {
                    reader.close();
                }
            }
        }
    }
    
    public static void extractActivityDatesFromIatiRecords(File xmlFolder, File iatiRecordIds, File iatiRecordsAndDates) throws IOException, XMLStreamException{
        Set<String> recordIds=getRecordIds(iatiRecordIds);
        for(File singleFile : xmlFolder.listFiles()){
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
            
            try (InputStream is = new BufferedInputStream(new FileInputStream(singleFile))) {
                XMLStreamReader reader = factory.createXMLStreamReader(is);

                try {
                    String id = null;
                    boolean matched = false;
                    int depth = 0;

                    while (reader.hasNext()) {
                        int event = reader.next();

                        if (event == XMLStreamConstants.START_ELEMENT) {
                            String name = reader.getLocalName();

                            if (name.equals("iati-activity")) {
                                id = null;
                                matched = false;
                                depth = 1;
                            }
                            else if (depth > 0) {
                                depth++;

                                if (name.equals("iati-identifier") && depth == 2) {
                                    id = reader.getElementText().trim();
                                    matched = recordIds.contains(id);
                                    depth--;
                                }
                                else if (matched && depth == 2 &&
                                        (name.equals("activity-date"))) {

                                    String type = reader.getAttributeValue(null, "type");
                                    String isoDate = reader.getAttributeValue(null, "iso-date");

                                    if (type != null && isoDate != null) {
                                        Results.flushCSVResults(Arrays.asList("\""+id+"\"",type,isoDate),
                                                                iatiRecordsAndDates);
                                    }
                                }
                            }
                        }
                        else if (event == XMLStreamConstants.END_ELEMENT && depth > 0) {
                            depth--;

                            if (depth == 0) {
                                id = null;
                                matched = false;
                            }
                        }
                    }
                } finally {
                    reader.close();
                }
            }
        }
    }
    
    public static void main(String[] args) throws IOException, XMLStreamException {
//        parseFromSplittedFiles(new File("D:/temp/FAO-IATI/split"),new File("D:/temp/FAO-IATI/iati-info.csv"));
//        extractSDGFromIatiRecords(new File("D:/temp/FAO-IATI/split"), 
//                                  new File("D:/temp/FAO-IATI/sample-to-work-on/iati_record_ids.txt"),
//                                  new File("D:/temp/FAO-IATI/sample-to-work-on/iati_record_SDGs.csv"));
//        extractCountriesFromIatiRecords(new File("D:/temp/FAO-IATI/split"), 
//                                        new File("D:/temp/FAO-IATI/sample-to-work-on/iati_record_ids.txt"),
//                                        new File("D:/temp/FAO-IATI/sample-to-work-on/iati_record_countries.csv"));
//        extractReportingOrgFromIatiRecords(new File("D:/temp/FAO-IATI/split"), 
//                                        new File("D:/temp/FAO-IATI/sample-to-work-on/iati_record_ids.txt"),
//                                        new File("D:/temp/FAO-IATI/sample-to-work-on/iati_record_reporting_organizations.csv"));
//        extractAidTypeFromIatiRecords(new File("D:/temp/FAO-IATI/split"), 
//                                      new File("D:/temp/FAO-IATI/sample-to-work-on/iati_record_ids.txt"),
//                                      new File("D:/temp/FAO-IATI/sample-to-work-on/iati_record_reporting_aid_type.csv"));
//        extractActivityStatusFromIatiRecords(new File("D:/temp/FAO-IATI/split"), 
//                                             new File("D:/temp/FAO-IATI/sample-to-work-on/iati_record_ids.txt"),
//                                             new File("D:/temp/FAO-IATI/sample-to-work-on/iati_record_reporting_activity_status.csv"));
//        extractActivityScopeFromIatiRecords(new File("D:/temp/FAO-IATI/split"), 
//                                            new File("D:/temp/FAO-IATI/sample-to-work-on/iati_record_ids.txt"),
//                                            new File("D:/temp/FAO-IATI/sample-to-work-on/iati_record_reporting_activity_scope.csv"));
        extractActivityDatesFromIatiRecords(new File("D:/temp/FAO-IATI/split"), 
                                            new File("D:/temp/FAO-IATI/sample-to-work-on/iati_record_ids.txt"),
                                            new File("D:/temp/FAO-IATI/sample-to-work-on/iati_record_reporting_activity_dates.csv"));
    }
}
