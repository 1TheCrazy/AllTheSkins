package me.onethecrazy.util.objects;

import me.onethecrazy.util.parsing.ParsingFormat;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CacheSkin {
    @Nullable
    public List<Vertex> vertices;
    public ParsingFormat format;

    public static CacheSkin empty(){
        return new CacheSkin(List.of(), null);
    }

    public CacheSkin(@Nullable List<Vertex> vertices, ParsingFormat format){
        this.format = format;
        this.vertices = vertices;
    }
}
