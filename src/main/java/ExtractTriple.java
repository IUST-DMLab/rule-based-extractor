import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.tokensregex.Env;
import edu.stanford.nlp.ling.tokensregex.TokenSequenceMatcher;
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import ir.ac.iust.dml.kg.raw.TextProcess;
import ir.ac.iust.dml.kg.raw.coreference.CorefUtility;
import ir.ac.iust.dml.kg.resource.extractor.client.ExtractorClient;
import ir.ac.iust.dml.kg.resource.extractor.client.MatchedResource;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Mohammad Abdous md.abdous@gmail.com
 * @version 1.1.0
 * @since 5/4/17 11:51 AM
 */
public class ExtractTriple {

    final ExtractorClient client;
    final List<String> rules;
    Map<String, String> dictionary = new HashMap<String, String>();
    final HttpClientBuilder builder = HttpClientBuilder.create();
    final Gson gson = new Gson();
    final String address;
    final Type listType = new TypeToken<List<MatchedResource>>() {
    }.getType();
    final List<TokenSequencePattern> patterns;


    ExtractTriple() {
        client = new ExtractorClient("http://194.225.227.161:8094");
        rules = new CorefUtility().readListedFile(ExtractTriple.class, "/tripleRules.txt");
        rules.remove(0);
        patterns = new ArrayList<TokenSequencePattern>();
        Env environment = TokenSequencePattern.getNewEnv();
        for (String rule : rules) {
            patterns.add(TokenSequencePattern.compile(environment, rule));
        }
        dictionary = fillPredictDictionary();
        this.address = "http://194.225.227.161:8091";
    }

    private Map<String, String> fillPredictDictionary() {
        List<String> predictionList = new CorefUtility().readListedFile(ExtractTriple.class, "/predictMap.txt");
        Map<String, String> dictionary = new HashMap<String, String>();
        for (String line : predictionList) {
            String[] strs = line.split(":");
            dictionary.put(strs[0], strs[1]);
        }
        return dictionary;
    }

    public List<Triple> extractTripleFromSentence(CoreMap sentence) {
        List<Triple> triples = new ArrayList<Triple>();
        Triple triple = new Triple();
        List<MatchedResource> result = client.match(sentence.get(CoreAnnotations.TextAnnotation.class));
        annotateEntityType(sentence, result);


        for (TokenSequencePattern pattern : patterns) {

            List<CoreLabel> StanfordTokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            TokenSequenceMatcher matcher = pattern.getMatcher(StanfordTokens);

            while (matcher.find()) {
                triple = getTriple(matcher);
                triple.setSentence(sentence.get(CoreAnnotations.TextAnnotation.class));
                triples.add(triple);
            }
        }

        return triples;

    }

    private void annotateEntityType(CoreMap sentence, List<MatchedResource> matchedResources) {
        List<CoreLabel> coreLabels = sentence.get(CoreAnnotations.TokensAnnotation.class);
        for (CoreLabel coreLabel : coreLabels)
            coreLabel.setNER("");
        for (MatchedResource matchedResource : matchedResources) {
            if (matchedResource.getAmbiguities().size() == 0 && matchedResource.getResource().getClassTree().size() > 0) {
                int tokenBeginIndex = matchedResource.getStart();
                int tokenEndIndex = matchedResource.getEnd();
                String matchedResourceType = getMatchedResourceType(matchedResource);
                for (int i = tokenBeginIndex; i <= tokenEndIndex; i++) {
                    if (i == tokenBeginIndex)
                        coreLabels.get(i).setNER("B_" + matchedResourceType);
                    else
                        coreLabels.get(i).setNER("I_" + matchedResourceType);
                }
            }

        }
    }

    private String getMatchedResourceType(MatchedResource matchedResource) {
        StringBuilder builder = new StringBuilder();
        matchedResource.getResource().getClassTree().forEach(it ->
                builder.append(it.replace("fkgo:", "").replace("http://fkg.iust.ac.ir/ontology", ""))
                        .append(',')
        );
        if (builder.length() > 0) builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    private Triple getTriple(TokenSequenceMatcher matcher) {
        Triple triple = new Triple();
        //  SequenceMatchResult.MatchedGroupInfo<edu.stanford.nlp.util.CoreMap> coreMaps = matcher.groupInfo("$object");

        triple.setSubject(matcher.group("$subject"));
        triple.setPredicate(matcher.group("$predicate"));
        triple.setObject(matcher.group("$object"));

        // predicate = normalizePrediction(predicate);
        // List<String> predicates = getPredicate(triple.object, triple.subject);

        return triple;
    }

    private List<String> getPredicate(String object, String subject) {
        List<String> predicates = new ArrayList<String>();
        try {
            final HttpGet request = new HttpGet(
                    address + "/rs/v1/triples/search?subject="
                            + "http://fkg.iust.ac.ir/resources/" + URLEncoder.encode(subject.replace(" ", "_"), "UTF-8") + "&object="
                            + "http://fkg.iust.ac.ir/resources/" + URLEncoder.encode(object.replace(" ", "_"), "UTF-8") + "&page=0&pageSize=10"
            );
            request.addHeader("accept", "application/json");
            builder.build();
            try (CloseableHttpClient client = builder.build()) {
                final HttpResponse result = client.execute(request);
                String json_string = EntityUtils.toString(result.getEntity());
                JSONObject temp1 = new JSONObject(json_string);
                JSONArray data = temp1.getJSONArray("data");
                for (int i = 0; i < data.length(); i++) {
                    JSONObject o = data.getJSONObject(i);
                    predicates.add(o.getString("predicate"));
                }
                return predicates;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String normalizePrediction(String prediction) {


        return null;
    }

    public List<Triple> extractTripleFromAnnotation(Annotation annotation) {
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        List<Triple> triples = new ArrayList<Triple>();
        List<Triple> sentenceTriples;
        for (CoreMap sentence : sentences) {
            sentenceTriples = extractTripleFromSentence(sentence);
            if (sentenceTriples.size() != 0)
                triples.addAll(sentenceTriples);
        }
        return triples;
    }

    public static void main(String[] args) {

        String inputPath ="D://inputText.txt";// args[0].toString();
        String outputPath ="D://outputTxt.txt";// args[1].toString();

        List<String> lines = null;
        try {
            lines = FileUtils.readLines(new File(inputPath), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        lines.remove(0);
        List<Triple> tripleList = new ArrayList<Triple>();
        TextProcess tp = new TextProcess();
        ExtractTriple extractTriple = new ExtractTriple();
        for (String line : lines) {
            Annotation annotation = new Annotation(line);
            tp.preProcess(annotation);

            tripleList.addAll(extractTriple.extractTripleFromAnnotation(annotation));
        }


        String tripleJsons = generateTripleJson(tripleList);
        try {
            FileUtils.write(new File(outputPath), tripleJsons);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static String generateTripleJson(List<Triple> tripleList) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<TripleJson> tripleJsons = new ArrayList<TripleJson>();
        String wikiUrl = "https://fa.wikipedia.org/wiki/";
        for (Triple triple : tripleList) {
            TripleJson tripleJson = new TripleJson();


            tripleJson.setSubject(wikiUrl + triple.getSubject().replace(" ", "_"));
            tripleJson.setObject(wikiUrl + triple.getObject().replace(" ", "_"));
            tripleJson.setPredicate(triple.getPredicate());
            tripleJson.setSource(triple.getSentence());
            tripleJsons.add(tripleJson);
        }

        String json = gson.toJson(tripleJsons);
        return json;
    }
}
