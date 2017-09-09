package ir.ac.iust.dml.kg.raw.rulebased;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.tokensregex.Env;
import edu.stanford.nlp.ling.tokensregex.TokenSequenceMatcher;
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import ir.ac.iust.dml.kg.raw.extractor.EnhancedEntityExtractor;
import ir.ac.iust.dml.kg.raw.extractor.IobType;
import ir.ac.iust.dml.kg.raw.extractor.ResolvedEntityToken;
import ir.ac.iust.dml.kg.raw.extractor.ResolvedEntityTokenResource;
import ir.ac.iust.dml.kg.raw.triple.RawTriple;
import ir.ac.iust.dml.kg.resource.extractor.client.ExtractorClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mohammad Abdous md.abdous@gmail.com
 * @version 1.1.0
 * @since 5/4/17 11:51 AM
 */
public class ExtractTriple {

    private final ExtractorClient client;
    private final List<RuleAndPredicate> rules;
    EnhancedEntityExtractor enhancedEntityExtractor;

    public ExtractTriple(List<RuleAndPredicate> rules) throws IOException {
        client = new ExtractorClient("http://194.225.227.161:8094");
        this.rules = rules;
        Env environment = TokenSequencePattern.getNewEnv();
        for (RuleAndPredicate rule : rules) {
            rule.setPattern(TokenSequencePattern.compile(environment, rule.getRule()));
        }
        enhancedEntityExtractor = new EnhancedEntityExtractor();

    }

    public List<List<ResolvedEntityToken>> fkgFy(String text) {
        if (enhancedEntityExtractor == null) enhancedEntityExtractor = new EnhancedEntityExtractor();
        final List<List<ResolvedEntityToken>> resolved = enhancedEntityExtractor.extract(text);
        enhancedEntityExtractor.disambiguateByContext(resolved, 0.0011f);
        enhancedEntityExtractor.resolveByName(resolved);
        enhancedEntityExtractor.resolvePronouns(resolved);
        return resolved;
    }

    public List<RawTriple> extractTripleFromSentence(CoreMap sentence) {
        List<RawTriple> triples = new ArrayList<RawTriple>();
        String sentenceText = sentence.get(CoreAnnotations.TextAnnotation.class);
        // List<MatchedResource> result = client.match(sentenceText);

        List<List<ResolvedEntityToken>> lists = fkgFy(sentenceText);
        annotateEntityType(sentence, lists.get(0));
        for (RuleAndPredicate rule : rules) {
            List<CoreLabel> StanfordTokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            TokenSequenceMatcher matcher = rule.getPattern().getMatcher(StanfordTokens);
            while (matcher.find()) {
                RawTriple triple = getTriple(matcher);
                triple.setPredicate(rule.getPredicate());
                triple.setSourceUrl(sentence.get(CoreAnnotations.TextAnnotation.class));
                triple.setModule("RuleBased");
                triple.setExtractionTime(System.currentTimeMillis());
                triples.add(triple);
            }
        }
        return triples;
    }

    private void annotateEntityType(CoreMap sentence, List<ResolvedEntityToken> matchedResources) {
        List<CoreLabel> coreLabels = sentence.get(CoreAnnotations.TokensAnnotation.class);
        for (CoreLabel coreLabel : coreLabels)
            coreLabel.setNER("O");
        int sentenceSize = sentence.get(CoreAnnotations.TokensAnnotation.class).size();
        if (matchedResources == null)
            return;
        try {
            int i = 0;
            for (ResolvedEntityToken matchedResource : matchedResources) {
                if (matchedResource.getResource() != null) {
                    String matchedResourceType = getMatchedResourceType(matchedResource);
                    if (matchedResource.getIobType().equals(IobType.Beginning))
                        coreLabels.get(i).setNER("B_" + matchedResourceType);
                    else if (matchedResource.getIobType().equals(IobType.Inside))
                        coreLabels.get(i).setNER("I_" + matchedResourceType);

                }
                i++;
            }

        } catch (Exception ex)

        {
            System.out.println(ex.getMessage());
        }

    }

    private String getMatchedResourceType(ResolvedEntityToken matchedResource) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (ResolvedEntityTokenResource resolvedEntityTokenResource : matchedResource.getAmbiguities()) {
            if (i <= 1)
                builder.append(resolvedEntityTokenResource.getMainClass().replace("http://fkg.iust.ac.ir/ontology/", "") + ',');
            i++;
        }
        for (String classStr : matchedResource.getResource().getClasses()) {

            builder.append(classStr.replace("http://fkg.iust.ac.ir/ontology/", "") + ',');
        }

        //if (builder.length() > 0) builder.setLength(builder.length() - 1);
        builder.append(matchedResource.getResource().getMainClass().replace("http://fkg.iust.ac.ir/ontology/", "").toString());
        return builder.toString();
    }

    private RawTriple getTriple(TokenSequenceMatcher matcher) {
        RawTriple triple = new RawTriple();
        String url = "fkgr:";
        triple.setSubject(url + matcher.group("$subject"));
        triple.setObject(url + matcher.group("$object"));
        //triple.setSubject(matcher.groupInfo("$subject").nodes.get(0).get(CoreAnnotations.AbbrAnnotation.class));
        // triple.setSubject(matcher.groupInfo("$object").nodes.get(0).get(CoreAnnotations.AbbrAnnotation.class));
        triple.setAccuracy(0.1);
        triple.needsMapping(true);

        return triple;
    }

    public List<RawTriple> extractTripleFromAnnotation(Annotation annotation) {
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        List<RawTriple> triples = new ArrayList<RawTriple>();
        List<RawTriple> sentenceTriples;
        for (CoreMap sentence : sentences) {
            int sentenceLength = sentence.get(CoreAnnotations.TextAnnotation.class).length();
            if (sentenceLength > 20 && sentenceLength < 200) {
                sentenceTriples = extractTripleFromSentence(sentence);
                if (sentenceTriples.size() != 0)
                    triples.addAll(sentenceTriples);
            }
        }
        return triples;
    }

    public List<RawTriple> extractTripleFromText(String inputText) {
        return extractTripleFromAnnotation(new Annotation(inputText));
    }

}
