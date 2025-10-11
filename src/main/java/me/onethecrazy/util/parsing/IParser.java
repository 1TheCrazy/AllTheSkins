package me.onethecrazy.util.parsing;

import me.onethecrazy.util.objects.Vertex;

import java.util.List;
import java.util.Optional;

public interface IParser {
    Optional<List<Vertex>> parse(String data);
}
