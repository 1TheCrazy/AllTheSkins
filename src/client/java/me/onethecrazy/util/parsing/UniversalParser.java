package me.onethecrazy.util.parsing;

import me.onethecrazy.util.objects.Vertex;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class UniversalParser {
    private static final OBJParser objParser = new OBJParser();
    private static final GLBParser glbParser = new GLBParser();

    public static Optional<List<Vertex>> parse(Path path){
        var format = getParsingFormat(path);

        // If format == null we crash here
        // This could be due to:
        // - NotImplemented
        // - Outdated Client
        assert format != null;

        return parse(path, format);
    }

    public static Optional<List<Vertex>> parse(Path path, ParsingFormat format){
        return switch (format) {
            case OBJ -> objParser.parse(path);
            case GLB -> glbParser.parse(path);
        };
    }

    public static ParsingFormat getParsingFormat(Path file){
        var fileName = file.getFileName().toString().toLowerCase();

        if(fileName.endsWith(".obj"))
            return ParsingFormat.OBJ;
        else if(fileName.endsWith(".glb") || fileName.endsWith(".gltf"))
            return ParsingFormat.GLB;

        return null;
    }
}
