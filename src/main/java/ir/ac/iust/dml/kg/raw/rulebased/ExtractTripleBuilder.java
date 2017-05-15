package ir.ac.iust.dml.kg.raw.rulebased;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ExtractTripleBuilder {

  private final static Charset utf = Charset.forName("UTF-8");

  public static ExtractTriple getFromFile(String rulesPath, String predicatesPath) {
    try {
      return new ExtractTriple(
          Files.readAllLines(Paths.get(rulesPath), utf),
          Files.readAllLines(Paths.get(predicatesPath), utf)
      );
    } catch (IOException e) {
      return null;
    }
  }
}
