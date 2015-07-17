package org.elixir_lang;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.elixir_lang.icons.ElixirIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Created by luke.imhoff on 7/27/14.
 */
public class ElixirFileType extends LanguageFileType {
    public static final ElixirFileType INSTANCE = new ElixirFileType();
    public static final ElixirScripFileType SCRIPT = new ElixirScripFileType();

    private ElixirFileType() {
        super(ElixirLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "Elixir file";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Elixir language file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "ex";
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return ElixirIcon.FILE;
    }

    public static class ElixirScripFileType extends ElixirFileType{
        @NotNull
        @Override
        public String getName() {
            return "Elixir Script";
        }

        @NotNull
        @Override
        public String getDescription() {
            return "Elixir Script File";
        }

        @NotNull
        @Override
        public String getDefaultExtension() {
            return "exs";
        }

        @NotNull
        @Override
        public Icon getIcon() {
            return ElixirIcons.FILE;
        }
    }
}
