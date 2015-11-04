package edu.brandeis.cs.lappsgrid.clearnlp;

import edu.brandeis.cs.lappsgrid.Version;
import edu.emory.clir.clearnlp.component.AbstractComponent;
import edu.emory.clir.clearnlp.component.mode.dep.DEPConfiguration;
import edu.emory.clir.clearnlp.component.mode.srl.SRLConfiguration;
import edu.emory.clir.clearnlp.component.utils.GlobalLexica;
import edu.emory.clir.clearnlp.component.utils.NLPUtils;
import edu.emory.clir.clearnlp.dependency.DEPTree;
import edu.emory.clir.clearnlp.tokenization.AbstractTokenizer;
import edu.emory.clir.clearnlp.util.lang.TLanguage;
import org.apache.xerces.impl.io.UTF8Reader;
import org.lappsgrid.api.WebService;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import static org.lappsgrid.discriminator.Discriminators.Uri;


/**
 * @author krim@brandeis.edu
 *
 */

@org.lappsgrid.annotations.CommonMetadata(

        vendor = "http://www.cs.brandeis.edu/",
        license = "apache2",
        allow = "any",
        language = { "en" }
//        version = "2.0.1-SNAPSHOT" // this will automatically be extracted from POM, later
)
public abstract class AbstractClearNLPWebService implements WebService {

    protected static final Logger log
            = LoggerFactory.getLogger(AbstractClearNLPWebService.class);


    static protected ConcurrentHashMap<String, AbstractComponent> cache = new ConcurrentHashMap<>();

    public static final String MORPH = "MORPH";
    public static final String POS = "POS";
    public static final String NER = "NER";
    public static final String DEP = "DEP";
    public static final String SRL = "SRL";

    public static final String TOKEN_ID = "tk_";
    public static final String SENT_ID = "s_";
    public static final String CONSTITUENT_ID = "c_";
    public static final String PS_ID = "ps_";
    public static final String DEPENDENCY_ID = "dep_";
    public static final String DS_ID = "ds_";
    public static final String MENTION_ID = "m_";
    public static final String COREF_ID = "coref_";
    public static final String NE_ID = "ne_";


    private String metadata;
    protected TLanguage lang;
    protected AbstractTokenizer tokenizer;
    protected List<AbstractComponent> components;


