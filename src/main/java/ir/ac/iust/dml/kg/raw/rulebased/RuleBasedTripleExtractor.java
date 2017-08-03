package ir.ac.iust.dml.kg.raw.rulebased;

import edu.stanford.nlp.pipeline.Annotation;
import ir.ac.iust.dml.kg.raw.SentenceTokenizer;
import ir.ac.iust.dml.kg.raw.TextProcess;
import ir.ac.iust.dml.kg.raw.services.access.entities.Rule;
import ir.ac.iust.dml.kg.raw.services.access.repositories.RuleRepository;
import ir.ac.iust.dml.kg.raw.triple.RawTriple;
import ir.ac.iust.dml.kg.raw.triple.RawTripleExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mohammad on 7/23/2017.
 */
@Service
public class RuleBasedTripleExtractor implements RawTripleExtractor {
    @Autowired
    private RuleRepository ruleDao;
    List<RuleAndPredicate> mainRuleAndPredicates;

    @PostConstruct
    void init() {
        List<Rule> rules = ruleDao.findAll();
        for (Rule rule : rules) {
            RuleAndPredicate ruleAndPredicate = new RuleAndPredicate();
            ruleAndPredicate.setRule(rule.getRule());
            ruleAndPredicate.setPredicate(rule.getPredicate());
            mainRuleAndPredicates.add(ruleAndPredicate);
        }
    }

    @Override
    public List<RawTriple> extract(String source, String version, String inputText) {

        List<RawTriple> result = new ArrayList<>();
        ExtractTriple extractTriple = null;
        try {
            extractTriple = new ExtractTriple(mainRuleAndPredicates);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final List<String> lines = SentenceTokenizer.SentenceSplitterRaw(inputText);
        TextProcess tp = new TextProcess();
        for (String line : lines) {
            Annotation annotation = new Annotation(line);
            tp.preProcess(annotation);
            result.addAll(extractTriple.extractTripleFromAnnotation(annotation));
        }
        return result;
    }
}
