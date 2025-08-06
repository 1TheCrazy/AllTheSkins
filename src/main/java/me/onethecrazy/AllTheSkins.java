package me.onethecrazy;

import me.onethecrazy.util.ModelNormalizer;
import me.onethecrazy.util.OBJParser;
import me.onethecrazy.util.objects.Vertex;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Dictionary;
import java.util.List;

public class AllTheSkins implements ModInitializer {
	public static final String MOD_ID = "all-the-skins";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final List<Vertex> HEAD_MODEL = ModelNormalizer.normalize(OBJParser.parse(Path.of("C:\\Users\\User\\Downloads\\random-male-human-head-base-mesh\\source\\random head\\random head.obj")));
	public static final List<Vertex> HATSUNE_MIKU_TYPE_SHIT = ModelNormalizer.normalize(OBJParser.parse(Path.of("C:\\Users\\User\\Downloads\\source_male_rougestock\\source\\male_rougestock.obj")));
	public static final List<Vertex> TRIANGLE = ModelNormalizer.normalize(OBJParser.parse(Path.of("C:\\Users\\User\\Downloads\\triangle.txt")));
	public static final List<Vertex> QUAD = ModelNormalizer.normalize(OBJParser.parse(Path.of("C:\\Users\\User\\Downloads\\quad.txt")));
	public static final List<Vertex> FURRY = ModelNormalizer.normalize(OBJParser.parse(Path.of("C:\\Users\\User\\Downloads\\source_776bd998cc3230164ebebd7dcaa88fca\\source\\776bd998cc3230164ebebd7dcaa88fca.obj")));
	public static final List<Vertex> MUSHROOM = ModelNormalizer.normalize(OBJParser.parse(Path.of("C:\\Users\\User\\Downloads\\source_PC_Nightmare_Mushroom\\source\\PC_Nightmare_Mushroom.obj")));

	public static Dictionary<String, String> cachedLookup;
	public static Dictionary<String, String> fallbackLookup;

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing " + MOD_ID);
	}
}