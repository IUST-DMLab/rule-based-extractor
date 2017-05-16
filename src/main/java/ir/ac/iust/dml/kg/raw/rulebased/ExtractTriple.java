package ir.ac.iust.dml.kg.raw.rulebased;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.tokensregex.Env;
import edu.stanford.nlp.ling.tokensregex.TokenSequenceMatcher;
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import ir.ac.iust.dml.kg.resource.extractor.client.ExtractorClient;
import ir.ac.iust.dml.kg.resource.extractor.client.MatchedResource;

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

  public ExtractTriple(List<RuleAndPredicate> rules) throws IOException {
    client = new ExtractorClient("http://194.225.227.161:8094");
    this.rules = rules;
    Env environment = TokenSequencePattern.getNewEnv();
    for (RuleAndPredicate rule : rules) {
      System.out.println(rule);
      rule.setPattern(TokenSequencePattern.compile(environment, rule.getRule()));
    }
  }

  public List<Triple> extractTripleFromSentence(CoreMap sentence) {
    List<Triple> triples = new ArrayList<Triple>();
    List<MatchedResource> result = client.match(sentence.get(CoreAnnotations.TextAnnotation.class));
    annotateEntityType(sentence, result);

    for (RuleAndPredicate rule : rules) {
      List<CoreLabel> StanfordTokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
      TokenSequenceMatcher matcher = rule.getPattern().getMatcher(StanfordTokens);
      while (matcher.find()) {
        Triple triple = getTriple(matcher);
        triple.setPredicate(rule.getPredicate());
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
    triple.setSubject(matcher.group("$subject"));
    triple.setObject(matcher.group("$object"));
    return triple;
  }

  List<Triple> extractTripleFromAnnotation(Annotation annotation) {
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

}
