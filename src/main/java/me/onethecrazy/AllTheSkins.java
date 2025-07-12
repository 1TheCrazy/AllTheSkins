package me.onethecrazy;

import me.onethecrazy.util.ModelNormalizer;
import me.onethecrazy.util.OBJParser;
import me.onethecrazy.util.Vertex;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class AllTheSkins implements ModInitializer {
	public static final String MOD_ID = "all-the-skins";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final List<Vertex> HEAD_MODEL = ModelNormalizer.normalize(OBJParser.parse("C:\\Users\\User\\Downloads\\random-male-human-head-base-mesh\\source\\random head\\random head.obj"));
	public static final List<Vertex> HATSUNE_MIKU_TYPE_SHIT = ModelNormalizer.normalize(OBJParser.parse("C:\\Users\\User\\Downloads\\source_male_rougestock\\source\\male_rougestock.obj"));
	public static final List<Vertex> TRIANGLE = ModelNormalizer.normalize(OBJParser.parse("C:\\Users\\User\\Downloads\\triangle.txt"));
	public static final List<Vertex> QUAD = ModelNormalizer.normalize(OBJParser.parse("C:\\Users\\User\\Downloads\\quad.txt"));
	public static final List<Vertex> FURRY = ModelNormalizer.normalize(OBJParser.parse("C:\\Users\\User\\Downloads\\source_776bd998cc3230164ebebd7dcaa88fca\\source\\776bd998cc3230164ebebd7dcaa88fca.obj"));
	public static final List<Vertex> MUSHROOM = ModelNormalizer.normalize(OBJParser.parse("C:\\Users\\User\\Downloads\\source_PC_Nightmare_Mushroom\\source\\PC_Nightmare_Mushroom.obj"));

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");
	}
}