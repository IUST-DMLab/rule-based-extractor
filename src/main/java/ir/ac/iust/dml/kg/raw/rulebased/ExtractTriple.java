/*
 * Farsi Knowledge Graph Project
 *  Iran University of Science and Technology (Year 2017)
 *  Developed by Mohammad Abdous.
 */

package ir.ac.iust.dml.kg.raw.rulebased;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.tokensregex.Env;
import edu.stanford.nlp.ling.tokensregex.TokenSequenceMatcher;
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import ir.ac.iust.dml.kg.raw.SentenceTokenizer;
import ir.ac.iust.dml.kg.raw.extractor.EnhancedEntityExtractor;
import ir.ac.iust.dml.kg.raw.extractor.IobType;
import ir.ac.iust.dml.kg.raw.extractor.ResolvedEntityToken;
import ir.ac.iust.dml.kg.raw.extractor.ResolvedEntityTokenResource;
import ir.ac.iust.dml.kg.raw.triple.RawTriple;
import ir.ac.iust.dml.kg.resource.extractor.client.ExtractorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExtractTriple {

  private static final Logger LOGGER = LoggerFactory.getLogger(SentenceTokenizer.class);
  private final List<RuleAndPredicate> rules;
  private EnhancedEntityExtractor enhancedEntityExtractor;

  public ExtractTriple(List<RuleAndPredicate> rules) throws IOException {
    ExtractorClient client = new ExtractorClient("http://194.225.227.161:8094");
    this.rules = rules;
    Env environment = TokenSequencePattern.getNewEnv();
    for (RuleAndPredicate rule : rules) {
      rule.setPattern(TokenSequencePattern.compile(environment, rule.getRule()));
    }
    enhancedEntityExtractor = new EnhancedEntityExtractor();

  }

  private List<List<ResolvedEntityToken>> fkgFy(String text) {
    if (enhancedEntityExtractor == null) enhancedEntityExtractor = new EnhancedEntityExtractor();
    final List<List<ResolvedEntityToken>> resolved = enhancedEntityExtractor.extract(text);
    enhancedEntityExtractor.disambiguateByContext(resolved, 0.0011f);
    enhancedEntityExtractor.resolveByName(resolved);
    enhancedEntityExtractor.resolvePronouns(resolved);
    return resolved;
  }

  private List<RawTriple> extractTripleFromSentence(CoreMap sentence, List<ResolvedEntityToken> preResolvedToken) {
    List<RawTriple> triples = new ArrayList<>();
    String sentenceText = sentence.get(CoreAnnotations.TextAnnotation.class);
    // List<MatchedResource> result = client.match(sentenceText);

    if (preResolvedToken == null) {
      List<List<ResolvedEntityToken>> lists = fkgFy(sentenceText);
      annotateEntityType(sentence, lists.get(0));
    } else annotateEntityType(sentence, preResolvedToken);
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
    if (matchedResources == null)
      return;
    try {
      for (int i = 0; i < matchedResources.size(); i++) {
        ResolvedEntityToken matchedResource = matchedResources.get(i);
        if (matchedResource.getResource() != null) {
          String matchedResourceType = getMatchedResourceType(matchedResource);
          if (matchedResource.getIobType().equals(IobType.Beginning))
            coreLabels.get(i).setNER("B_" + matchedResourceType);
          else if (matchedResource.getIobType().equals(IobType.Inside))
            coreLabels.get(i).setNER("I_" + matchedResourceType);

        }
      }
    } catch (Exception ex) {
      LOGGER.trace("Error in annotateEntityType method", ex);
    }

  }

  private String getMatchedResourceType(ResolvedEntityToken matchedResource) {
    StringBuilder builder = new StringBuilder();
    List<ResolvedEntityTokenResource> ambiguities = matchedResource.getAmbiguities();
    for (int i = 0; i < ambiguities.size(); i++) {
      ResolvedEntityTokenResource resolvedEntityTokenResource = ambiguities.get(i);
      if (i <= 1 && resolvedEntityTokenResource.getMainClass() != null)
        builder.append(resolvedEntityTokenResource.getMainClass()
            .replace("http://fkg.iust.ac.ir/ontology/", "")).append(',');
    }
    for (String classStr : matchedResource.getResource().getClasses()) {

      builder.append(classStr.replace("http://fkg.iust.ac.ir/ontology/", "")).append(',');
    }

    //if (builder.length() > 0) builder.setLength(builder.length() - 1);
    builder.append(matchedResource.getResource().getMainClass()
        .replace("http://fkg.iust.ac.ir/ontology/", ""));
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
    return extractTripleFromAnnotation(annotation, null);
  }

  List<RawTriple> extractTripleFromAnnotation(Annotation annotation,
                                              List<List<ResolvedEntityToken>> preResolvedTokens) {
    List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
    List<RawTriple> triples = new ArrayList<>();
    List<RawTriple> sentenceTriples;
    for (int i = 0; i < sentences.size(); i++) {
      CoreMap sentence = sentences.get(i);
      int sentenceLength = sentence.get(CoreAnnotations.TextAnnotation.class).length();
      if (sentenceLength > 20 && sentenceLength < 200) {
        sentenceTriples = extractTripleFromSentence(sentence,
            preResolvedTokens != null ? preResolvedTokens.get(i) : null);
        if (sentenceTriples.size() != 0)
          triples.addAll(sentenceTriples);
      }
    }
    return triples;
  }

  public List<RawTriple> extractTripleFromText(String inputText) {
    return extractTripleFromAnnotation(new Annotation(inputText), null);
  }

}
