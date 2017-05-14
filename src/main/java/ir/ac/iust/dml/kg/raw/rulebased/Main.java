package ir.ac.iust.dml.kg.raw.rulebased;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
        String predicatesPath = "predicates.txt";

        if (args.length > 0) inputPath = args[0];
        if (args.length > 1) inputPath = args[1];
        if (args.length > 2) rulesPath = args[2];
        if (args.length > 3) predicatesPath = args[3];

        if (Files.notExists(Paths.get(inputPath)))
            Files.copy(ExtractTriple.class.getResourceAsStream("/inputText.txt"), Paths.get(inputPath));
        if (Files.notExists(Paths.get(rulesPath)))
            Files.copy(ExtractTriple.class.getResourceAsStream("/tripleRules.txt"), Paths.get(rulesPath));
        if (Files.notExists(Paths.get(predicatesPath)))
            Files.copy(ExtractTriple.class.getResourceAsStream("/predicates.txt"), Paths.get(predicatesPath));

        List<String> lines = null;
        try {
            lines = FileUtils.readLines(new File(inputPath), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        lines.remove(0);
        List<Triple> tripleList = new ArrayList<Triple>();
        TextProcess tp = new TextProcess();
        ExtractTriple extractTriple = new ExtractTriple(rulesPath, predicatesPath);
        for (String line : lines) {
            Annotation annotation = new Annotation(line);
            tp.preProcess(annotation);

            tripleList.addAll(extractTriple.extractTripleFromAnnotation(annotation));
        }


        String tripleJsons = generateTripleJson(tripleList);
        try {
            System.out.println(new File(outputPath).toPath().toAbsolutePath());
            FileUtils.write(new File(outputPath), tripleJsons,"UTF-8",false);
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
