package org.kodeekk.etta.texture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.kodeekk.etta.ETTA;
import org.kodeekk.etta.access.SpriteContentsAccess;
import org.kodeekk.etta.mixin.accessor.TextureAtlasAccessor;
import org.kodeekk.etta.mixin.accessor.TextureAtlasSpriteAccessor;
import org.kodeekk.etta.mixin.accessor.TextureManagerAccessor;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public class ReloadTexture {
    public void onHotReload(String texturePath, ResourceLocation textureToReload) {
        onHotReload(texturePath, Set.of(textureToReload));
    }

    public void onHotReload(String texturePath, Set<ResourceLocation> texturesToReload) {
        var client = Minecraft.getInstance();
        var textures = client.getTextureManager();
        var opener = SpriteResourceLoader.create(SpriteLoader.DEFAULT_METADATA_SECTIONS);

        for (var entry : ((TextureManagerAccessor) textures).getByPath().entrySet()) {
            if (!(entry.getValue() instanceof TextureAtlas atlas)) continue;

            for (var spriteEntry : ((TextureAtlasAccessor) atlas).getTexturesByName().entrySet()) {
                ResourceLocation spriteName = spriteEntry.getKey();

                if (!texturesToReload.isEmpty() && !texturesToReload.contains(spriteName)) {
                    continue;
                }

                var contents = spriteEntry.getValue().contents();
                var originalId = ((SpriteContentsAccess) contents).etta$originalId();
                if (originalId == null) continue;

                SpriteContents newSprite = null;

                try {
                    if (texturePath != null) {
                        Resource resource;
                        Path customPath = Path.of(texturePath);

                        if (java.nio.file.Files.exists(customPath)) {
                            resource = new Resource(
                                    null,
                                    () -> new FileInputStream(customPath.toFile())
                            );
                            ETTA.getLOGGER().info("Loading custom sprite from: {}", customPath);
                        } else {
                            resource = Minecraft.getInstance().getResourceManager().getResourceOrThrow(originalId);
                        }

                        newSprite = opener.loadSprite(spriteEntry.getKey(), resource);
                    } else {
                        newSprite = opener.loadSprite(spriteEntry.getKey(), Minecraft.getInstance().getResourceManager().getResourceOrThrow(originalId));
                    }

                    if (newSprite == null) continue;
                    if (newSprite.height() != contents.height() || newSprite.width() != contents.height()) continue;

                    newSprite.increaseMipLevel(((SpriteContentsAccess) contents).etta$getMipLevel());

                    ((TextureAtlasSpriteAccessor) spriteEntry.getValue()).setContents(newSprite);

                    spriteEntry.getValue().uploadFirstFrame(atlas.getTexture());

                    ETTA.getLOGGER().info("Successfully reloaded texture: {}", spriteName);
                } catch (RuntimeException | IOException e) {
                    ETTA.getLOGGER().error("Couldn't hot reload sprite {} of {}", spriteEntry.getKey(), atlas.location(), e);
                }
            }
        }
    }

    public void onHotReload(String texturePath) {
        onHotReload(texturePath, Set.of());
    }

    public void onHotReload(ResourceLocation atlasLocation, ResourceLocation spriteToReload, String texturePath) {
        var client = Minecraft.getInstance();
        var textures = client.getTextureManager();
        var texture = textures.getTexture(atlasLocation);

        if (!(texture instanceof TextureAtlas atlas)) {
            ETTA.getLOGGER().error("Texture at {} is not a TextureAtlas", atlasLocation);
            return;
        }

        var sprite = atlas.getSprite(spriteToReload);
        if (sprite == null) {
            ETTA.getLOGGER().error("Sprite {} not found in atlas {}", spriteToReload, atlasLocation);
            return;
        }

        reloadSingleSprite(atlas, spriteToReload, sprite, texturePath);
    }

    private void reloadSingleSprite(TextureAtlas atlas, ResourceLocation spriteName,
                                    TextureAtlasSprite sprite, String texturePath) {
        var opener = SpriteResourceLoader.create(SpriteLoader.DEFAULT_METADATA_SECTIONS);
        var contents = sprite.contents();
        var originalId = ((SpriteContentsAccess) contents).etta$originalId();

        if (originalId == null) {
            ETTA.getLOGGER().error("Original ID not found for sprite: {}", spriteName);
            return;
        }

        try {
            SpriteContents newSprite;

            if (texturePath != null) {
                Resource resource;
                Path customPath = Path.of(texturePath);

                if (java.nio.file.Files.exists(customPath)) {
                    resource = new Resource(
                            null,
                            () -> new FileInputStream(customPath.toFile())
                    );
                    ETTA.getLOGGER().info("Loading custom sprite from: {}", customPath);
                } else {
                    resource = Minecraft.getInstance().getResourceManager().getResourceOrThrow(originalId);
                }

                newSprite = opener.loadSprite(spriteName, resource);
            } else {
                newSprite = opener.loadSprite(spriteName, Minecraft.getInstance().getResourceManager().getResourceOrThrow(originalId));
            }

            if (newSprite == null) {
                ETTA.getLOGGER().error("Failed to load new sprite for: {}", spriteName);
                return;
            }

            if (newSprite.height() != contents.height() || newSprite.width() != contents.height()) {
                ETTA.getLOGGER().error("Sprite dimensions don't match for: {}", spriteName);
                return;
            }

            newSprite.increaseMipLevel(((SpriteContentsAccess) contents).etta$getMipLevel());
            ((TextureAtlasSpriteAccessor) sprite).setContents(newSprite);
            sprite.uploadFirstFrame(atlas.getTexture());

            ETTA.getLOGGER().info("Successfully reloaded texture: {}", spriteName);
        } catch (RuntimeException | IOException e) {
            ETTA.getLOGGER().error("Couldn't hot reload sprite {}", spriteName, e);
        }
    }
}