    /**
     * Default constructor.
     * By default, initiating any service will load up the tokenizer for English
     * and distributional semantics model, which will be used globally.
     */
    public AbstractClearNLPWebService() {
        lang = TLanguage.ENGLISH;
        tokenizer  = NLPUtils.getTokenizer(lang);
        components = new ArrayList<>();

        loadDistSemWords();

        try {
            loadMetadata();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Load Distributional Semantics model into memory. This will be use globally.
     */
    protected void loadDistSemWords() {
        log.info("Loading DS");
        List<String> paths = new ArrayList<>();
        paths.add("brown-rcv1.clean.tokenized-CoNLL03.txt-c1000-freq1.txt.xz");
        GlobalLexica.initDistributionalSemanticsWords(paths);
        log.info("DS loaded");
    }

    /**
     * Load Named Entity Dictionary. This will be used in NER only.
     * NE dict is very large and takes up a couple gig of memory.
     */
    protected void loadNamedEntityDict() {
        log.info("Loading NE dict");
        GlobalLexica.initNamedEntityDictionary("general-en-ner-gazetteer.xz");
        log.info("NE dict loaded");
    }

    /**
     * Get version from metadata
     */
    String getVersion() {
        return Version.getVersion();
    }

    /**
     * Load up cNLP components using bundled models
     */
    protected AbstractComponent loadComponent(String componentName) {

        AbstractComponent c;
        switch (componentName) {
            case MORPH:
                c = NLPUtils.getMPAnalyzer(lang);
                log.info("MORPH loaded");
                break;
            case POS:
                c = NLPUtils.getPOSTagger(lang, "general-en-pos.xz");
                log.info("POS loaded");
                break;
            case DEP:
                c = NLPUtils.getDEPParser(lang, "general-en-dep.xz",
                        new DEPConfiguration("root"));
                log.info("DEP loaded");
                break;
            case SRL:
                c= NLPUtils.getSRLabeler(lang, "general-en-srl.xz",
                        new SRLConfiguration(4, 3));
                log.info("SRL loaded");
                break;
            case NER:
                c= NLPUtils.getNERecognizer(lang, "general-en-ner.xz");
                log.info("NER loaded");
                loadNamedEntityDict();
                break;
            default:
                return null;
        }
        return c;
    }

    /**
     * Retrieve cNLP components from cached hash, if fails, build one.
     */
    protected void getComponent(String componentName) {
        log.info(String.format("Retriveing from cache: %s", componentName));
        AbstractComponent component = cache.get(componentName);
        if (component == null) {
            log.info(String.format("No cached found, newly caching: %s", componentName));
            component = loadComponent(componentName);
            cache.put(componentName, component);
            log.info(String.format("Cached: %s", componentName));
        }
        components.add(component);
    }

    /**
     * Generates a LEDS json with error message.
     * @param message
     * @return
     */
    protected String errorLEDS(String message) {
        return new Data<>(Uri.ERROR, message).asJson();
    }

    @Override
    /**
     * This is default execute: takes a json, wrap it as a LIF, run modules
     */
    public String execute(String input) {
        if (input == null) {
            log.error("Input is null");
            return errorLEDS("Input is null");
        }
        Data leds;
        leds = Serializer.parse(input, Data.class);
        if (leds ==  null) {
            leds = new Data();
            leds.setDiscriminator(Uri.TEXT);
            leds.setPayload(input);
        }

        final String discriminator = leds.getDiscriminator();
        Container lif;

        switch (discriminator) {
            case Uri.ERROR:
                log.info("Input contains ERROR");
                return input;
            case Uri.JSON_LD:
                log.info("Input contains LIF");
                lif = new Container((Map) leds.getPayload());
                break;
            case Uri.LIF:
                log.info("Input contains LIF");
                lif = new Container((Map) leds.getPayload());
                break;
            case Uri.TEXT:
                log.info("Input contains TEXT");
                lif = new Container();
                lif.setText((String) leds.getPayload());
                lif.setLanguage("en");
                // return empty metadata for process result (for now)
//                cont.setMetadata((Map) Serializer.parse(
//                        this.getMetadata(), Data.class).getPayload());
                break;
            default:
                String unsupported = String.format(
                        "Unsupported discriminator type: %s", discriminator);
                log.error(unsupported);
                return errorLEDS(unsupported);
        }

        try {
            // TODO 151103 this will be redundant when @context stuff sorted out
            lif.setContext(Container.REMOTE_CONTEXT);
            return execute(lif);
        } catch (Throwable th) {
            th.printStackTrace();
            log.error("Error processing input", th.toString());
            return errorLEDS(String.format(
                    "Error processing input: %s", th.toString()));
        }
    }

    /**
     * This will be overridden for each module
     * TODO 151103 need a specific exception class
     */
    public abstract String execute(Container json) throws Exception;

    /**
     * this will be called within execute()
     */
    protected List<DEPTree> process(String input) {
        List<List<String>> sentences = split(input);
        List<DEPTree> processed = new ArrayList<>();
        for (List<String> sentence : sentences) {
            DEPTree depTree = new DEPTree(sentence);
            for (AbstractComponent component : components) {
                component.process(depTree);
            }
            processed.add(depTree);
        }
        return processed;

    }

    /**
     * Splitter + Tokenizer in cNLP
     * @param input text to process
     * @return a list sentences, a sentence is a list of tokens
     */
    public List<List<String>> split(String input) {
        InputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return tokenizer.segmentize(is);
    }

    /**
     * From token list, generate a list of start-end pairs of all tokens
     * TODO 151104 current implementation performs tokenization twice
     * TODO (another in split()) remove this redundancy
     * @param input text to process
     * @return list of [start, end] arrays
     * @throws IOException
     */
    public List<int[]> getSpans(String input) throws IOException {
        List<int[]> allSpans = new ArrayList<>();
        List<String> allTokens = tokenizer.tokenize(input);
        int tid = 0;
        int start = 0;
        for (String token : allTokens) {
            start = input.indexOf(token, start);
            if (start < 0) {
                String message = "Tokenizer failed to get span of: " + token;
                log.error(message);
                throw new IOException(message);
            }
            int end = start + token.length();
            allSpans.add(new int[]{start, end});
        }
        return allSpans;
    }

    /**
     * Load metadata from compiler generated json files.
     * @throws IOException when metadata json file was not found.
     */
    public void loadMetadata() throws IOException {
        // get caller name using reflection
        String serviceName = this.getClass().getName();
        String resUri = String.format("/metadata/%s.json", serviceName);
        log.info("load resources:" + resUri);
        InputStream inputStream = this.getClass().getResourceAsStream(resUri);

        if (inputStream == null) {
            String message = "Unable to load metadata file for " + serviceName;
            log.error(message);
            throw new IOException(message);
        } else {
            UTF8Reader reader = new UTF8Reader(inputStream);
            try {
                Scanner s = new Scanner(reader).useDelimiter("\\A");
                String metadataText = s.hasNext() ? s.next() : "";
                metadata = (new Data<>(Uri.META,
                        Serializer.parse(metadataText, ServiceMetadata.class))).asPrettyJson();
            } catch (Exception e) {
                String message = "Unable to parse json for " + this.getClass().getName();
                log.error(message, e);
                metadata = (new Data<>(Uri.ERROR, message)).asPrettyJson();
            }
            reader.close();
        }
    }

    @Override
    public String getMetadata() {
        return this.metadata;
    }

    /* ================= some helpers ================== */
    /**
     * Generates ID string
     */
    protected static String makeID(String type, int sid, int tid) {
        return String.format("%s%d_%d", type, sid, tid);
    }

    /**
     * Generates ID string
     */
    protected static String makeID(String type, int id) {
        return String.format("%s%d", type, id);

    }

}

