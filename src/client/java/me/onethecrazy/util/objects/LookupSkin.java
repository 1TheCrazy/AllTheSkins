package me.onethecrazy.util.objects;

import me.onethecrazy.util.parsing.ParsingFormat;
import org.jetbrains.annotations.Nullable;

public class LookupSkin {
    public @Nullable String hash;
    public ParsingFormat format;

    public LookupSkin(@Nullable String hash, ParsingFormat format){
        this.hash = hash;
        this.format = format;
    }
}
