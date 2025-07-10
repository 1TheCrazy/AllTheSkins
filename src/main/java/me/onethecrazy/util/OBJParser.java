package me.onethecrazy.util;

import me.onethecrazy.AllTheSkins;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class OBJParser {
    public static Optional<List<Vertex>> Parse(String path){
        List<Float3> vertexList = new ArrayList<>();
        List<Float3> normalList = new ArrayList<>();
        List<Float2> textureList = new ArrayList<>();

        List<Vertex> filledVerticies = new ArrayList<>();

        try {
            List<String> lines = Files.readAllLines(Paths.get(path));

            for (String line : lines) {
                if(line.isEmpty())
                    continue;


                // Vertex
                if(line.charAt(0) == 'v'){
                    CharSequence dataStr =  line.subSequence(3, line.length());
                    String[] floats = dataStr.toString().split(" ");

                    Float3 f3Intermediate = Float3.empty();
                    Float2 f2Intermediate = Float2.empty();

                    if(floats.length == 3)
                        f3Intermediate = new Float3(Float.parseFloat(floats[0]), Float.parseFloat(floats[1]), Float.parseFloat(floats[2]));
                    else
                        f2Intermediate = new Float2(Float.parseFloat(floats[0]), Float.parseFloat(floats[1]));

                    switch(line.charAt(1)){
                        // Vertex
                        case ' ':
                            vertexList.add(f3Intermediate);
                            break;

                        // Vertex Normal
                        case 'n':
                            normalList.add(f3Intermediate);
                            break;

                        //Texture UV
                        case 't':
                            textureList.add(f2Intermediate);
                            break;
                    }
                }
                // Face
                else if(line.charAt(0) == 'f'){
                    CharSequence dataStr =  line.subSequence(2, line.length());
                    String[] faceData = dataStr.toString().split(" ");

                    for(String vertexInfo : faceData){
                        String[] indicies = vertexInfo.split("/");

                        int posIndex = Integer.parseInt(indicies[0]) - 1;
                        int textureIndex  = Integer.parseInt(indicies[1]) - 1;
                        int normalIndex = Integer.parseInt(indicies[2]) - 1;

                        Vertex intermediate = new Vertex(vertexList.get(posIndex), normalList.get(normalIndex), textureList.get(textureIndex));

                        filledVerticies.add(intermediate);
                    }
                }
            }
        } catch (IOException e) {
            return Optional.empty();
        }

        return Optional.of(filledVerticies);
    }
}
