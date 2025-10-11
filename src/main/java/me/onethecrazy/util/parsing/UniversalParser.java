package me.onethecrazy.util.parsing;

import me.onethecrazy.util.objects.Vertex;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class UniversalParser {
    private static final OBJParser objParser = new OBJParser();

    public static Optional<List<Vertex>> parse(Path path){
        return parse(path, getParsingFormat(path));
    }

    public static Optional<List<Vertex>> parse(Path path, ParsingFormat format){
        switch(format){
            case OBJ:
                return objParser.parse(path);

            default:
                return Optional.empty();
        }
    }

    public static ParsingFormat getParsingFormat(Path file){
        // NOT FINAL
        return ParsingFormat.OBJ;
    }

    public static ParsingFormat getParsingFormat(String content){
        // NOT FINAL
        return ParsingFormat.OBJ;
    }
}
