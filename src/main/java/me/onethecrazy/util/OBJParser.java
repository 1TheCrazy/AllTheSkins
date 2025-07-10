package me.onethecrazy.util;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

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
                    // Regex shit
                    List<Float> floats = Pattern.compile("-?\\d+(\\.\\d+)?").matcher(line).results().map(m -> Float.parseFloat(m.group())).toList();

                    Float3 f3Intermediate = Float3.empty();
                    Float2 f2Intermediate = Float2.empty();

                    if(floats.size() >= 3)
                        f3Intermediate = new Float3(floats.get(0), floats.get(1), floats.get(2));
                    else
                        f2Intermediate = new Float2(floats.get(0), floats.get(1));

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
                    // Skip one element in order to account for the f
                    List<String> faceData = Arrays.stream(line.split(" ")).skip(1).toList();

                    for(String vertexInfo : faceData){
                        Vertex intermediate;

                        String[] parts = vertexInfo.split("/");

                        int vIndex = Integer.parseInt(parts[0]);
                        Integer vtIndex = null;
                        Integer vnIndex = null;

                        // We have no normal data (v/vt)
                        if (parts.length == 2) {
                            vtIndex = Integer.parseInt(parts[1]);
                        }
                        // We have either everything or no texture data
                        else if (parts.length == 3) {
                            // No texture data (v//vn)
                            if (parts[1].isEmpty()) {
                                vnIndex = Integer.parseInt(parts[2]);
                            }
                            // We have everything (v/vt/vn)
                            else {
                                vtIndex = Integer.parseInt(parts[1]);
                                vnIndex = Integer.parseInt(parts[2]);
                            }
                        }

                        intermediate = new Vertex(vertexList.get(vIndex - 1), vnIndex != null ? normalList.get(vnIndex - 1) : new Float3(0, 0, 0), vtIndex != null ? textureList.get(vtIndex - 1) : new Float2(0, 0));

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
