package ir.ac.iust.dml.kg.raw.rulebased;

import edu.stanford.nlp.pipeline.Annotation;
import ir.ac.iust.dml.kg.raw.TextProcess;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mohammad Abdous md.abdous@gmail.com
 * @version 1.1.0
 * @since 5/4/17 12:14 PM
 */
public class ExtractTripleTest {

  @Test
  public void testExtractTriple() throws IOException {


    String inputPath = "inputText.txt";
    String rulesPath = "tripleRules.txt";
    String predicatesPath = "predicates.txt";

    if (Files.notExists(Paths.get(inputPath)))
      Files.copy(ExtractTriple.class.getResourceAsStream("/inputText.txt"), Paths.get(inputPath));
    if (Files.notExists(Paths.get(rulesPath)))
      Files.copy(ExtractTriple.class.getResourceAsStream("/tripleRules.txt"), Paths.get(rulesPath));
    if (Files.notExists(Paths.get(predicatesPath)))
      Files.copy(ExtractTriple.class.getResourceAsStream("/predicates.txt"), Paths.get(predicatesPath));

    List<String> lines = Files.readAllLines(Paths.get(inputPath), Charset.forName("UTF-8"));
    for (String line : lines) {
      System.out.println("سلام: " + line);
    }
    lines.remove(0);
    List<Triple> tripleList = new ArrayList<Triple>();
    TextProcess tp = new TextProcess();
    ExtractTriple extractTriple = ExtractTripleBuilder.getFromFile(rulesPath, predicatesPath);
    for (String line : lines) {
      Annotation annotation = new Annotation(line);
      tp.preProcess(annotation);

      tripleList.addAll(extractTriple.extractTripleFromAnnotation(annotation));
    }
    System.out.println(tripleList.toString());
  }
}
