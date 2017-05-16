package ir.ac.iust.dml.kg.raw.rulebased;

import edu.stanford.nlp.pipeline.Annotation;
import ir.ac.iust.dml.kg.raw.TextProcess;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mohammad Abdous md.abdous@gmail.com
 * @version 1.1.0
 * @since 5/14/17 10:27 PM
 */
public class Main {
  public static void main(String[] args) throws IOException {

    String inputPath = "inputText.txt";
    String outputPath = "outputTxt.txt";
    String rulesPath = "tripleRules.txt";

    if (args.length > 0) inputPath = args[0];
    if (args.length > 1) outputPath = args[1];
    if (args.length > 2) rulesPath = args[2];

    if (Files.notExists(Paths.get(inputPath)))
      Files.copy(ExtractTriple.class.getResourceAsStream("/inputText.txt"), Paths.get(inputPath));
    if (Files.notExists(Paths.get(rulesPath)))
      Files.copy(ExtractTriple.class.getResourceAsStream("/tripleRules.txt"), Paths.get(rulesPath));

    List<String> lines = null;
    try {
      lines = FileUtils.readLines(new File(inputPath), "UTF-8");
    } catch (IOException e) {
      e.printStackTrace();
    }

    List<Triple> tripleList = new ArrayList<Triple>();
    TextProcess tp = new TextProcess();
    ExtractTriple extractTriple = RuleFileLoader.load(rulesPath);
    assert extractTriple != null;
    assert lines != null;
    for (String line : lines) {
      Annotation annotation = new Annotation(line);
      tp.preProcess(annotation);
      tripleList.addAll(extractTriple.extractTripleFromAnnotation(annotation));
    }

    TripleJsonProducer.write(tripleList, Paths.get(outputPath));
  }
}
