package me.onethecrazy.util.objects;

public class Vertex {
    public Float3 position;
    public Float3 normals;
    public Float2 textureUV;

    public Vertex(Float3 position, Float3 normals, Float2 textureUV){
        this.position = position;
        this.normals = normals;
        this.textureUV = textureUV;
    }
}
