import edu.stanford.nlp.pipeline.Annotation;
import ir.ac.iust.dml.kg.raw.TextProcess;
import ir.ac.iust.dml.kg.raw.coreference.CorefUtility;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mohammad Abdous md.abdous@gmail.com
 * @version 1.1.0
 * @since 5/4/17 12:14 PM
 */
public class ExtractTripleTest {

    @Test
    public void testExtractTriple() {

        List<String> lines = new CorefUtility().readListedFile(ExtractTripleTest.class, "/inputText.txt");
        lines.remove(0);
        List<Triple> tripleList = new ArrayList<Triple>();
        TextProcess tp = new TextProcess();
        ExtractTriple extractTriple = new ExtractTriple();
        for (String line : lines) {
            Annotation annotation = new Annotation(line);
            tp.preProcess(annotation);

            tripleList.addAll(extractTriple.extractTripleFromAnnotation(annotation));
        }
        System.out.println(tripleList.toString());
    }
}
