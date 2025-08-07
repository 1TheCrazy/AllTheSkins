package me.onethecrazy.util;

import me.onethecrazy.AllTheSkins;
import me.onethecrazy.util.objects.Float2;
import me.onethecrazy.util.objects.Float3;
import me.onethecrazy.util.objects.Vertex;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public class OBJParser {
    public static Optional<List<Vertex>> parse(Path path){
        try {
            String content = Files.readString(path);
            return parse(content);
        }
        catch (Exception e){
            AllTheSkins.LOGGER.error("Ran into error while reading .obj File: {0}", e);
            return Optional.empty();
        }

    }

    public static Optional<List<Vertex>> parse(String obj){
        List<Float3> vertexList = new ArrayList<>();
        List<Float3> normalList = new ArrayList<>();
        List<Float2> textureList = new ArrayList<>();

        List<Vertex> filledVerticies = new ArrayList<>();

        try {
            List<String> lines = obj.lines().toList();

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

                    int numVerticies = faceData.size();

                    for(String vertexInfo : faceData){
                        Vertex intermediate;

                        String[] parts = vertexInfo.split("/");

                        if(parts[0].isBlank() || parts[0].isEmpty())
                            continue;

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

                    // Add duplicate since Minecraft renders Triangles wierd
                    if(numVerticies == 3){
                        Vertex v = filledVerticies.getLast();

                        Vertex dup = new Vertex(
                                new Float3(v.position.x, v.position.y, v.position.z),
                                new Float3(v.normals.x, v.normals.y, v.normals.z),
                                new Float2(v.textureUV.u, v.textureUV.v)
                        );

                        filledVerticies.add(dup);
                    }
                }
            }
        } catch(Exception e) {
            return Optional.empty();
        }

        return Optional.of(filledVerticies);
    }

    public static Float2 getModelHeight(List<Vertex> vertices){
        float minY = Float.POSITIVE_INFINITY,
                maxY = Float.NEGATIVE_INFINITY;

        for(Vertex v : vertices){
            if(v.position.y < minY){
                minY = v.position.y;
            }
            else if (v.position.y > maxY){
                maxY = v.position.y;
            }
        }

        return new Float2(maxY, minY);
    }


}