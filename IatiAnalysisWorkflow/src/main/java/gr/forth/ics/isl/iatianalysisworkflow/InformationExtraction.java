package gr.forth.ics.isl.iatianalysisworkflow;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Properties;
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
            if(csvRecord.get(0).equalsIgnoreCase("identifier")){
                continue;
            }
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
    
    public void extractEcosystemInformationUsingLLM(File datasetFileCsv) throws FileNotFoundException, UnsupportedEncodingException, IOException{
        log.info("Start extracting information using LLM");
        CSVParser csvParser= CSVFormat.DEFAULT.parse(new InputStreamReader(new FileInputStream(datasetFileCsv), "UTF-8"));
        for(CSVRecord csvRecord : csvParser){
            if(csvRecord.get(0).equalsIgnoreCase("identifier")){
                continue;
            }
            this.classifyUsingLlm(csvRecord.get(0),csvRecord.get(1)+" - "+csvRecord.get(2));
        }
    }
        
    private void classifyUsingLlm(String recordId, String text) throws FileNotFoundException, IOException{
        String prompt="You are given ONE IATI activity record containing an identifier, title, and description.\n" +
                        "\n" +
                        "Your task is to determine which natural ecosystem realm or realms are substantively addressed by the activity.\n" +
                        "\n" +
                        "Allowed classifications are:\n" +
                        "\n" +
                        "TERRESTRIAL\n" +
                        "FRESHWATER\n" +
                        "MARINE\n" +
                        "TERRESTRIAL+FRESHWATER\n" +
                        "TERRESTRIAL+MARINE\n" +
                        "FRESHWATER+MARINE\n" +
                        "TERRESTRIAL+FRESHWATER+MARINE\n" +
                        "UNKNOWN\n" +
                        "\n" +
                        "Classification principles:\n" +
                        "\n" +
                        "1. Use ONLY the information contained in the activity record.\n" +
                        "\n" +
                        "2. Classify an ecosystem only when the title or description provides sufficient evidence that the activity substantively concerns that natural ecosystem.\n" +
                        "\n" +
                        "3. Classify the ecosystem that is addressed by the activity, not merely the physical location or sector in which the activity takes place.\n" +
                        "\n" +
                        "4. Do NOT classify an activity as TERRESTRIAL solely because it concerns:\n" +
                        "   - agriculture\n" +
                        "   - crops\n" +
                        "   - farmers\n" +
                        "   - livestock\n" +
                        "   - poultry\n" +
                        "   - food production\n" +
                        "   - rural development\n" +
                        "\n" +
                        "   These should be classified as TERRESTRIAL only when the text also indicates a substantive relationship with terrestrial ecosystems, habitats, land, soils, forests, grasslands, biodiversity, ecosystem restoration, or comparable ecological features.\n" +
                        "\n" +
                        "5. Do NOT classify an activity as FRESHWATER solely because it concerns:\n" +
                        "   - drinking water\n" +
                        "   - water supply\n" +
                        "   - sanitation\n" +
                        "   - WASH\n" +
                        "   - wastewater\n" +
                        "   - irrigation\n" +
                        "   - water scarcity\n" +
                        "   - water infrastructure\n" +
                        "\n" +
                        "   These should be classified as FRESHWATER only when the activity substantively concerns natural freshwater ecosystems such as rivers, lakes, streams, inland wetlands, watersheds, freshwater habitats, or freshwater biodiversity.\n" +
                        "\n" +
                        "6. Do NOT classify an activity as MARINE solely because it takes place in a coastal country, island state, coastal city, or fishing community.\n" +
                        "\n" +
                        "   Classify MARINE when the text substantively concerns seas, oceans, coastal waters, marine habitats, coral reefs, seagrass, marine biodiversity, marine fisheries, or comparable marine ecological systems.\n" +
                        "\n" +
                        "7. Aquaculture or fisheries alone are not sufficient to distinguish between FRESHWATER and MARINE.\n" +
                        "   - If the text specifies freshwater/inland waters, return FRESHWATER.\n" +
                        "   - If the text specifies marine/coastal/sea/ocean environments, return MARINE.\n" +
                        "   - If the aquatic environment cannot be determined, return UNKNOWN.\n" +
                        "\n" +
                        "8. Terms such as the following are NOT sufficient on their own for ecosystem classification:\n" +
                        "   - climate\n" +
                        "   - climate change\n" +
                        "   - sustainability\n" +
                        "   - resilience\n" +
                        "   - environment\n" +
                        "   - environmental\n" +
                        "   - biodiversity\n" +
                        "   - nature-based solutions\n" +
                        "   - pollution\n" +
                        "   - carbon\n" +
                        "   - circular economy\n" +
                        "   - green development\n" +
                        "\n" +
                        "   Use them only when the surrounding text identifies the relevant ecosystem realm.\n" +
                        "\n" +
                        "9. The word \"ecosystem\" may be used metaphorically. Expressions such as:\n" +
                        "   - business ecosystem\n" +
                        "   - innovation ecosystem\n" +
                        "   - digital ecosystem\n" +
                        "   - financial ecosystem\n" +
                        "   - entrepreneurial ecosystem\n" +
                        "   - philanthropic ecosystem\n" +
                        "\n" +
                        "   do NOT refer to natural ecosystems and should not trigger an ecosystem classification.\n" +
                        "\n" +
                        "10. Typical strong evidence includes:\n" +
                        "\n" +
                        "TERRESTRIAL:\n" +
                        "forest, woodland, grassland, savanna, desert, mountain habitat, terrestrial habitat, land ecosystem, soil ecosystem, terrestrial biodiversity.\n" +
                        "\n" +
                        "FRESHWATER:\n" +
                        "river, lake, stream, freshwater wetland, inland water, freshwater habitat, freshwater biodiversity, freshwater fishery.\n" +
                        "\n" +
                        "MARINE:\n" +
                        "sea, ocean, marine habitat, coral reef, seagrass, marine biodiversity, coastal waters, marine fishery.\n" +
                        "\n" +
                        "11. Some features may connect more than one ecosystem. For example:\n" +
                        "   - forest + river → TERRESTRIAL+FRESHWATER\n" +
                        "   - terrestrial habitat + coastal/marine habitat → TERRESTRIAL+MARINE\n" +
                        "   - river-estuary-marine systems → FRESHWATER+MARINE\n" +
                        "\n" +
                        "Return multiple classes only when the text provides evidence for each one.\n" +
                        "\n" +
                        "12. Do not infer an ecosystem merely from geographical knowledge that is not present in the activity record.\n" +
                        "\n" +
                        "For example, do not infer that a named location contains forests, rivers, wetlands, or marine areas unless the activity text itself provides that information.\n" +
                        "\n" +
                        "13. If an activity clearly concerns environmental protection, ecosystem restoration, biodiversity, or climate adaptation but the text does not provide enough information to determine whether the relevant ecosystem is terrestrial, freshwater, or marine, return:\n" +
                        "\n" +
                        "UNKNOWN\n" +
                        "\n" +
                        "14. If the evidence is weak, ambiguous, indirect, or insufficient, return:\n" +
                        "\n" +
                        "UNKNOWN\n" +
                        "\n" +
                        "Prefer UNKNOWN over speculative classification.\n" +
                        "\n" +
                        "OUTPUT REQUIREMENTS:\n" +
                        "\n" +
                        "Return ONLY one of the allowed classifications.\n" +
                        "\n" +
                        "Do not provide:\n" +
                        "- explanations\n" +
                        "- reasoning\n" +
                        "- confidence scores\n" +
                        "- punctuation\n" +
                        "- JSON\n" +
                        "- markdown\n" +
                        "- additional text\n" +
                        "\n" +
                        "Activity record:\n" +
                        "\n" +
                        "{{ACTIVITY_RECORD}}" +text ;

        
        Properties properties=new Properties();
        try(FileInputStream fileInputStream=new FileInputStream("config.properties")){
            properties.load(fileInputStream);
        }
        
        OpenAIClient client=OpenAIOkHttpClient.builder().apiKey(properties.getProperty("openai.api.key")).build();
        ResponseCreateParams params=ResponseCreateParams.builder().model(ChatModel.GPT_5_6_LUNA).input(prompt).build();
        Response response=client.responses().create(params);
        String result=response.output().stream()
                .flatMap(output -> output.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(content -> content.outputText().stream())
                .map(outputText -> outputText.text())
                .reduce("", String::concat);
                
        Results.flushCSVResults(Arrays.asList(recordId,result), new File(Resources.ECOSYSTEMS_CLASSIFICATION_RESULTS_LLM));
    }

}