package ir.ac.iust.dml.kg.raw.rulebased;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ir.ac.iust.dml.kg.raw.triple.RawTriple;
import ir.ac.iust.dml.kg.raw.utils.PrefixService;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class TripleJsonProducer {

  static String generateTripleJson(List<RawTriple> tripleList) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    List<TripleJson> tripleJsons = new ArrayList<TripleJson>();
    final String resourcePrefix = PrefixService.INSTANCE.prefixToUri("fkgr");
    for (RawTriple triple : tripleList) {
      TripleJson tripleJson = new TripleJson();


      tripleJson.setSubject(resourcePrefix + triple.getSubject().replace(" ", "_"));
      tripleJson.setObject(resourcePrefix + triple.getObject().replace(" ", "_"));
      tripleJson.setPredicate(PrefixService.INSTANCE.prefixToUri(triple.getPredicate()));
      tripleJson.setSource(triple.getSourceUrl());
      tripleJsons.add(tripleJson);
    }

    return gson.toJson(tripleJsons);
  }

  static void write(List<RawTriple> tripleList, Path path) {
    try {
      System.out.println("writing to file: " + path.toAbsolutePath());
      FileUtils.write(path.toFile(), generateTripleJson(tripleList), "UTF-8", false);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
