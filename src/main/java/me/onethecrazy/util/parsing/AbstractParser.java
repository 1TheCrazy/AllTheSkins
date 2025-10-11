package me.onethecrazy.util.parsing;

import me.onethecrazy.AllTheSkins;
import me.onethecrazy.util.objects.Vertex;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public abstract class AbstractParser implements IParser {
    public Optional<List<Vertex>> parse(Path filePath){
        try {
            String content = Files.readString(filePath);
            return parse(content);
        }
        catch (Exception e){
            AllTheSkins.LOGGER.error("Ran into error while reading 3D Object File: ", e);
            return Optional.empty();
        }
    }
}
