package me.onethecrazy.util.objects.save;

import me.onethecrazy.util.parsing.ParsingFormat;
import org.jetbrains.annotations.Nullable;

public class ClientSkin {
    public String hash;
    public String name;
    public ParsingFormat format;

    public ClientSkin(){
        this.hash = "";
        this.name = "";
        this.format = null;
    }

    public ClientSkin(String hash, String name, @Nullable ParsingFormat format){
        this.hash = hash;
        this.name = name;
        this.format = format;
    }
}
